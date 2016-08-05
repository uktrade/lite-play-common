package components.common.journey;

import com.google.common.collect.Table;

/**
 * EVENT argument = argument provided when an event fires
 * TRANSITION argument = toString of argument object used to fire transition (can represent event argument,
 * or may have been supplied by another method)
 */
public class JourneyDefinition {

  private final String journeyName;

  private final Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap;

  private final JourneyStage startStage;

  JourneyDefinition(String journeyName, Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap, JourneyStage startStage) {
    this.journeyName = journeyName;
    this.stageTransitionMap = stageTransitionMap;
    this.startStage = startStage;
  }

  public String getJourneyName() {
    return journeyName;
  }

  JourneyStage getStartStage() {
    return startStage;
  }

  public JourneyStage resolveStageFromMnemonic(String mnemonic) {
    //TODO is there a better way?
    return stageTransitionMap.rowKeySet()
        .stream()
        .filter(e -> e.getMnemonic().equals(mnemonic))
        .findFirst()
        .orElseThrow(() -> new JourneyException(String.format("Stage '%s' is not defined in this journey", mnemonic)));
  }

  private CommonJourneyEvent resolveEventMnemonic(String mnemonic) {
    //TODO is there a better way?
    return stageTransitionMap.columnKeySet()
        .stream()
        .filter(e -> e.getEventMnemonic().equals(mnemonic))
        .findFirst()
        .orElseThrow(() -> new JourneyException(String.format("Even '%s' is not defined in this journey", mnemonic)));
  }


  public TransitionResult fireEvent(String currentStageName, JourneyEvent event) {
    return fireEventInternal(currentStageName, event, null);
  }

  public <T> TransitionResult fireEvent(String currentStageName, ParameterisedJourneyEvent<T> event, T eventArgument) {
    return fireEventInternal(currentStageName, event, eventArgument);
  }

  private TransitionResult resolveMoveAction(JourneyStage currentStage, TransitionAction transitionAction,
                                             CommonJourneyEvent event) {

    if (transitionAction instanceof TransitionAction.MoveStage) {

      TransitionAction.MoveStage moveStageAction = ((TransitionAction.MoveStage) transitionAction);

      JourneyStage destinationStage = moveStageAction.getDestinationStage();

      return new TransitionResultImpl(currentStage, destinationStage);
    }
    else {
      //TODO - handle journey end
      throw new JourneyException("Unknown action type " + transitionAction.getClass().getName(), currentStage, event);
    }
  }

  private TransitionResult fireEventInternal(String currentStageName, CommonJourneyEvent event, Object eventArgument) {

    //TODO validation: transition cannot be a loop

    JourneyStage currentStage = resolveStageFromMnemonic(currentStageName);

    TransitionAction transitionAction = stageTransitionMap.get(currentStage, event);
    if (transitionAction == null) {
      throw new JourneyException("No transition available for %s, %s", currentStage, event);
    }
    else {
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
        }
        else {
          throw new JourneyException("Branch not matched with argument " + transitionArgument, currentStage, event);
        }
      }
      else {
        return resolveMoveAction(currentStage, transitionAction, event);
      }
    }
  }

  private static class TransitionResultImpl implements TransitionResult {

    private final JourneyStage prevStage;
    private final JourneyStage newStage;

    public TransitionResultImpl(JourneyStage prevStage, JourneyStage newStage) {
      this.prevStage = prevStage;
      this.newStage = newStage;
    }

    @Override
    public JourneyStage getNewStage() {
      return newStage;
    }

    @Override
    public JourneyStage getPreviousStage() {
      return prevStage;
    }

    @Override
    public String getBackLinkText() {
      return prevStage.getDisplayName();
    }
  }
}
