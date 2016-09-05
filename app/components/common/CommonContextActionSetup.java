package components.common;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.state.ContextParamManager;
import play.mvc.Http;

/**
 * Helper which sets up an HttpContext at the start of a request with all attributes required by the common library.
 */
public class CommonContextActionSetup {

  private final JourneyManager journeyManager;
  private final ContextParamManager contextParamManager;

  @Inject
  public CommonContextActionSetup(JourneyManager journeyManager, ContextParamManager contextParamManager) {
    this.journeyManager = journeyManager;
    this.contextParamManager = contextParamManager;
  }

  public void setupContext(Http.Context ctx) {
    //Order is important!
    contextParamManager.setAllContextArgsFromRequest();
    journeyManager.setContextArguments();

    //Add a reference to the ContextParamManager to the context so views can see it (DI workaround)
    ctx.args.put(ContextParamManager.CTX_PARAM_NAME, contextParamManager);
  }
}
