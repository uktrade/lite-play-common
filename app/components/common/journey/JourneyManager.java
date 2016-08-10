package components.common.journey;

import static play.mvc.Controller.ctx;

import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public class JourneyManager {

  public static final String JOURNEY_STAGE_REQUEST_PARAM = "journeyStage";
  public static final String JOURNEY_HISTORY_REQUEST_PARAM = "journeyHistory";

  public static final String JOURNEY_STAGE_CONTEXT_PARAM = "journeyStage";
  public static final String JOURNEY_HISTORY_CONTEXT_PARAM = "journeyHistory";
  public static final String JOURNEY_BACK_LINK_CONTEXT_PARAM = "journeyBackLink";
  public static final String JOURNEY_SUPPRESS_BACK_LINK_CONTEXT_PARAM = "journeySuppressBackLink";

  private static final String SESSION_JOURNEY_PARAM = "journey";

  private final Map<String, JourneyDefinition> journeyNameToDefinitionMap;

  public JourneyManager(JourneyDefinition... journeyDefinitions) {

    if (journeyDefinitions.length == 0) {
      throw new RuntimeException("At least one JourneyDefinition must be provided");
    }

    Map<String, JourneyDefinition> journeyNameToDefinitionMap = new HashMap<>();
    for (JourneyDefinition definition : journeyDefinitions) {
      journeyNameToDefinitionMap.put(definition.getJourneyName(), definition);
    }

    this.journeyNameToDefinitionMap = Collections.unmodifiableMap(journeyNameToDefinitionMap);
  }

  private static List<String> deserialiseJourneyHistory(String fromString) {
    List<String> history = new ArrayList<>();
    Collections.addAll(history, fromString.split(","));
    return history;
  }

  private static String serialiseJourneyHistory(List<String> history) {
    return String.join(",", history);
  }

  public void startJourney(String journeyId) {
    //TODO should be a request parameter
    ctx().session().put(SESSION_JOURNEY_PARAM, journeyId);

    String startStageMnem = journeyNameToDefinitionMap.get(journeyId).getStartStage().getMnemonic();

    ctx().args.put(JOURNEY_STAGE_CONTEXT_PARAM, startStageMnem);
    ctx().args.put(JOURNEY_HISTORY_CONTEXT_PARAM, serialiseJourneyHistory(Collections.singletonList(startStageMnem)));

    //don't show the back link for a new journey
    ctx().args.put(JOURNEY_SUPPRESS_BACK_LINK_CONTEXT_PARAM, true);
  }

  private static String getPostParamValue(String paramName, boolean strict) {

    Map<String, String[]> postParamMap = ctx().request().body().asFormUrlEncoded();

    if (postParamMap == null) {
      if (strict) {
        throw new RuntimeException("Journey navigation is only supported by POST requests");
      }
      else {
        return null;
      }
    }

    String[] paramArray = postParamMap.get(paramName);

    if (paramArray == null || paramArray.length == 0 /*|| StringUtils.isBlank(paramArray[0])*/) {
      if (strict) {
        throw new RuntimeException("Request body missing mandatory journey parameter " + paramName);
      }
      else {
        return null;
      }
    }
    else {
      return paramArray[0];
    }
  }

  private CompletionStage<Result> performTransitionInternal(BiFunction<JourneyDefinition, String, TransitionResult> fireEventAction) {

    //TODO should also be in request
    JourneyDefinition journeyDefinition = journeyNameToDefinitionMap.get(ctx().session().get(SESSION_JOURNEY_PARAM));

    String currentStage = getPostParamValue(JOURNEY_STAGE_REQUEST_PARAM, true);
    String historyString = getPostParamValue(JOURNEY_HISTORY_REQUEST_PARAM, true);

    TransitionResult transitionResult = fireEventAction.apply(journeyDefinition, currentStage);

    List<String> history = deserialiseJourneyHistory(historyString);

    history.add(transitionResult.getNewStage().getMnemonic());

    ctx().args.put(JOURNEY_STAGE_CONTEXT_PARAM, transitionResult.getNewStage().getMnemonic());
    ctx().args.put(JOURNEY_HISTORY_CONTEXT_PARAM, serialiseJourneyHistory(history));

    //TODO persist history

    ctx().args.put(JOURNEY_BACK_LINK_CONTEXT_PARAM, transitionResult.getBackLinkText());

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

  private void setBackLinkOnContext(JourneyDefinition journeyDefinition, List<String> history, int offset) {

    if (history.size() > offset - 1) {
      JourneyStage newPreviousStage = journeyDefinition.resolveStageFromMnemonic(history.get(history.size() - offset));
      ctx().args.put(JOURNEY_BACK_LINK_CONTEXT_PARAM, newPreviousStage.getDisplayName());
    }
    else {
      //don't show the back link if no back history is available
      hideBackLink();
    }
  }

  public CompletionStage<Result> navigateBack() {

    String historyString = getPostParamValue(JOURNEY_HISTORY_REQUEST_PARAM, true);
    JourneyDefinition journeyDefinition = journeyNameToDefinitionMap.get(ctx().session().get("journey"));
    List<String> history = deserialiseJourneyHistory(historyString);

    if (history.size() > 1) {
      //Remove the current stage
      history.remove(history.size() - 1);
      //Resolve the previous stage
      String previousStageMnem = history.get(history.size() - 1);

      JourneyStage stage = journeyDefinition.resolveStageFromMnemonic(previousStageMnem);

      ctx().args.put(JOURNEY_STAGE_CONTEXT_PARAM, stage.getMnemonic());
      ctx().args.put(JOURNEY_HISTORY_CONTEXT_PARAM, serialiseJourneyHistory(history));

      //TODO persist history

      setBackLinkOnContext(journeyDefinition, history, 2);

      return stage.getFormRenderSupplier().get();
    } else {
      throw new RuntimeException("Cannot go back any further");
    }
  }

  public void setContextArguments() {

    String currentStage = getPostParamValue(JOURNEY_STAGE_REQUEST_PARAM, false);
    String historyString = getPostParamValue(JOURNEY_HISTORY_REQUEST_PARAM, false);
    String journeyName = ctx().session().get(SESSION_JOURNEY_PARAM);

    if (StringUtils.isNoneBlank(currentStage, historyString, journeyName)) {

      Logger.info("Setting journey parameters on request (stage " + currentStage + ")");

      JourneyDefinition journeyDefinition = journeyNameToDefinitionMap.get(journeyName);

      ctx().args.put(JOURNEY_STAGE_CONTEXT_PARAM, currentStage);
      ctx().args.put(JOURNEY_HISTORY_CONTEXT_PARAM, historyString);

      setBackLinkOnContext(journeyDefinition, deserialiseJourneyHistory(historyString), 1);
    }
    else {
      hideBackLink();
      Logger.info(String.format("Request missing journey parameters, not setting any (stage %s history %s name %s)",
          currentStage, historyString, journeyName));
    }
  }
}
