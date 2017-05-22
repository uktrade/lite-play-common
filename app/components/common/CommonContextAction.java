package components.common;

import com.google.inject.Inject;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * Action which sets up the Http Context with objects required by common library components.
 */
public class CommonContextAction extends Action<Void> {

  private final CommonContextActionSetup commonContextActionSetup;

  @Inject
  public CommonContextAction(CommonContextActionSetup commonContextActionSetup) {
    this.commonContextActionSetup = commonContextActionSetup;
  }

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {
    //Always set up the context with all common library requirements
    commonContextActionSetup.setupContext(ctx);

    return delegate.call(ctx);
  }
}
