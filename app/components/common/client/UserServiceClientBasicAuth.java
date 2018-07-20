package components.common.client;

import static components.common.client.RequestUtil.parse;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.user.api.view.UserAccountTypeView;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class UserServiceClientBasicAuth {

  private static final String USER_SERVICE = "user-service";

  private static final String USER_ACCOUNT_TYPE_PATH = "%s/user-account-type/%s";

  private final String address;
  private final int timeout;
  private final String credentials;
  private final WSClient wsClient;
  private final HttpExecutionContext context;

  @Inject
  public UserServiceClientBasicAuth(@Named("userServiceAddress") String address,
                                    @Named("userServiceTimeout") int timeout,
                                    @Named("userServiceCredentials") String credentials,
                                    WSClient wsClient, HttpExecutionContext httpExecutionContext) {
    this.address = address;
    this.timeout = timeout;
    this.credentials = credentials;
    this.wsClient = wsClient;
    this.context = httpExecutionContext;
  }

  public CompletionStage<UserAccountTypeView> getUserAccountTypeView(String userId) {
    String url = String.format(USER_ACCOUNT_TYPE_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("User basic", "GET", context))
        .setRequestTimeout(Duration.ofMillis(timeout))
        .setAuth(credentials);
    return request.get().handleAsync((response, error) ->
            parse(request, response, error, USER_SERVICE, "getUserAccountTypeView", UserAccountTypeView.class),
        context.current());
  }
}
