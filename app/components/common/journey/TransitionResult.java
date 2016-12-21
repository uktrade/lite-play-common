package components.common.journey;

/**
 * Result wrapper for a journey transition.
 */
class TransitionResult {

  private final JourneyStage newStage;
  private final MoveAction.Direction direction;

  TransitionResult(JourneyStage newStage, MoveAction.Direction direction) {
    this.newStage = newStage;
    this.direction = direction;
  }

  /**
   * @return The stage which should be proceeded to as a result of this transition.
   */
  public JourneyStage getNewStage() {
    return newStage;
  }

  public MoveAction.Direction getDirection() {
    return direction;
  }
}
