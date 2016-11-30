package components.common.journey;

import static play.mvc.Controller.ctx;

import com.google.inject.Inject;
import components.common.state.ContextParamManager;
import io.mikael.urlbuilder.UrlBuilder;
import play.Logger;
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

  private final JourneyContextParamProvider journeyContextParamProvider = new JourneyContextParamProvider();

  private final JourneySerialiser journeySerialiser;

  private final ContextParamManager contextParamManager;

  @Inject
  public JourneyManager(JourneySerialiser journeySerialiser, ContextParamManager contextParamManager,
                        Collection<JourneyDefinitionBuilder> journeyDefinitionBuilders) {
    this.journeySerialiser = journeySerialiser;
    this.contextParamManager = contextParamManager;

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
    String startStageHash = getDefinition(journeyName).getStartStage().getHash();

    Journey newJourney = Journey.createJourney(journeyName, startStageHash);

    journeyContextParamProvider.updateParamValueOnContext(newJourney.serialiseToString());

    //Back link may need hiding or setting to the journey's exit link if it has one
    setBackLinkOnContext(newJourney);

    return stageAsResult(getDefinition(journeyName).getStartStage());
  }

  private CompletionStage<Result> stageAsResult(JourneyStage stage) {
    if (stage.isCallable()) {
      return contextParamManager.addParamsAndRedirect(stage.getEntryCall());
    }
    else {
      return stage.getFormRenderSupplier().get();
    }
  }

  private JourneyDefinition getDefinition(String journeyName) {
    JourneyDefinition journeyDefinition = journeyNameToDefinitionMap.get(journeyName);
    if (journeyDefinition == null) {
      throw new JourneyManagerException("Unknown journey name " + journeyName);
    }
    else {
      return journeyDefinition;
    }
  }

  private JourneyDefinition getDefinition(Journey journey) {
    return getDefinition(journey.getJourneyName());
  }

  private CompletionStage<Result> performTransitionInternal(
      BiFunction<JourneyDefinition, String, TransitionResult> fireEventAction, CommonJourneyEvent event) {

    Journey journey = getJourneyFromRequest();

    JourneyDefinition journeyDefinition = getDefinition(journey);

    String previousStageName = journeyDefinition.resolveStageFromHash(journey.getCurrentStageHash()).getInternalName();

    TransitionResult transitionResult = fireEventAction.apply(journeyDefinition, journey.getCurrentStageHash());

    journey.nextStage(transitionResult.getNewStage().getHash());

    journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

    saveJourney(journey);

    setBackLinkOnContext(journey);

    Logger.debug(String.format("Journey transition: journey '%s', previous stage '%s', event '%s', new stage '%s'",
        journey.getJourneyName(), previousStageName, event.getEventMnemonic(), transitionResult.getNewStage().getInternalName()));

    return stageAsResult(transitionResult.getNewStage());
  }


  private String uriForTransitionInternal(BiFunction<JourneyDefinition, String, TransitionResult> fireEventAction) {

    Journey journey = getJourneyFromRequest();
    JourneyDefinition journeyDefinition = getDefinition(journey);

    TransitionResult transitionResult = fireEventAction.apply(journeyDefinition, journey.getCurrentStageHash());

    JourneyStage nextStage = transitionResult.getNewStage();
    if (nextStage.isCallable()) {
      //Set all known context params in the querystring of the callable URI
      String callUri = contextParamManager.addParamsToCall(nextStage.getEntryCall());

      //Update the journey context param so it reflects the stage of the journey the user WILL be on when they click it
      journey.nextStage(nextStage.getHash());
      callUri = updateJourneyParamOnUri(callUri, journey.serialiseToString());

      return callUri;
    }
    else {
      throw new JourneyException(nextStage.getInternalName() + " is not callable (must be defined with a Call in JourneyDefinition)");
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

    return performTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(stage, event), event);
  }

  public <T extends ParameterisedJourneyEvent<U>, U> CompletionStage<Result> performTransition(T event, U eventArgument) {

    return performTransitionInternal(
        (journeyDefinition, stage) -> journeyDefinition.fireEvent(stage, event, eventArgument), event);

  }

  public <T extends JourneyEvent> String uriForTransition(T event) {
    return uriForTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(stage, event));
  }

  public <T extends ParameterisedJourneyEvent<U>, U>  String uriForTransition(T event, U eventArgument) {
    return uriForTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(stage, event, eventArgument));
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
    }
    else if (journeyDefinition.getExitBackLink().isPresent()) {
      String exitPrompt = journeyDefinition.getExitBackLink().get().getPrompt();
      setBackLinkPrompt(exitPrompt);
    }
    else {
      //don't show the back link if no back history is available
      hideBackLink();
    }
  }

  private void hideBackLink() {
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

    }
    else if (journeyDefinition.getExitBackLink().isPresent()) {
      //We are exiting the current journey - wipe context info
      journeyContextParamProvider.updateParamValueOnContext("");
      hideBackLink();

      BackLink exitBackLink = journeyDefinition.getExitBackLink().get();
      return contextParamManager.addParamsAndRedirect(exitBackLink.getCall());
    }
    else {
      throw new RuntimeException("Cannot go back any further");
    }
  }

  public void setContextArguments() {

    Journey journey = Journey.fromString(journeyContextParamProvider.getParamValueFromRequest());

    //If no journey parameter is available on this request, don't show a back link
    if (journey != null) {
      setBackLinkOnContext(journey);
    }
    else {
      hideBackLink();
    }
  }

  public CompletionStage<Result> restoreCurrentStage() {
    Journey journey = restoreJourney();

    JourneyDefinition journeyDefinition = getDefinition(journey);

    JourneyStage stage = journeyDefinition.resolveStageFromHash(journey.getCurrentStageHash());

    journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

    setBackLinkOnContext(journey);

    return stageAsResult(stage);
  }

  public void saveJourney(Journey journey) {
    journeySerialiser.writeJourneyString(journey.serialiseToString());
  }

  public Journey restoreJourney() {
    return Journey.fromString(journeySerialiser.readJourneyString());
  }

  public boolean isJourneySerialised() {
    return restoreJourney() != null;
  }

}
