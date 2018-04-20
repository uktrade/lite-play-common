package components.common.client.userservice;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import uk.gov.bis.lite.user.api.view.UserAccountTypeView;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class UserServiceClientBasicAuth {

  private static final String USER_ACCOUNT_TYPE_PATH = "/user-account-type/";

  private final String address;
  private final int timeout;
  private final String credentials;
  private final WSClient wsClient;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public UserServiceClientBasicAuth(@Named("userServiceAddress") String address,
                                    @Named("userServiceTimeout") int timeout,
                                    @Named("userServiceCredentials") String credentials,
                                    WSClient wsClient,
                                    HttpExecutionContext httpExecutionContext) {
    this.address = address;
    this.timeout = timeout;
    this.credentials = credentials;
    this.wsClient = wsClient;
    this.httpExecutionContext = httpExecutionContext;
  }

  public CompletionStage<UserAccountTypeView> getUserAccountTypeView(String userId) {
    String url = address + USER_ACCOUNT_TYPE_PATH + userId;
    return wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .setAuth(credentials)
        .get()
        .handleAsync((response, error) -> {
          if (error != null) {
            String message = "Unable to get user account type view with id " + userId;
            throw new RuntimeException(message, error);
          } else if (response.getStatus() != 200) {
            String message = String.format("Unexpected HTTP status code %d. Unable to get user account type view with id %s",
                response.getStatus(), userId);
            throw new RuntimeException(message);
          } else {
            return Json.fromJson(response.asJson(), UserAccountTypeView.class);
          }
        }, httpExecutionContext.current());
  }
}
