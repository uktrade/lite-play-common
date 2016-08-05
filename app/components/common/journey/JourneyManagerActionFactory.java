package components.common.journey;

import com.google.inject.Inject;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class JourneyManagerActionFactory {

  private final JourneyManager journeyManager;

  @Inject
  public JourneyManagerActionFactory(JourneyManager journeyManager) {
    this.journeyManager = journeyManager;
  }

  public Action createAction(Http.Request request) {
    return new Action.Simple() {

      @Override
      public CompletionStage<Result> call(Http.Context ctx) {
        journeyManager.setContextArguments();
        return delegate.call(ctx);
      }
    };
  }
}
