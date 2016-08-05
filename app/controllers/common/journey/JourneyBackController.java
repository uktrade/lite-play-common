package controllers.common.journey;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class JourneyBackController {

  private final JourneyManager journeyManager;

  @Inject
  public JourneyBackController(JourneyManager journeyManager) {
    this.journeyManager = journeyManager;
  }

  public CompletionStage<Result> handleBack() {
    return journeyManager.navigateBack();

  }

}
