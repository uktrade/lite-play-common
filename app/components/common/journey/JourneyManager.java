package components.common.journey;

import static play.mvc.Controller.ctx;

import org.apache.commons.lang3.StringUtils;
import play.mvc.Result;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public class JourneyManager {

  public static final String JOURNEY_BACK_LINK_CONTEXT_PARAM = "journeyBackLink";
  public static final String JOURNEY_SUPPRESS_BACK_LINK_CONTEXT_PARAM = "journeySuppressBackLink";
  private static final String JOURNEY_NAME_SEPARATOR_CHAR = "~";

  private final Map<String, JourneyDefinition> journeyNameToDefinitionMap;

  private final JourneyContextParamProvider journeyContextParamProvider = new JourneyContextParamProvider();

  public JourneyManager(JourneyDefinition... journeyDefinitions) {

    if (journeyDefinitions.length == 0) {
      throw new JourneyManagerException("At least one JourneyDefinition must be provided");
    }

    Map<String, JourneyDefinition> journeyNameToDefinitionMap = new HashMap<>();
    for (JourneyDefinition definition : journeyDefinitions) {
      //Check journey names don't contain the special delimiter character as this will break deserialisation
      if (definition.getJourneyName().contains(JOURNEY_NAME_SEPARATOR_CHAR)) {
        throw new JourneyManagerException(String.format("Journey names cannot contain '%s' character", JOURNEY_NAME_SEPARATOR_CHAR));
      }

      journeyNameToDefinitionMap.put(definition.getJourneyName(), definition);
    }

    this.journeyNameToDefinitionMap = Collections.unmodifiableMap(journeyNameToDefinitionMap);
  }

  public void startJourney(String journeyName) {
    String startStageMnem = getDefinition(journeyName).getStartStage().getMnemonic();

    Journey newJourney = Journey.createJourney(journeyName, startStageMnem);

    journeyContextParamProvider.updateParamValueOnContext(newJourney.serialiseToString());

    //Make sure back link is hidden for the new journey
    setBackLinkOnContext(newJourney);
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
    return getDefinition(journey.journeyName);
  }

  private CompletionStage<Result> performTransitionInternal(BiFunction<JourneyDefinition, String, TransitionResult> fireEventAction) {

    Journey journey = Journey.fromString(journeyContextParamProvider.getParamValueFromRequest());

    if (journey == null) {
      throw new JourneyManagerException("Cannot perform a journey transition without a journey parameter");
    }

    JourneyDefinition journeyDefinition = getDefinition(journey);

    TransitionResult transitionResult = fireEventAction.apply(journeyDefinition, journey.getCurrentStageMnemonic());

    journey.nextStage(transitionResult.getNewStage().getMnemonic());

    journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

    //TODO persist history

    setBackLinkOnContext(journey);

    return transitionResult.getNewStage().getFormRenderSupplier().get();
  }

  public <T extends JourneyEvent> CompletionStage<Result> performTransition(T event) {

    return performTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(stage, event));
  }

  public <T extends ParameterisedJourneyEvent<U>, U> CompletionStage<Result> performTransition(T event, U eventArgument) {

    return performTransitionInternal((journeyDefinition, stage) -> journeyDefinition.fireEvent(stage, event, eventArgument));

  }

  private void hideBackLink() {
    ctx().args.put(JOURNEY_SUPPRESS_BACK_LINK_CONTEXT_PARAM, true);
  }

  private void setBackLinkOnContext(Journey journey) {

    if (journey.historyQueue.size() > 1) {

      String previousStageMnem = new ArrayList<>(journey.historyQueue).get(journey.historyQueue.size() - 2);

      JourneyStage newPreviousStage = getDefinition(journey).resolveStageFromMnemonic(previousStageMnem);
      ctx().args.put(JOURNEY_BACK_LINK_CONTEXT_PARAM, newPreviousStage.getDisplayName());
    }
    else {
      //don't show the back link if no back history is available
      hideBackLink();
    }
  }

  public CompletionStage<Result> navigateBack() {

    Journey journey = Journey.fromString(journeyContextParamProvider.getParamValueFromRequest());

    if (journey == null) {
      throw new JourneyManagerException("Cannot navigate back without a journey parameter");
    }

    JourneyDefinition journeyDefinition = getDefinition(journey);

    Deque<String> history = journey.historyQueue;

    if (history.size() > 1) {
      history.removeLast();

      //Resolve the previous stage
      String previousStageMnem = history.peekLast();

      JourneyStage stage = journeyDefinition.resolveStageFromMnemonic(previousStageMnem);

      journeyContextParamProvider.updateParamValueOnContext(journey.serialiseToString());

      //TODO persist newly modified history

      setBackLinkOnContext(journey);

      return stage.getFormRenderSupplier().get();
    } else {
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

  private static class Journey {

    private final String journeyName;
    private final Deque<String> historyQueue;

    private Journey(String journeyName, Deque<String> historyQueue) {
      this.journeyName = journeyName;
      this.historyQueue = historyQueue;
    }

    public String getCurrentStageMnemonic() {
      return historyQueue.peekLast();
    }

    public String serialiseToString() {
      return journeyName + JOURNEY_NAME_SEPARATOR_CHAR +  String.join(",", historyQueue);
    }

    public void nextStage(String stageMnemonic) {
      historyQueue.addLast(stageMnemonic);
    }

    public static Journey fromString(String journeyString) {

      if (StringUtils.isBlank(journeyString)) {
        return null;
      }

      if (!journeyString.contains(JOURNEY_NAME_SEPARATOR_CHAR)) {
        throw new JourneyManagerException("Invalid journey string, missing underscore character");
      }
      else {
        String[] journeyStringSplit = journeyString.split(JOURNEY_NAME_SEPARATOR_CHAR, 2);
        String journeyName = journeyStringSplit[0];
        String journeyHistory = journeyStringSplit[1];

        List<String> history = new ArrayList<>();
        Collections.addAll(history, journeyHistory.split(","));

        return new Journey(journeyName, new ArrayDeque<>(history));
      }
    }

    public static Journey createJourney(String journeyName, String initialStateMnemonic) {
      return new Journey(journeyName, new ArrayDeque<>(Collections.singletonList(initialStateMnemonic)));
    }
  }
}
