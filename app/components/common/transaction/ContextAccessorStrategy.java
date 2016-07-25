package components.common.transaction;

import play.mvc.Http;

public interface ContextAccessorStrategy {

  Http.Context getCurrentContext();

}
