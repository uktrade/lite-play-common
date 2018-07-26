package pact.consumer.components.common.client;

import filters.common.JwtRequestFilter;
import play.libs.ws.WSRequestExecutor;

public class JwtTestRequestFilter extends JwtRequestFilter {

  JwtTestRequestFilter() {
    super(null, null, null);
  }

  @Override
  public WSRequestExecutor apply(WSRequestExecutor executor) {
    return request -> {
      request.addHeader("Authorization", "Bearer " + TestHelper.JWT_TOKEN);
      return executor.apply(request);
    };
  }

}
