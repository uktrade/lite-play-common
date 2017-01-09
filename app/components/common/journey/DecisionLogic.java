package components.common.journey;

import java.util.Map;
import java.util.function.Function;

/**
 * Encapsulates the built logic of a decision stage.
 */
class DecisionLogic {

  private final Decider<Object> decider;
  private final Function<Object, Object> decisionResultConverter;
  private final Map<String, TransitionAction> conditionMap;
  private final TransitionAction elseCondition;

  public DecisionLogic(Decider<?> decider, Function<?, ?> decisionResultConverter, Map<String, TransitionAction> conditionMap,
                       TransitionAction elseCondition) {
    this.decider = (Decider<Object>) decider;
    this.decisionResultConverter = (Function<Object, Object>) decisionResultConverter;
    this.conditionMap = conditionMap;
    this.elseCondition = elseCondition;
  }

  public Decider<Object> getDecider() {
    return decider;
  }

  public Function<Object, Object> getDecisionResultConverter() {
    return decisionResultConverter;
  }

  public Map<String, TransitionAction> getConditionMap() {
    return conditionMap;
  }

  public TransitionAction getElseCondition() {
    return elseCondition;
  }
}
