package components.common.journey;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import components.common.journey.MoveAction.Direction;
import org.apache.commons.codec.digest.DigestUtils;
import play.mvc.Call;
import play.mvc.Result;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builder for defining one or more Journeys. Consumers should extend this class and implement the {@link #journeys} method
 * in order to define stages, transitions and branching logic for a journey. For more information see the documentation in
 * <tt>/docs/Journey.md</tt>.
 */
public abstract class JourneyDefinitionBuilder {

  /** Transitions (stage + event) mapped to defined branching/transition logic  */
  private final Table<JourneyStage, CommonJourneyEvent, ActionBuilderContainer> stageTransitionBuilderMap = HashBasedTable.create();

  /** All decision stages and their configured branching logic */
  private final Map<DecisionStage, DecisionBuilder> decisionBuilders = new HashMap<>();

  /** Stage names to registered Stages */
  private final Map<String, CommonStage> knownStages = new HashMap<>();

  /** Names to start stages/back links */
  private final Map<String, JourneyDefinitionOptions> definedJourneys = new HashMap<>();

  protected JourneyDefinitionBuilder() {
  }

  /**
   * Defines all transitions and journeys for this builder.
   */
  protected abstract void journeys();

  private static final class JourneyDefinitionOptions {
    private final CommonStage startStage;
    private final BackLink exitBackLink;

    private JourneyDefinitionOptions(CommonStage startStage, BackLink exitBackLink) {
      this.startStage = startStage;
      this.exitBackLink = exitBackLink;
    }
  }

  /**
   * Defines a journey which will be created by this Builder. All the transitions defined in the Builder will be available,
   * but the journey will start on the given stage. Using this signature, the first stage of the journey will not have a
   * back link.
   * @param journeyName Name of the journey (use a String constant).
   * @param startStage Stage which the journey will start on.
   */
  protected final void defineJourney(String journeyName, CommonStage startStage) {
    defineJourney(journeyName, new JourneyDefinitionOptions(startStage, null));
  }

  /**
   * Defines a journey which will be created by this Builder. All the transitions defined in the Builder will be available,
   * but the journey will start on the given stage. The user will be able to exit the journey using the given BackLink.
   * @param journeyName Name of the journey (use a String constant).
   * @param startStage Stage which the journey will start on.
   * @param exitBackLink BackLink which will be presented to the user on the initial stage of the journey.
   */
  protected final void defineJourney(String journeyName, CommonStage startStage, BackLink exitBackLink) {
    defineJourney(journeyName, new JourneyDefinitionOptions(startStage, exitBackLink));
  }

  private void defineJourney(String journeyName, JourneyDefinitionOptions journeyDefinitionOptions) {
    assertStageKnown(journeyDefinitionOptions.startStage);

    definedJourneys.merge(journeyName, journeyDefinitionOptions, (a, b) -> {
      throw new JourneyDefinitionException("Journey " + journeyName + " is already defined in this Builder");
    });
  }

  /**
   * Builds all the JourneyDefinitions known to this builder. Typically this method should only be called by the
   * JourneyManager.
   * @return All built JourneyDefinitions.
   */
  public final Collection<JourneyDefinition> buildAll() {

    journeys();

    if (definedJourneys.size() == 0) {
      throw new JourneyDefinitionException("No journeys have been defined in this Builder");
    } else {
      return definedJourneys.entrySet().stream().map(e -> build(e.getKey(), e.getValue())).collect(Collectors.toSet());
    }
  }

  private JourneyDefinition build(String journeyName, JourneyDefinitionOptions options) {

    Table<JourneyStage, CommonJourneyEvent, TransitionAction> stageTransitionMap = HashBasedTable.create();

    //Build transitions
    for (Table.Cell<JourneyStage, CommonJourneyEvent, ActionBuilderContainer> cell : stageTransitionBuilderMap.cellSet()) {
      ActionBuilderContainer value = cell.getValue();
      if (value == null) {
        throw new JourneyDefinitionException(String.format("Null ActionBuilderContainer found in table row %s column %s",
            cell.getRowKey(), cell.getColumnKey()));
      }

      TransitionAction transitionAction = value.build();
      stageTransitionMap.put(cell.getRowKey(), cell.getColumnKey(), transitionAction);
    }

    //Build decisions
    Map<DecisionStage, DecisionLogic> decisionLogicMap = decisionBuilders.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));

    //Validate that all stages referenced by transition actions are registered
    validateStages(stageTransitionMap.values(), decisionLogicMap.values());

    //Filter out decision stages to create a map of stage hashes to JourneyStages
    Map<String, JourneyStage> knownJourneyStages = knownStages.values().stream()
        .filter(e -> e instanceof JourneyStage)
        .map(e -> (JourneyStage) e)
        .collect(Collectors.toMap(JourneyStage::getHash, Function.identity(), (a, b) -> {
          throw new JourneyDefinitionException(String.format("Hash collision caused by stages %s and %s ",
              a.getInternalName(), b.getInternalName()));
        }));

    return new JourneyDefinition(journeyName, stageTransitionMap, decisionLogicMap, knownJourneyStages,
        options.startStage, options.exitBackLink);
  }

  /**
   * Validates that all TransitionActions in the JourneyDefinition about to be built are registered on this Builder.
   */
  private void validateStages(Collection<TransitionAction> standardTransitionActions, Collection<DecisionLogic> decisions) {
    // Flatten TransitionActions (moves/branches) into their ultimate destination stages. These may come from standard
    // transitions, or the branch map/else condition of decision stages. Concat the 3 sources together so only one stream
    // needs to be searched.
    Set<CommonStage> unregisteredStages = Stream.of(
          stagesFromActions(standardTransitionActions).stream(),
          decisions.stream().flatMap(e -> stagesFromActions(e.getConditionMap().values()).stream()),
          decisions.stream()
              .filter(e -> e.getElseCondition() != null)
              .flatMap(e -> stagesFromActions(Collections.singleton(e.getElseCondition())).stream())
        )
        .flatMap(Function.identity())
        .filter(e -> !knownStages.containsKey(e.getInternalName()))
        .collect(Collectors.toSet());

    if (unregisteredStages.size() > 0) {
      String unregistered = unregisteredStages.stream()
          .map(CommonStage::getInternalName)
          .sorted()
          .collect(Collectors.joining(", "));

      throw new JourneyDefinitionException("Stages are not registered: " + unregistered);
    }
  }

  /**
   * Extracts the destination stages from the given actions. For BranchActions, the destinations are taken from the branch
   * map or else conditions. For MoveActions the destination is taken directly. This method is called recursively as
   * BranchActions contain MoveActions.
   * @param actions TransitionAction to extract stages from.
   * @return Stages which the TransitionActions eventually resolve to.
   */
  private static Collection<CommonStage> stagesFromActions(Collection<TransitionAction> actions) {
    Collection<CommonStage> stages = new HashSet<>();
    for (TransitionAction action : actions) {
      if (action instanceof BranchAction) {
        BranchAction branchAction = ((BranchAction) action);
        stages.addAll(stagesFromActions(branchAction.resultMap.values()));
        if (branchAction.elseTransition != null) {
          stages.addAll(stagesFromActions(Collections.singleton(branchAction.elseTransition)));
        }
      } else if (action instanceof MoveAction) {
        stages.add(((MoveAction) action).getDestinationStage());
      } else {
        throw new JourneyDefinitionException("Unknown TransitionAction type");
      }
    }
    return stages;
  }

  /**
   * Defines a stage for this journey which will perform arbitrary processing to generate a <tt>Result</tt> to send to the user.
   * Typically this will be a call to a controller's render method. Only use this signature if it is not possible to
   * provide a direct route to the desired form using a <tt>Call</tt> - this is preferred as it allows stages to be linked
   * to directly. Use this variant if no special back link prompt is required.
   * @param name Internal stage name, for debugging and logging.
   * @param formRenderSupplier Supplier which can produce a Play Result.
   * @return A new Stage. This object must be used when defining transitions.
   */
  protected final JourneyStage defineStage(String name, Supplier<CompletionStage<Result>> formRenderSupplier) {
    return defineStage(name, null, null, formRenderSupplier);
  }

  /**
   * Defines a stage for this journey which will perform arbitrary processing to generate a <tt>Result</tt> to send to the user.
   * Typically this will be a call to a controller's render method. Only use this signature if it is not possible to
   * provide a direct route to the desired form using a <tt>Call</tt> - this is preferred as it allows stages to be linked
   * to directly.
   * @param name Internal stage name, for debugging and logging.
   * @param backLinkPrompt Prompt for the back link which will navigate back to this stage.
   * @param formRenderSupplier Supplier which can produce a Play Result.
   * @return A new Stage. This object must be used when defining transitions.
   */
  protected final JourneyStage defineStage(String name, String backLinkPrompt, Supplier<CompletionStage<Result>> formRenderSupplier) {
    return defineStage(name, backLinkPrompt, null, formRenderSupplier);
  }

  /**
   * Defines a stage for this journey using a <tt>Call</tt> which the user is redirected to. This is the preferred way to
   * define a stage. Use this variant if no special back link prompt is required.
   * @param name Internal stage name, for debugging and logging.
   * @param call Call which will render the stage.
   * @return A new Stage. This object must be used when defining transitions.
   */
  protected final JourneyStage defineStage(String name, Call call) {
    return defineStage(name, null, call, null);
  }

  /**
   * Defines a stage for this journey using a <tt>Call</tt> which the user is redirected to. This is the preferred way to
   * define a stage.
   * @param name Internal stage name, for debugging and logging.
   * @param backLinkPrompt Prompt for the back link which will navigate back to this stage.
   * @param call Call which will render the stage.
   * @return A new Stage. This object must be used when defining transitions.
   */
  protected final JourneyStage defineStage(String name, String backLinkPrompt, Call call) {
    return defineStage(name, backLinkPrompt, call, null);
  }

  private JourneyStage defineStage(String name, String backLinkPrompt, Call call,
                                   Supplier<CompletionStage<Result>> formRenderSupplier) {
    if (!(call != null ^ formRenderSupplier != null)) {
      throw new IllegalArgumentException("Call and formRendererSupplier are mutually exclusive arguments");
    }

    if (knownStages.containsKey(name)) {
      throw new JourneyDefinitionException(String.format("Stage named %s is already registered", name));
    } else {
      String hash = DigestUtils.sha1Hex(name).substring(0, 5);

      JourneyStage journeyStage;
      if (call != null) {
        journeyStage = new CallableJourneyStage(hash, name, backLinkPrompt, call);
      } else {
        journeyStage = new RenderedJourneyStage(hash, name, backLinkPrompt, formRenderSupplier);
      }

      knownStages.put(name, journeyStage);

      return journeyStage;
    }
  }

  /**
   * Defines a decision stage where the result of a Decider is used directly as the decision argument.
   * @param name Internal name of the stage, for use in logging and debugging. e.g. "hasSoftwareControls".
   * @param decider Decider which produces the argument value to be used when making the decision.
   * @param <T> Type produced by the decider, for use in branching logic. Requires a comparable toString value.
   * @return New DecisionStage.
   */
  protected final <T> DecisionStage<T> defineDecisionStage(String name, Decider<T> decider) {
    return defineDecisionStage(name, decider, Function.identity());
  }

  /**
   * Defines a decision stage with a converter, which takes the result of the Decider and converts it to a different
   * type. This means Deciders can be generic and individual decision stages can specialise their decision handling
   * as appropriate.
   * @param name Internal name of the stage, for use in logging and debugging. e.g. "hasSoftwareControls".
   * @param decider Decider which is used to produce an initial result object.
   * @param converter Function to convert the decider's result into another result type.
   * @param <T> Type produced by the Decider.
   * @param <U> Type produced by the converter, for use in branching logic. Requires a comparable toString value.
   * @return New DecisionStage.
   */
  protected final <T, U> DecisionStage<U> defineDecisionStage(String name, Decider<T> decider, Function<T, U> converter) {
    DecisionStage<U> decisionStage = new DecisionStage<>(name, decider, converter);

    knownStages.merge(name, decisionStage,
        (a, b) -> { throw new JourneyDefinitionException("Duplicate definition of decision stage with name " + name); });

    return decisionStage;
  }

  /**
   * Initial step of a fluent interface chain. Starts defining a transition for the given stage.
   * @param stage Must be registered on this Builder.
   * @return Fluent interface step.
   */
  protected final StageBuilder atStage(JourneyStage stage) {
    assertStageKnown(stage);
    return new StageBuilder(stage);
  }

  /**
   * Initial step of a fluent interface chain. Starts defining a transition for the given decision stage.
   * @param stage Must be registered on this Builder.
   * @return Fluent interface step.
   */
  protected final <T> DecisionBuilder<T> atDecisionStage(DecisionStage<T> stage) {
    assertStageKnown(stage);
    return new DecisionBuilder<>(stage);
  }

  private void assertStageKnown(CommonStage stage) {
    if (!knownStages.containsKey(stage.getInternalName())) {
      throw new JourneyDefinitionException("Unknown stage " + stage.getInternalName());
    }
  }

  protected final class StageBuilder {

    private final JourneyStage stage;
    private CommonJourneyEvent event;

    private StageBuilder(JourneyStage stage) {
      this.stage = stage;
    }

    private void addIfNotDefined(JourneyStage stage, CommonJourneyEvent event, ActionBuilderContainer resultBuilderContainer) {
      if (stageTransitionBuilderMap.contains(stage, event)) {
        throw new JourneyDefinitionException(String.format("Transition for %s, %s has already been defined", stage, event));
      } else {
        stageTransitionBuilderMap.put(stage, event, resultBuilderContainer);
      }
    }

    /**
     * Intermediate fluent interface step. Specifies the JourneyEvent to define an action for.
     * @param event Any JourneyEvent.
     * @return Fluent interface step.
     */
    public NoParamEventBuilder onEvent(JourneyEvent event) {

      this.event = event;

      NoParamEventBuilder eventBuilder = new NoParamEventBuilder(this);
      addIfNotDefined(stage, event, eventBuilder);
      return eventBuilder;
    }

    /**
     * Intermediate fluent interface step. Specifies the ParameterisedJourneyEvent to define an action for.
     * @param event Any ParameterisedJourneyEvent.
     * @return Fluent interface step.
     */
    public <T> ParamEventBuilder<T> onEvent(ParameterisedJourneyEvent<T> event) {

      this.event = event;

      ParamEventBuilder<T> eventBuilder = new ParamEventBuilder<>(this);
      addIfNotDefined(stage, event, eventBuilder);
      return eventBuilder;
    }
  }


  protected final class DecisionBuilder<T> {

    private final DecisionStage<T> stage;
    private final DecisionBranchBuilder<T> branchBuilder = new DecisionBranchBuilder<>();

    private DecisionBuilder(DecisionStage<T> stage) {
      this.stage = stage;

      if (decisionBuilders.containsKey(stage)) {
        throw new JourneyDefinitionException(String.format("Decision for %s has already been defined", stage));
      } else {
        decisionBuilders.put(stage, this);
      }
    }

    /**
     * Intermediate fluent interface step. Starts defining the branches for a decision value produced by the current
     * DecisionStage's Decider.
     * @return Fluent interface step.
     */
    public DecisionBranchBuilder<T> decide() {
      return branchBuilder;
    }

    private DecisionLogic build() {
      Map<Object, TransitionAction> transitionActionMap = branchBuilder.conditionMap.entrySet()
          .stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().build()));

      TransitionAction elseCondition = null;
      if (branchBuilder.elseCondition != null) {
        elseCondition = branchBuilder.elseCondition.build();
      }
      return new DecisionLogic(stage.getDecider(), stage.getConverter(), transitionActionMap, elseCondition);
    }
  }

  protected final class DecisionBranchBuilder<T> {

    private final Map<Object, TransitionActionBuilder> conditionMap = new HashMap<>();
    private TransitionActionBuilder elseCondition = null;

    /**
     * Defines a journey branch for a certain value produced by a Decider.
     * @param criterionValue Value to match on (using Object.equals)
     * @param actionBuilder Action to undertake (e.g. <tt>moveTo(nextStage)</tt>)
     * @return Self-reference for method chaining.
     */
    public DecisionBranchBuilder<T> when(T criterionValue, TransitionActionBuilder actionBuilder) {
      conditionMap.merge(criterionValue, actionBuilder,
          (k,v) -> {throw new JourneyDefinitionException(String.format("Action for branch value %s already defined", criterionValue));});

      return this;
    }

    /**
     * Defines a catch-all branch in case no other <tt>when()</tt> conditions were matched. This is a terminal step of
     * a decision definition.
     * @param actionBuilder Action to undertake (e.g. <tt>moveTo(nextStage)</tt>)
     */
    public void otherwise(TransitionActionBuilder actionBuilder) {
      if (elseCondition != null) {
        throw new JourneyDefinitionException("otherwise() already defined for this decision branch");
      }
      else {
        elseCondition = actionBuilder;
      }
    }
  }

  protected static abstract class ActionBuilderContainer {

    private final StageBuilder owningStageBuilder;

    protected TransitionActionBuilder transitionActionBuilder = null;

    protected ActionBuilderContainer(StageBuilder owningStageBuilder) {
      this.owningStageBuilder = owningStageBuilder;
    }

    /**
     * Terminal fluent interface step. Specifies an unconditional action which should always be performed for the
     * current event.
     * @param transitionActionBuilder Action to undertake (e.g. <tt>moveTo(nextStage)</tt>)
     */
    public void then(TransitionActionBuilder transitionActionBuilder) {
      this.transitionActionBuilder = transitionActionBuilder;
    }

    private TransitionAction build() {

      if (transitionActionBuilder == null) {
        throw new JourneyDefinitionException(String.format("No transition action was defined for %s %s",
            owningStageBuilder.stage, owningStageBuilder.event));
      } else {
        return transitionActionBuilder.build();
      }
    }
  }

  private static class BranchActionBuilder implements TransitionActionBuilder {

    private final BranchBuilder<?, ?> branchBuilder;

    public BranchActionBuilder(BranchBuilder<?, ?> branchBuilder) {
      this.branchBuilder = branchBuilder;
    }

    @Override
    public TransitionAction build() {
      return branchBuilder.build();
    }
  }

  protected static final class NoParamEventBuilder extends ActionBuilderContainer {

    public NoParamEventBuilder(StageBuilder owningStageBuilder) {
      super(owningStageBuilder);
    }
  }

  protected static final class ParamEventBuilder<T> extends ActionBuilderContainer {

    public ParamEventBuilder(StageBuilder owningStageBuilder) {
      super(owningStageBuilder);
    }

    /**
     * Intermediate fluent interface step. Starts defining branching logic based on the value of the current event's parameter.
     * @return Fluent interface step.
     */
    public BranchBuilder<T, T> branch() {
      BranchBuilder<T, T> branchBuilder = new BranchBuilder<>(Function.identity());
      transitionActionBuilder = new BranchActionBuilder(branchBuilder);
      return branchBuilder;
    }

    /**
     * Intermediate fluent interface step. Starts defining branching logic based on the converted value of an event parameter.
     * @param paramConverter Function for converting the event parameter to a different value (e.g. convert an enum to a
     *                       boolean for brevity).
     * @return Fluent interface step.
     */
    public <U> BranchBuilder<T, U> branchWith(Function<T, U> paramConverter) {
      BranchBuilder<T, U> branchBuilder = new BranchBuilder<>(paramConverter);
      transitionActionBuilder = new BranchActionBuilder(branchBuilder);
      return branchBuilder;
    }
  }

  private interface TransitionActionBuilder {
    TransitionAction build();
  }

  /**
   * @param <E> Event argument type
   * @param <T> Transition argument type
   */
  protected static final class BranchBuilder<E, T> {

    private final Function<E, T> eventArgumentConverter;
    private final Map<Object, TransitionActionBuilder> conditionMap = new HashMap<>();
    private TransitionActionBuilder elseCondition = null;

    public BranchBuilder(Function<E, T> eventArgumentConverter) {
      this.eventArgumentConverter = eventArgumentConverter;
    }

    /**
     * Defines a journey branch for a certain event parameter value.
     * @param criterionValue Value to match on (using Object.equals).
     * @param actionBuilder Action to undertake (e.g. <tt>moveTo(nextStage)</tt>)
     * @return Self-reference for method chaining.
     */
    public BranchBuilder<E, T> when(T criterionValue, TransitionActionBuilder actionBuilder) {

      if (!conditionMap.containsKey(criterionValue)) {
        conditionMap.put(criterionValue, actionBuilder);
      } else {
        throw new JourneyDefinitionException("Duplicate branch condition value: " + criterionValue);
      }

      return this;
    }

    /**
     * Defines a catch-all branch in case no other <tt>when()</tt> conditions were matched. This is a terminal step of
     * a decision definition.
     * @param actionBuilder Action to undertake (e.g. <tt>moveTo(nextStage)</tt>)
     */
    public void otherwise(TransitionActionBuilder actionBuilder) {
      if (elseCondition == null) {
        elseCondition = actionBuilder;
      } else {
        throw new JourneyDefinitionException("Otherwise condition has already been defined");
      }
    }

    private TransitionAction build() {
      Map<Object, TransitionAction> transitionActionMap = conditionMap.entrySet()
          .stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().build()));

      TransitionAction elseConditionAction = null;
      if (elseCondition != null) {
        elseConditionAction = elseCondition.build();
      }

      //Do a safe cast from <T, U> to <Object, Object>
      @SuppressWarnings("unchecked")
      Function<Object, Object> upcastConverter = (Function<Object, Object>) this.eventArgumentConverter;

      return new BranchAction(upcastConverter, transitionActionMap, elseConditionAction);
    }
  }

  /**
   * Defines an action which moves the user forward to the given stage.
   * @param stage Stage to go to.
   * @return Fluent interface chain component which should be passed to a transition definition method.
   */
  protected static TransitionActionBuilder moveTo(CommonStage stage) {
    return () -> new MoveAction(stage, Direction.FORWARD);
  }

  /**
   * Defines an action which moves the user backwards (i.e. pops history) to the given stage.
   * @param stage Stage to go back to.
   * @return Fluent interface chain component which should be passed to a transition definition method.
   */
  protected static TransitionActionBuilder backTo(JourneyStage stage) {
    return () -> new MoveAction(stage, Direction.BACKWARD);
  }
}
