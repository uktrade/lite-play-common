package components.common.journey;

/**
 * Result wrapper for a journey transition.
 */
class TransitionResult {

  private final JourneyStage newStage;

  TransitionResult(JourneyStage newStage) {
    this.newStage = newStage;
  }

  /**
   * @return The stage which should be proceeded to as a result of this transition.
   */
  public JourneyStage getNewStage() {
    return newStage;
  }

}
