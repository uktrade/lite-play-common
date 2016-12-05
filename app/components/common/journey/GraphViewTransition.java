package components.common.journey;

/**
 * Encapsulates information about a Journey transition, for use by unit tests, journey graph serialization, etc.
 */
class GraphViewTransition {

  private final JourneyStage startStage;
  private final JourneyStage endStage;

  private final String eventName;
  private final String conditionValue;

  GraphViewTransition(JourneyStage startStage, JourneyStage endStage, String eventName, String conditionValue) {
    this.startStage = startStage;
    this.endStage = endStage;
    this.eventName = eventName;
    this.conditionValue = conditionValue;
  }

  public JourneyStage getStartStage() {
    return startStage;
  }

  public JourneyStage getEndStage() {
    return endStage;
  }

  public String getEventName() {
    return eventName;
  }

  public String getConditionValue() {
    return conditionValue;
  }

  public boolean isConditional() {
    return conditionValue != null;
  }
}
