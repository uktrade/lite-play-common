package controllers.common.journey;

import com.google.inject.Inject;
import components.common.CommonContextAction;
import components.common.journey.JourneyManager;
import play.mvc.Result;
import play.mvc.With;

import java.util.concurrent.CompletionStage;

@With(CommonContextAction.class)
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
