package components.common.journey;

import com.google.common.collect.Table;
import play.Logger;
import play.libs.concurrent.HttpExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * EVENT argument = argument provided when an event fires
 * TRANSITION argument = toString of argument object used to fire transition (can represent event argument,
 * or may have been supplied by another method)
 */
public class JourneyDefinition {

  private final String journeyName;

  private final Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap;

  private final Map<DecisionStage, DecisionLogic> decisionLogicMap;

  private final Map<String, JourneyStage> registeredStages;

  private final CommonStage startStage;

  private final BackLink exitBackLink;

  JourneyDefinition(String journeyName, Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap,
                    Map<DecisionStage, DecisionLogic> decisionLogicMap, Map<String, JourneyStage> registeredStages,
                    CommonStage startStage, BackLink exitBackLink) {
    this.journeyName = journeyName;
    this.stageTransitionMap = stageTransitionMap;
    this.decisionLogicMap = decisionLogicMap;
    this.registeredStages = registeredStages;
    this.startStage = startStage;
    this.exitBackLink = exitBackLink;
  }

  public String getJourneyName() {
    return journeyName;
  }

  CommonStage getStartStage() {
    return startStage;
  }

  JourneyStage resolveStageFromHash(String hash) {
    JourneyStage journeyStage = registeredStages.get(hash);

    if (journeyStage == null) {
      throw new JourneyException(String.format("Stage '%s' is not defined in this journey", hash));
    } else {
      return journeyStage;
    }
  }

  public EventResult fireEvent(HttpExecutionContext httpExecutionContext, String currentStageHash, JourneyEvent event) {
    return fireEventInternal(httpExecutionContext, currentStageHash, event, null);
  }

  public <T> EventResult fireEvent(HttpExecutionContext httpExecutionContext, String currentStageHash,
                                   ParameterisedJourneyEvent<T> event, T eventArgument) {
    return fireEventInternal(httpExecutionContext, currentStageHash, event, eventArgument);
  }

  private MoveAction asMoveAction(TransitionAction transitionAction) {
    if (transitionAction instanceof MoveAction) {
      return ((MoveAction) transitionAction);
    } else {
      throw new JourneyException("Unknown action type " + transitionAction.getClass().getName());
    }
  }

  private EventResult fireEventInternal(HttpExecutionContext httpExecutionContext, String currentStageHash,
                                        CommonJourneyEvent event, Object eventArgument) {

    JourneyStage currentStage = resolveStageFromHash(currentStageHash);

    MoveAction moveAction = doMoveOrBranch(event, eventArgument, currentStage);
    return convertMoveToResult(httpExecutionContext, moveAction);
  }

  public EventResult startJourney(HttpExecutionContext httpExecutionContext) {
    MoveAction moveAction = new MoveAction(startStage, MoveAction.Direction.FORWARD);
    return convertMoveToResult(httpExecutionContext, moveAction);
  }

  private EventResult convertMoveToResult(HttpExecutionContext httpExecutionContext, MoveAction moveAction) {
    CommonStage nextStage = moveAction.getDestinationStage();

    if (nextStage instanceof JourneyStage) {
      //If the next stage is not a decision, we can return an "immediate" EventResult which does not require async handling
      return new EventResult(new TransitionResult((JourneyStage) nextStage, moveAction.getDirection()));
    } else {
      //For decisions, build up a chain of 1 or more CompletableFutures to make the decisions and return a non-immediate EventResult
      CompletionStage<TransitionResult> resultCompletionStage = buildDecisionChain(httpExecutionContext, moveAction)
          .thenApply(e -> new TransitionResult((JourneyStage) e.getDestinationStage(), e.getDirection()));

      return new EventResult(resultCompletionStage);
    }
  }

  private CompletionStage<MoveAction> buildDecisionChain(HttpExecutionContext httpExecutionContext, MoveAction moveAction) {

    if (moveAction.getDestinationStage() instanceof DecisionStage) {
      DecisionStage decisionStage = (DecisionStage) moveAction.getDestinationStage();
      DecisionLogic decisionLogic = decisionLogicMap.get(decisionStage);

      //Recurse through DecisionStages, building up the chain until a JourneyStage is hit
      return decisionLogic.getDecider().decide()
          .thenApplyAsync(e -> doDecision(decisionStage, decisionLogic, e), httpExecutionContext.current())
          .thenComposeAsync(e -> buildDecisionChain(httpExecutionContext, e), httpExecutionContext.current());

    } else {
      //End the chain when a JourneyStage is hit
      return CompletableFuture.completedFuture(moveAction);
    }
  }

  private MoveAction doDecision(DecisionStage stage, DecisionLogic decisionLogic, Object deciderResult) {
    //Convert raw decision result using the converter function (could just be identity for simple cases)
    Object conversionResult = decisionLogic.getDecisionResultConverter().apply(deciderResult);
    Logger.debug("Journey decision: stage {} - raw result {}, converted to {}", stage.getInternalName(), deciderResult, conversionResult);

    //Find then when() clause which matches the decider result value
    TransitionAction transitionAction = decisionLogic.getConditionMap().get(conversionResult);
    if (transitionAction == null) {
      if (decisionLogic.getElseCondition() != null) {
        transitionAction = decisionLogic.getElseCondition();
      } else {
        throw new JourneyException(String.format("No matching branch for value '%s' at decision stage %s", conversionResult, stage.getInternalName()));
      }
    }

    //Find the associated move action for the matched when() clause
    MoveAction resultMoveAction = asMoveAction(transitionAction);
    Logger.debug("Journey decision: resolved next stage {}", resultMoveAction.getDestinationStage().getInternalName());

    return resultMoveAction;
  }

  private MoveAction doMoveOrBranch(CommonJourneyEvent event, Object eventArgument, JourneyStage currentStage) {

    //TODO validation: transition cannot be a loop

    TransitionAction transitionAction = stageTransitionMap.get(currentStage, event);
    if (transitionAction == null) {
      throw new JourneyException(String.format("No transition defined for %s, %s", currentStage, event));
    } else {
      if (transitionAction instanceof BranchAction) {
        BranchAction branch = (BranchAction) transitionAction;

        if (eventArgument == null) {
          throw new JourneyException("Event argument cannot be null", currentStage, event);
        }

        //Convert event argument using the branch's conversion function (possibly just the identity function)
        Object transitionArgument = branch.eventArgumentConverter.apply(eventArgument);

        if (transitionArgument == null) {
          throw new JourneyException("Transition argument cannot be null", currentStage, event);
        }

        //Resolve an action corresponding to the transition argument value
        TransitionAction conditionAction = branch.resultMap.get(transitionArgument);

        conditionAction = conditionAction == null ? branch.elseTransition : conditionAction;

        if (conditionAction != null) {
          return asMoveAction(conditionAction);
        } else {
          throw new JourneyException("Branch not matched with argument " + transitionArgument, currentStage, event);
        }
      } else {
        return asMoveAction(transitionAction);
      }
    }
  }

  public Optional<BackLink> getExitBackLink() {
    return Optional.ofNullable(exitBackLink);
  }

  /**
   * @return A collection of all transitions in this JourneyDefinition, expressed as GraphViewTransition objects. Events with
   * branching logic are represented as multiple transitions with distinct condition values.
   */
  Collection<GraphViewTransition> asGraphViewTransitions() {

    List<GraphViewTransition> allTransitions = new ArrayList<>();

    for (Table.Cell<JourneyStage, CommonJourneyEvent, TransitionAction> cell : stageTransitionMap.cellSet()) {

      TransitionAction action = cell.getValue();
      if (action instanceof BranchAction) {
        //For a branch, create a transition to represent each condition
        for (Map.Entry<Object, TransitionAction> branchOption : ((BranchAction) action).resultMap.entrySet()) {
          CommonStage newStage = asMoveAction(branchOption.getValue()).getDestinationStage();
          allTransitions.add(new GraphViewTransition(cell.getRowKey(), newStage, cell.getColumnKey().getEventMnemonic(), branchOption.getKey().toString()));
        }
      } else {
        CommonStage newStage = asMoveAction(cell.getValue()).getDestinationStage();
        allTransitions.add(new GraphViewTransition(cell.getRowKey(), newStage, cell.getColumnKey().getEventMnemonic(), null));
      }
    }

    decisionLogicMap.forEach((stage, decisionLogic) -> {
      decisionLogic.getConditionMap().forEach((conditionValue, action) -> {
        allTransitions.add(new GraphViewTransition(stage, asMoveAction(action).getDestinationStage(), conditionValue.toString(), null));
      });
    });

    return allTransitions;
  }

}
