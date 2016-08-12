package components.common;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.state.ContextParamManager;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * ActionFactory to create an action which will correctly setup an HTTP context with required arguments before a request
 * is processed.
 */
public class CommonContextActionFactory {

  //TODO dynamic injection?
  private final JourneyManager journeyManager;
  private final ContextParamManager contextParamManager;

  @Inject
  public CommonContextActionFactory(JourneyManager journeyManager, ContextParamManager contextParamManager) {
    this.journeyManager = journeyManager;
    this.contextParamManager = contextParamManager;
  }

  public Action createAction(Http.Request request) {
    return new Action.Simple() {

      @Override
      public CompletionStage<Result> call(Http.Context ctx) {
        //Order is important!
        contextParamManager.setAllContextArgsFromRequest();
        journeyManager.setContextArguments();

        //Add a reference to the ContextParamManager to the context so views can see it (DI workaround)
        ctx.args.put(ContextParamManager.CTX_PARAM_NAME, contextParamManager);

        return delegate.call(ctx);
      }
    };
  }
}
