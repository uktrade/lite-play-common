package components.common.journey;

//@FunctionalInterface
public interface TransitionResult {

  JourneyStage getNewStage();

  JourneyStage getPreviousStage();

  String getBackLinkText();

}
