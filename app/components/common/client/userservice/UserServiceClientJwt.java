package components.common.client.userservice;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import filters.common.JwtRequestFilter;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import uk.gov.bis.lite.user.api.view.UserDetailsView;
import uk.gov.bis.lite.user.api.view.UserPrivilegesView;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class UserServiceClientJwt {

  private static final String USER_PRIVILEGES_PATH = "/user-privileges/";
  private static final String USER_DETAILS_PATH = "/user-details/";

  private final String address;
  private final int timeout;
  private final JwtRequestFilter jwtRequestFilter;
  private final WSClient wsClient;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public UserServiceClientJwt(@Named("userServiceAddress") String address,
                              @Named("userServiceTimeout") int timeout,
                              JwtRequestFilter jwtRequestFilter,
                              WSClient wsClient,
                              HttpExecutionContext httpExecutionContext) {
    this.address = address;
    this.timeout = timeout;
    this.jwtRequestFilter = jwtRequestFilter;
    this.wsClient = wsClient;
    this.httpExecutionContext = httpExecutionContext;
  }

  public CompletionStage<UserPrivilegesView> getUserPrivilegeView(String userId) {
    String url = address + USER_PRIVILEGES_PATH + userId;
    return wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .get()
        .handleAsync((response, error) -> {
          if (error != null) {
            String message = "Unable to get user privileges view with id " + userId;
            throw new RuntimeException(message, error);
          } else if (response.getStatus() != 200) {
            String message = String.format("Unexpected HTTP status code %d. Unable to get user privileges view with id %s",
                response.getStatus(), userId);
            throw new RuntimeException(message);
          } else {
            return Json.fromJson(response.asJson(), UserPrivilegesView.class);
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<UserDetailsView> getUserDetailsView(String userId) {
    String url = address + USER_DETAILS_PATH + userId;
    return wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .get()
        .handleAsync((response, error) -> {
          if (error != null) {
            String message = "Unable to get user details view with id " + userId;
            throw new RuntimeException(message, error);
          } else if (response.getStatus() != 200) {
            String message = String.format("Unexpected HTTP status code %d. Unable to get user details view with id %s",
                response.getStatus(), userId);
            throw new RuntimeException(message);
          } else {
            return Json.fromJson(response.asJson(), UserDetailsView.class);
          }
        }, httpExecutionContext.current());
  }
}
