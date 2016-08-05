package components.common.journey;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JourneyDefinitionBuilder {

  private final Table<JourneyStage, CommonJourneyEvent, ActionBuilderContainer> stageTransitionBuilderMap = HashBasedTable.create();

  public JourneyDefinitionBuilder() {
  }


  public StageBuilder atStage(JourneyStage stage) {
    return new StageBuilder(stage);
  }


  public StageBuilder atAnyStage() {
    //wait till build(), iterate all known stages
    throw new JourneyDefinitionException("atAnyStage not yet supported");
  }

  public final class StageBuilder {

    private final JourneyStage stage;
    private CommonJourneyEvent event;

    private StageBuilder(JourneyStage stage) {
      this.stage = stage;
    }

    private void addIfNotDefined(JourneyStage stage, CommonJourneyEvent event, ActionBuilderContainer resultBuilderContainer) {
      if (stageTransitionBuilderMap.contains(stage, event)) {
        throw new JourneyDefinitionException(String.format("Transition for %s, %s has already been defined", stage, event));
      }
      else {
        stageTransitionBuilderMap.put(stage, event, resultBuilderContainer);
      }
    }

    public NoParamEventBuilder onEvent(JourneyEvent event) {

      this.event = event;

      NoParamEventBuilder eventBuilder = new NoParamEventBuilder(this);
      addIfNotDefined(stage, event, eventBuilder);
      return eventBuilder;
    }

    public <T> ParamEventBuilder<T> onEvent(ParameterisedJourneyEvent<T> event) {

      this.event = event;

      if (!event.getParamType().isAssignableFrom(String.class) && event.getParamType().isAssignableFrom(Number.class) &&
          !event.getParamType().isAssignableFrom(Enum.class) && !event.getParamType().isAssignableFrom(Boolean.class)) {
        throw new JourneyDefinitionException("Parameter type must be an Enum, String, Number or Boolean for event " +
            event);
      }

      ParamEventBuilder<T> eventBuilder = new ParamEventBuilder<>(this);
      addIfNotDefined(stage, event, eventBuilder);
      return eventBuilder;
    }
  }


  public static abstract class ActionBuilderContainer {

    private final StageBuilder owningStageBuilder;

    protected TransitionActionBuilder transitionActionBuilder = null; //could be 2 mutually exclusive fields

    protected ActionBuilderContainer(StageBuilder owningStageBuilder) {
      this.owningStageBuilder = owningStageBuilder;
    }

    public void then(TransitionActionBuilder transitionActionBuilder) {
      this.transitionActionBuilder = transitionActionBuilder;
    }

    private TransitionAction build() {

      if (transitionActionBuilder == null) {
        throw new JourneyDefinitionException(String.format("No transition action was defined for %s %s",
            owningStageBuilder.stage, owningStageBuilder.event));
      }
      else {
        return transitionActionBuilder.build();
      }
    }
  }

  private static class BranchActionBuilder extends TransitionActionBuilder {

    private final BranchBuilder<?, ?> branchBuilder;

    public BranchActionBuilder(BranchBuilder<?, ?> branchBuilder) {
      this.branchBuilder = branchBuilder;
    }

    @Override
    protected TransitionAction build() {
      return branchBuilder.build();
    }
  }

  public static class NoParamEventBuilder extends ActionBuilderContainer {

    public NoParamEventBuilder(StageBuilder owningStageBuilder) {
      super(owningStageBuilder);
    }

    public <T> BranchBuilder<T, T> branchWith(Supplier<T> paramSupplier) {
      BranchBuilder<T, T> branchBuilder = new BranchBuilder<>(paramSupplier);
      transitionActionBuilder = new BranchActionBuilder(branchBuilder);
      return branchBuilder;
    }
  }


  public static class ParamEventBuilder<T> extends ActionBuilderContainer {

    public ParamEventBuilder(StageBuilder owningStageBuilder) {
      super(owningStageBuilder);
    }

    public BranchBuilder<T, T> branch() {
      BranchBuilder<T, T> branchBuilder = new BranchBuilder<>(Function.identity());
      transitionActionBuilder = new BranchActionBuilder(branchBuilder);
      return branchBuilder;
    }

    public <U> BranchBuilder<T, U> branchWith(Function<T, U> paramConverter) {
      BranchBuilder<T, U> branchBuilder = new BranchBuilder<>(paramConverter);
      transitionActionBuilder = new BranchActionBuilder(branchBuilder);
      return branchBuilder;
    }
  }

  public static abstract class TransitionActionBuilder {
    protected abstract TransitionAction build();
  }


  /**
   * @param <E> Event argument type
   * @param <T> Transition argument type
   */
  public static class BranchBuilder<E, T> {

    private final Function<E, T> eventArgumentConverter;
    private final Supplier<T> transitionArgumentSupplier;
    private final Map<String, TransitionActionBuilder> conditionMap = new HashMap<>();
    private TransitionActionBuilder elseCondition = null;

    public BranchBuilder(Function<E, T> eventArgumentConverter) {
      this.eventArgumentConverter = eventArgumentConverter;
      transitionArgumentSupplier = null;
    }

    public BranchBuilder(Supplier<T> paramProvider) {
      this.eventArgumentConverter = null;
      this.transitionArgumentSupplier = paramProvider;
    }

    public BranchBuilder<E, T> when(T criterionValue, TransitionActionBuilder actionBuilder) {

      if (!conditionMap.containsKey(criterionValue.toString())) {
        conditionMap.put(criterionValue.toString(), actionBuilder);
      }
      else {
        //TODO more contextual error message
        throw new JourneyDefinitionException("Duplicate branch condition value: " + criterionValue.toString());
      }

      return this;
    }

    public void otherwise(TransitionActionBuilder actionBuilder) {

      if (elseCondition == null) {
        elseCondition = actionBuilder;
      }
      else {
        throw new JourneyDefinitionException("Otherwise condition has already been defined");
      }
    }

    private TransitionAction build() {
      Map<String, TransitionAction> transitionActionMap = conditionMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().build()));

      TransitionAction elseConditionAction = null;
      if (elseCondition != null) {
        elseConditionAction = elseCondition.build();
      }

      //TODO fix casting issue
      return new TransitionAction.Branch(transitionArgumentSupplier, (Function<Object, Object>) eventArgumentConverter, transitionActionMap, elseConditionAction);
    }
  }

  public static TransitionActionBuilder moveTo(JourneyStage stage) {

    return new TransitionActionBuilder() {
      @Override
      protected TransitionAction build() {
        return new TransitionAction.MoveStage(stage);
      }
    };
  }
//
//  private static final class MoveToActionBuilder implements TransitionActionBuilder {
//
//  }

  public static TransitionActionBuilder completeJourney() {
    throw new JourneyDefinitionException("Journey completion not supported");
  }

  public JourneyDefinition build(String journeyName, JourneyStage startStage) {

    Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap = HashBasedTable.create();

    for (Table.Cell<JourneyStage, CommonJourneyEvent, ActionBuilderContainer> cell : stageTransitionBuilderMap.cellSet()) {
      ActionBuilderContainer value = cell.getValue();
      if (value == null) {
        throw new JourneyDefinitionException(String.format("Null ActionBuilderContainer found in table row %s column %s", cell.getRowKey(), cell.getColumnKey()));
      }

      TransitionAction transitionAction = value.build();
      stageTransitionMap.put(cell.getRowKey(), cell.getColumnKey(), transitionAction);
    }

    return new JourneyDefinition(journeyName, stageTransitionMap, startStage);
  }

//  public static SubjourneyBuilder subjourney(JourneyDefinition jd) {
//
//    return new SubjourneyBuilder();
//  }
//
//  public static class SubjourneyBuilder {
//
//    public TransitionActionBuilder whenFinished(TransitionActionBuilder transitionActionBuilder) {
//      return transitionActionBuilder;
//    }
//  }
////
////  public static <T> BranchBuilder<T, T> branch(Supplier<T> paramSupplier){
////    return new BranchBuilder<T, T>(paramSupplier);
////
////  }


}
