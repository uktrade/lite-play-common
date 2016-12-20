package components.common.journey;

import com.google.common.collect.Table;
import play.Logger;
import play.libs.concurrent.HttpExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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

  private final JourneyStage startStage;

  private final BackLink exitBackLink;

  JourneyDefinition(String journeyName, Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap,
                    Map<DecisionStage, DecisionLogic> decisionLogicMap, Map<String, JourneyStage> registeredStages,
                    JourneyStage startStage, BackLink exitBackLink) {
    this.journeyName = journeyName;
    this.stageTransitionMap = stageTransitionMap;
    this.decisionLogicMap = decisionLogicMap;
    this.registeredStages = registeredStages;
    this.startStage = startStage;
    this.exitBackLink = exitBackLink;

    //Validate that all stages referenced by transitions are registered
    Set<JourneyStage> unregisteredStages = stageTransitionMap.rowKeySet()
        .stream()
        .filter(e -> !registeredStages.containsKey(e.getHash()))
        .collect(Collectors.toSet());

    //TODO check destination stages are also registered
    if (unregisteredStages.size() > 0) {
      String unregistered = unregisteredStages.stream()
          .map(JourneyStage::getInternalName)
          .collect(Collectors.joining(", "));

      throw new JourneyException("Stages are not registered: " + unregistered);
    }
  }

  public String getJourneyName() {
    return journeyName;
  }

  JourneyStage getStartStage() {
    return startStage;
  }

  public JourneyStage resolveStageFromHash(String hash) {
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

  private CommonStage resolveMoveAction(CommonStage currentStage, TransitionAction transitionAction,
                                        CommonJourneyEvent event) {

    if (transitionAction instanceof TransitionAction.MoveStage) {
      TransitionAction.MoveStage moveStageAction = ((TransitionAction.MoveStage) transitionAction);
      return moveStageAction.getDestinationStage();
    } else {
      //TODO - handle journey end
      throw new JourneyException("Unknown action type " + transitionAction.getClass().getName(), currentStage, event);
    }
  }

  private EventResult fireEventInternal(HttpExecutionContext httpExecutionContext, String currentStageHash,
                                        CommonJourneyEvent event, Object eventArgument) {

    JourneyStage currentStage = resolveStageFromHash(currentStageHash);

    CommonStage nextStage = doMoveOrBranch(event, eventArgument, currentStage);

    if (nextStage instanceof JourneyStage) {
      //If the next stage is not a decision, we can return an "immediate" EventResult which does not require async handling
      return new EventResult(new TransitionResult((JourneyStage) nextStage));
    }
    else {
      //For decisions, build up a chain of 1 or more CompletableFutures to make the decisions and return a non-immediate EventResult
      CompletionStage<TransitionResult> resultCompletionStage = buildDecisionChain(httpExecutionContext, currentStage, nextStage, event)
          .thenApply(e -> new TransitionResult((JourneyStage) e));

      return new EventResult(resultCompletionStage);
    }
  }

  private CompletionStage<CommonStage> buildDecisionChain(HttpExecutionContext httpExecutionContext, JourneyStage initialStage,
                                                          CommonStage stage, CommonJourneyEvent event) {

    if (stage instanceof DecisionStage) {
      DecisionStage decisionStage = (DecisionStage) stage;
      DecisionLogic decisionLogic = decisionLogicMap.get(decisionStage);

      //Recurse through DecisionStages, building up the chain until a JourneyStage is hit
      return decisionLogic.getDecider().decide()
          .thenApplyAsync(e -> doDecision(decisionStage, decisionLogic, e), httpExecutionContext.current())
          .thenComposeAsync(e -> buildDecisionChain(httpExecutionContext, initialStage, e, event), httpExecutionContext.current());

    } else {
      //End the chain when a JourneyStage is hit
      return CompletableFuture.completedFuture(stage);
    }
  }

  private CommonStage doDecision(DecisionStage stage, DecisionLogic decisionLogic, Object deciderResult) {
    //Convert raw decision result using the converter function (could just be identity for simple cases)
    Object conversionResult = decisionLogic.getDecisionResultConverter().apply(deciderResult);
    Logger.debug("Journey decision: stage {} - raw result {}, converted to {}", stage.getInternalName(), deciderResult, conversionResult);

    //Find then when() clause which matches the decider result value
    TransitionAction transitionAction = decisionLogic.getConditionMap().get(conversionResult.toString());
    if (transitionAction == null) {
      if (decisionLogic.getElseCondition() != null) {
        transitionAction = decisionLogic.getElseCondition();
      } else {
        throw new JourneyException(String.format("No matching branch for value '%s' at decision stage %s", conversionResult, stage.getInternalName()));
      }
    }

    //Find the associated move action for the matched when() clause
    CommonStage resultStage = resolveMoveAction(stage, transitionAction, null);
    Logger.debug("Journey decision: resolved next stage {}", resultStage.getInternalName());

    return resultStage;
  }

  private CommonStage doMoveOrBranch(CommonJourneyEvent event, Object eventArgument, JourneyStage currentStage) {

    //TODO validation: transition cannot be a loop

    TransitionAction transitionAction = stageTransitionMap.get(currentStage, event);
    if (transitionAction == null) {
      throw new JourneyException("No transition available for %s, %s", currentStage, event);
    } else {
      if (transitionAction instanceof TransitionAction.Branch) {
        TransitionAction.Branch branch = (TransitionAction.Branch) transitionAction;

        Object transitionArgument;
        if (eventArgument != null) {
          if(branch.eventArgumentConverter == null) {
            throw new JourneyException("Event argument given but converter function unavailable", currentStage, event);
          }

          transitionArgument = branch.eventArgumentConverter.apply(eventArgument);
        } else {
          if(branch.transitionArgumentSupplier == null) {
            throw new JourneyException("Event argument not given but argument supplier unavailable", currentStage, event);
          }

          transitionArgument = branch.transitionArgumentSupplier.get();
        }

        //TODO may not be needed?
        if (transitionArgument == null) {
          throw new JourneyException("Transition argument cannot be null", currentStage, event);
        }

        TransitionAction conditionAction = branch.resultMap.get(transitionArgument.toString());

        conditionAction = conditionAction == null ? branch.elseTransition : conditionAction;

        if (conditionAction != null) {
          return resolveMoveAction(currentStage, conditionAction, event);
        } else {
          throw new JourneyException("Branch not matched with argument " + transitionArgument, currentStage, event);
        }
      } else {
        return resolveMoveAction(currentStage, transitionAction, event);
      }
    }
  }

  public Optional<BackLink> getExitBackLink() {
    return Optional.ofNullable(exitBackLink);
  }

  /**
   * @return A list of all transitions in this JourneyDefinition, expressed as GraphViewTransition objects. Events with
   * branching logic are represented as multiple transitions with distinct condition values.
   */
  List<GraphViewTransition> asGraphViewTransitions() {

    List<GraphViewTransition> allTransitions = new ArrayList<>();

    for (Table.Cell<JourneyStage, CommonJourneyEvent, TransitionAction> cell : stageTransitionMap.cellSet()) {

      TransitionAction action = cell.getValue();
      if (action instanceof TransitionAction.Branch) {
        //For a branch, create a transition to represent each condition
        for (Map.Entry<String, TransitionAction> branchOption : ((TransitionAction.Branch) action).resultMap.entrySet()) {
          CommonStage newStage = resolveMoveAction(cell.getRowKey(), branchOption.getValue(), cell.getColumnKey());
          allTransitions.add(new GraphViewTransition(cell.getRowKey(), newStage, cell.getColumnKey().getEventMnemonic(), branchOption.getKey()));
        }
      } else {
        CommonStage newStage = resolveMoveAction(cell.getRowKey(), cell.getValue(), cell.getColumnKey());
        allTransitions.add(new GraphViewTransition(cell.getRowKey(), newStage, cell.getColumnKey().getEventMnemonic(), null));
      }
    }

    decisionLogicMap.forEach((stage, decisionLogic) -> {
      decisionLogic.getConditionMap().forEach((conditionValue, action) -> {
        allTransitions.add(new GraphViewTransition(stage, resolveMoveAction(null, action, null), conditionValue, null));
      });
    });

    return allTransitions;
  }

}
