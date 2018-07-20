package pact.consumer.components.common.client;

import filters.common.JwtRequestFilter;
import play.libs.ws.WSRequestExecutor;

public class JwtTestRequestFilter extends JwtRequestFilter {

  public JwtTestRequestFilter() {
    super(null, null, null);
  }

  @Override
  public WSRequestExecutor apply(WSRequestExecutor executor) {
    return request -> {
      request.addHeader("Authorization", JwtTestHelper.JWT_AUTHORIZATION_HEADER_VALUE);
      return executor.apply(request);
    };
  }

}
