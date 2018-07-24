package components.common.client;

import static components.common.client.RequestUtil.parse;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import filters.common.JwtRequestFilter;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.user.api.view.UserDetailsView;
import uk.gov.bis.lite.user.api.view.UserPrivilegesView;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class UserServiceClientJwt {

  private static final String USER_SERVICE = "user-service";

  private static final String USER_PRIVILEGES_PATH = "%s/user-privileges/%s";
  private static final String USER_DETAILS_PATH = "%s/user-details/%s";

  private final String address;
  private final int timeout;
  private final JwtRequestFilter jwtRequestFilter;
  private final WSClient wsClient;
  private final HttpExecutionContext context;

  @Inject
  public UserServiceClientJwt(@Named("userServiceAddress") String address,
                              @Named("userServiceTimeout") int timeout,
                              JwtRequestFilter jwtRequestFilter, WSClient wsClient,
                              HttpExecutionContext httpExecutionContext) {
    this.address = address;
    this.timeout = timeout;
    this.jwtRequestFilter = jwtRequestFilter;
    this.wsClient = wsClient;
    this.context = httpExecutionContext;
  }

  public CompletionStage<UserPrivilegesView> getUserPrivilegeView(String userId) {
    String url = String.format(USER_PRIVILEGES_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("User JWT", "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parse(request, response, error, USER_SERVICE, "getUserPrivilegeView", UserPrivilegesView.class),
        context.current());
  }

  public CompletionStage<UserDetailsView> getUserDetailsView(String userId) {
    String url = String.format(USER_DETAILS_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("User JWT", "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parse(request, response, error, USER_SERVICE, "getUserDetailsView", UserDetailsView.class),
        context.current());
  }
}
