package components.common.journey;

import com.google.common.collect.Table;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EVENT argument = argument provided when an event fires
 * TRANSITION argument = toString of argument object used to fire transition (can represent event argument,
 * or may have been supplied by another method)
 */
public class JourneyDefinition {

  private final String journeyName;

  private final Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap;

  private final Map<String, JourneyStage> registeredStages;

  private final JourneyStage startStage;

  private final BackLink exitBackLink;

  JourneyDefinition(String journeyName, Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap,
                    Map<String, JourneyStage> registeredStages, JourneyStage startStage, BackLink exitBackLink) {
    this.journeyName = journeyName;
    this.stageTransitionMap = stageTransitionMap;
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
    }
    else {
      return journeyStage;
    }
  }

  private CommonJourneyEvent resolveEventMnemonic(String mnemonic) {
    //TODO is there a better way?
    return stageTransitionMap.columnKeySet()
        .stream()
        .filter(e -> e.getEventMnemonic().equals(mnemonic))
        .findFirst()
        .orElseThrow(() -> new JourneyException(String.format("Even '%s' is not defined in this journey", mnemonic)));
  }


  public TransitionResult fireEvent(String currentStageHash, JourneyEvent event) {
    return fireEventInternal(currentStageHash, event, null);
  }

  public <T> TransitionResult fireEvent(String currentStageHash, ParameterisedJourneyEvent<T> event, T eventArgument) {
    return fireEventInternal(currentStageHash, event, eventArgument);
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

  private TransitionResult fireEventInternal(String currentStageHash, CommonJourneyEvent event, Object eventArgument) {

    //TODO validation: transition cannot be a loop

    JourneyStage currentStage = resolveStageFromHash(currentStageHash);

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

  public Optional<BackLink> getExitBackLink() {
    return Optional.ofNullable(exitBackLink);
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
