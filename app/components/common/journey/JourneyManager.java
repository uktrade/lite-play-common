package components.common.journey;

import static play.mvc.Controller.ctx;

import com.google.inject.Inject;
import components.common.state.ContextParamManager;
import io.mikael.urlbuilder.UrlBuilder;
import play.Logger;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class JourneyManager {

  public static final String JOURNEY_BACK_LINK_CONTEXT_PARAM = "journeyBackLink";
  public static final String JOURNEY_SUPPRESS_BACK_LINK_CONTEXT_PARAM = "journeySuppressBackLink";
  public static final String JOURNEY_NAME_SEPARATOR_CHAR = "~";
  static final String JOURNEY_STAGE_SEPARATOR_CHAR = "-";

  private final Map<String, JourneyDefinition> journeyNameToDefinitionMap;

  private final JourneySerialiser journeySerialiser;

  private final ContextParamManager contextParamManager;

  private final JourneyContextParamProvider journeyContextParamProvider;

  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public JourneyManager(JourneySerialiser journeySerialiser,
                        ContextParamManager contextParamManager,
                        JourneyContextParamProvider journeyContextParamProvider,
                        Collection<JourneyDefinitionBuilder> journeyDefinitionBuilders,
                        HttpExecutionContext httpExecutionContext) {
    this.journeySerialiser = journeySerialiser;
    this.contextParamManager = contextParamManager;
    this.journeyContextParamProvider = journeyContextParamProvider;
    this.httpExecutionContext = httpExecutionContext;

    //Build all JourneyDefinitions from all JourneyDefinitionBuilders
    Collection<JourneyDefinition> journeyDefinitions = journeyDefinitionBuilders.stream()
        .flatMap(e -> e.buildAll().stream())
        .collect(Collectors.toSet());

    if (journeyDefinitions.size() == 0) {
      throw new JourneyManagerException("At least one JourneyDefinition must be provided");
    }

    Map<String, JourneyDefinition> journeyNameToDefinitionMap = new HashMap<>();
    for (JourneyDefinition definition : journeyDefinitions) {
      //Check journey names don't contain the special delimiter character as this will break deserialisation
      if (definition.getJourneyName().contains(JOURNEY_NAME_SEPARATOR_CHAR)) {
        throw new JourneyManagerException(String.format("Journey names cannot contain '%s' character", JOURNEY_NAME_SEPARATOR_CHAR));
      }

      //Don't allow duplicate journey names from multiple builders
      journeyNameToDefinitionMap.merge(definition.getJourneyName(), definition,
          (a,b) -> {throw new JourneyManagerException("Attempt to create duplicate journey named " + definition.getJourneyName());});
    }

    this.journeyNameToDefinitionMap = Collections.unmodifiableMap(journeyNameToDefinitionMap);
  }

  public String getCurrentInternalStageName() {
    Journey journey = getJourneyFromRequest();
    return getDefinition(journey).resolveStageFromHash(journey.getCurrentStageHash()).getInternalName();
  }

  public CompletionStage<Result> startJourney(String journeyName) {

    Journey newJourney = Journey.createJourney(journeyName);

    JourneyDefinition journeyDefinition = getDefinition(journeyName);

    EventResult startResult = journeyDefinition.startJourney(httpExecutionContext);

    return startResult.getCompletableResult().thenComposeAsync(t -> applyTransitionResult(t, "<START>", newJourney, "<none>"),
        httpExecutionContext.current());
  }

  private CompletionStage<Result> stageAsResult(JourneyStage stage) {
    if (stage.isCallable()) {
      return contextParamManager.addParamsAndRedirect(stage.getEntryCall());
    } else {
      return stage.getFormRenderSupplier().get();
    }
  }

  private JourneyDefinition getDefinition(String journeyName) {
    JourneyDefinition journeyDefinition = journeyNameToDefinitionMap.get(journeyName);
    if (journeyDefinition == null) {
      throw new JourneyManagerException("Unknown journey name " + journeyName);
    } else {
      return journeyDefinition;
    }
  }

  private JourneyDefinition getDefinition(Journey journey) {
    return getDefinition(journey.getJourneyName());
  }

  private void applyTransitionResultToJourney(Journey journey, TransitionResult transitionResult) {

    if (transitionResult.getDirection() == MoveAction.Direction.FORWARD) {
      journey.pushStage(transitionResult.getNewStage().getHash());
    } else if (transitionResult.getDirection() == MoveAction.Direction.BACKWARD) {
      journey.popBackToStage(transitionResult.getNewStage().getHash());
    } else {
      throw new JourneyManagerException("Unknown move direction");
    }
  }

  private CompletionStage<Result> performTransitionInternal(
      BiFunction<JourneyDefinition, String, EventResult> fireEventAction, String eventMnemonic) {

    Journey journey = getJourneyFromRequest();
    JourneyDefinition journeyDefinition = getDefinition(journey);
    String previousStageName = journeyDefinition.resolveStageFromHash(journey.getCurrentStageHash()).getInternalName();

    //Run the parameterised or non-parameterised event, then update the various journey serialisation points (context param/DAO) with new journey info
    return fireEventAction.apply(journeyDefinition, journey.getCurrentStageHash()).getCompletableResult()
        .thenComposeAsync(transitionResult -> applyTransitionResult(transitionResult, eventMnemonic, journey, previousStageName),
            httpExecutionContext.current());
  }

  /**
   * Updates the journey state (back link and journey history string) on the current HttpContext to reflect the given transition.
   * Also saves the journey using the current serialiser.
   * @param transitionResult Transition which has just occurred.
   * @param eventMnemonic For logging.
   * @param journey The current Journey state.
   * @param previousStageName For logging.
   * @return A completable JourneyStage result (i.e. call or render)
   */
  private CompletionStage<Result> applyTransitionResult(TransitionResult transitionResult, String eventMnemonic,
                                                        Journey journey, String previousStageName) {
    applyTransitionResultToJourney(journey, transitionResult);

    journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

    saveJourney(journey);

    setBackLinkOnContext(journey);

    Logger.debug(String.format("Journey transition: journey '%s', previous stage '%s', event '%s', new stage '%s'",
        journey.getJourneyName(), previousStageName, eventMnemonic, transitionResult.getNewStage().getInternalName()));

    return stageAsResult(transitionResult.getNewStage());
  }


  private String uriForTransitionInternal(BiFunction<JourneyDefinition, String, EventResult> fireEventAction) {

    Journey journey = getJourneyFromRequest();
    JourneyDefinition journeyDefinition = getDefinition(journey);

    //Fire the event (parameterised or non-parameterised)
    EventResult eventResult = fireEventAction.apply(journeyDefinition, journey.getCurrentStageHash());

    //Only allow URL generation if the result is "immediate", i.e. has no intermediate decisions
    if (eventResult.isImmediate()) {
      TransitionResult transitionResult = eventResult.getImmediateResult();

      JourneyStage nextStage = transitionResult.getNewStage();
      if (nextStage.isCallable()) {
        //Set all known context params in the querystring of the callable URI
        String callUri = contextParamManager.addParamsToCall(nextStage.getEntryCall());

        //Update the journey context param so it reflects the stage of the journey the user WILL be on when they click it
        applyTransitionResultToJourney(journey, transitionResult);
        callUri = updateJourneyParamOnUri(callUri, journey.serialiseToString());

        return callUri;
      } else {
        throw new JourneyException(nextStage.getInternalName() + " is not callable (must be defined with a Call in JourneyDefinition)");
      }
    } else {
      throw new JourneyException("Cannot get a URI for a transition with decision stages");
    }
  }

  private Journey getJourneyFromRequest() {
    Journey journey = Journey.fromString(journeyContextParamProvider.getParamValueFromRequest());

    if (journey == null) {
      throw new JourneyManagerException("Cannot perform a journey transition without a journey parameter");
    }
    return journey;
  }

  public <T extends JourneyEvent> CompletionStage<Result> performTransition(T event) {

    return performTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(httpExecutionContext, stage, event), event.getEventMnemonic());
  }

  public <T extends ParameterisedJourneyEvent<U>, U> CompletionStage<Result> performTransition(T event, U eventArgument) {

    return performTransitionInternal(
        (journeyDefinition, stage) -> journeyDefinition.fireEvent(httpExecutionContext, stage, event, eventArgument), event.getEventMnemonic());

  }

  public <T extends JourneyEvent> String uriForTransition(T event) {
    return uriForTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(httpExecutionContext, stage, event));
  }

  public <T extends ParameterisedJourneyEvent<U>, U>  String uriForTransition(T event, U eventArgument) {
    return uriForTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(httpExecutionContext, stage, event, eventArgument));
  }

  private String updateJourneyParamOnUri(String uri, String newJourney) {
    UrlBuilder urlBuilder = UrlBuilder.fromString(uri);
    urlBuilder = urlBuilder.setParameter(JourneyContextParamProvider.JOURNEY_CONTEXT_PARAM_NAME, newJourney);
    return urlBuilder.toString();
  }

  private void setBackLinkOnContext(Journey journey) {

    JourneyDefinition journeyDefinition = getDefinition(journey);
    if (journey.getHistoryQueue().size() > 1) {

      String previousStageHash = new ArrayList<>(journey.getHistoryQueue()).get(journey.getHistoryQueue().size() - 2);

      JourneyStage newPreviousStage = journeyDefinition.resolveStageFromHash(previousStageHash);
      setBackLinkPrompt(newPreviousStage.getDisplayName());
    } else if (journeyDefinition.getExitBackLink().isPresent()) {
      String exitPrompt = journeyDefinition.getExitBackLink().get().getPrompt();
      setBackLinkPrompt(exitPrompt);
    } else {
      //don't show the back link if no back history is available
      hideBackLink();
    }
  }

  public void hideBackLink() {
    ctx().args.put(JOURNEY_SUPPRESS_BACK_LINK_CONTEXT_PARAM, true);
  }

  private void setBackLinkPrompt(String prompt) {
    ctx().args.put(JOURNEY_BACK_LINK_CONTEXT_PARAM, prompt);
  }

  public CompletionStage<Result> navigateBack() {

    Journey journey = Journey.fromString(journeyContextParamProvider.getParamValueFromRequest());

    if (journey == null) {
      throw new JourneyManagerException("Cannot navigate back without a journey parameter");
    }

    JourneyDefinition journeyDefinition = getDefinition(journey);

    Deque<String> history = journey.getHistoryQueue();

    if (history.size() > 1) {
      history.removeLast();

      //Resolve the previous stage
      String previousStageHash = history.peekLast();

      JourneyStage stage = journeyDefinition.resolveStageFromHash(previousStageHash);

      journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

      saveJourney(journey);

      setBackLinkOnContext(journey);

      return stageAsResult(stage);

    } else if (journeyDefinition.getExitBackLink().isPresent()) {
      //We are exiting the current journey - wipe context info
      journeyContextParamProvider.updateParamValueOnContext("");
      hideBackLink();

      BackLink exitBackLink = journeyDefinition.getExitBackLink().get();
      return contextParamManager.addParamsAndRedirect(exitBackLink.getCall());
    } else {
      throw new RuntimeException("Cannot go back any further");
    }
  }

  public void setContextArguments() {

    Journey journey = Journey.fromString(journeyContextParamProvider.getParamValueFromRequest());

    //If no journey parameter is available on this request, don't show a back link
    if (journey != null) {
      setBackLinkOnContext(journey);
    } else {
      hideBackLink();
    }
  }

  public CompletionStage<Result> restoreCurrentStage(String journeyName) {
    Journey journey = restoreJourney(journeyName);

    JourneyDefinition journeyDefinition = getDefinition(journey);

    JourneyStage stage = journeyDefinition.resolveStageFromHash(journey.getCurrentStageHash());

    journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

    setBackLinkOnContext(journey);

    return stageAsResult(stage);
  }

  public void saveJourney(Journey journey) {
    journeySerialiser.writeJourneyString(journey.getJourneyName(), journey.serialiseToString());
  }

  public boolean isJourneySerialised(String journeyName) {
    return restoreJourney(journeyName) != null;
  }

  private Journey restoreJourney(String journeyName) {
    return Journey.fromString(journeySerialiser.readJourneyString(journeyName));
  }

}
