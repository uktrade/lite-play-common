package components.common.transaction;

import static play.mvc.Controller.ctx;

import play.mvc.Http;

public class StaticContextAccessorStrategy implements ContextAccessorStrategy {

  @Override
  public Http.Context getCurrentContext() {
    return ctx();
  }
}
