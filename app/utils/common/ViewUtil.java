package utils.common;

import static play.mvc.Controller.ctx;

import components.common.state.ContextParamManager;
import play.mvc.Call;

public class ViewUtil {

  public static ContextParamManager currentParamManager() {
    return (ContextParamManager) ctx().args.get(ContextParamManager.CTX_PARAM_NAME);
  }

  public static String urlFor(Call call) {
    return currentParamManager().addParamsToCall(call);
  }

}
