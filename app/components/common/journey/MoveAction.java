package components.common.journey;

final class MoveAction implements TransitionAction {

  public enum Direction {
    FORWARD, BACKWARD
  }

  private final CommonStage destinationStage;
  private final Direction direction;

  MoveAction(CommonStage destinationStage, Direction direction) {
    this.destinationStage = destinationStage;
    this.direction = direction;
  }

  public CommonStage getDestinationStage() {
    return destinationStage;
  }

  public Direction getDirection() {
    return direction;
  }
}
