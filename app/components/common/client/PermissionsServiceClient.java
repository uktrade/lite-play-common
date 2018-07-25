package components.common.client;

import static components.common.client.RequestUtil.handle;
import static components.common.client.RequestUtil.parse;
import static components.common.client.RequestUtil.parseList;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import filters.common.JwtRequestFilter;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import uk.gov.bis.lite.permissions.api.RegisterOgelResponse;
import uk.gov.bis.lite.permissions.api.param.RegisterParam;
import uk.gov.bis.lite.permissions.api.view.LicenceView;
import uk.gov.bis.lite.permissions.api.view.OgelRegistrationView;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class PermissionsServiceClient {

  private static final String SERVICE_ADMIN_SERVLET_PING_PATH = "/admin/ping";
  private static final String PERMISSIONS_SERVICE = "permissions-service";
  private static final String REGISTER_OGEL_PATH = "%s/register-ogel";
  private static final String GET_OGEL_REGISTRATIONS_PATH = "%s/ogel-registrations/user/%s";
  private static final String GET_LICENCES_PATH = "%s/licences/user/%s";

  private final String address;
  private final int timeout;
  private final String credentials;
  private final WSClient wsClient;
  private final HttpExecutionContext context;
  private final JwtRequestFilter jwtRequestFilter;

  @Inject
  public PermissionsServiceClient(@Named("permissionsServiceAddress") String address,
                                  @Named("permissionsServiceTimeout") int timeout,
                                  @Named("permissionsServiceCredentials") String credentials,
                                  WSClient wsClient, HttpExecutionContext httpExecutionContext,
                                  JwtRequestFilter jwtRequestFilter) {
    this.address = address;
    this.timeout = timeout;
    this.credentials = credentials;
    this.wsClient = wsClient;
    this.context = httpExecutionContext;
    this.jwtRequestFilter = jwtRequestFilter;
  }

  public CompletionStage<Boolean> serviceReachable() {
    return wsClient.url(address + SERVICE_ADMIN_SERVLET_PING_PATH)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .setAuth(credentials)
        .get().handleAsync((response, error) -> {
          return handle(response, error, PERMISSIONS_SERVICE, "serviceReachable");
        }, context.current());
  }

  public CompletionStage<String> registerOgel(RegisterParam registerParam, String callbackUrl) {
    String url = String.format(REGISTER_OGEL_PATH, address);

    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter(PERMISSIONS_SERVICE, "POST", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .addQueryParameter("callbackUrl", callbackUrl);

    return request.post(Json.toJson(registerParam)).handleAsync((response, error) ->
            parse(request, response, error, PERMISSIONS_SERVICE, "registerOgel", RegisterOgelResponse.class).getRequestId(),
        context.current());
  }

  public CompletionStage<List<OgelRegistrationView>> getOgelRegistrations(String userId) {
    String url = String.format(GET_OGEL_REGISTRATIONS_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter(PERMISSIONS_SERVICE, "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));

    return request.get().handleAsync((response, error) ->
            parseList(request, response, error, PERMISSIONS_SERVICE, "getOgelRegistrations", OgelRegistrationView[].class),
        context.current());
  }

  public CompletionStage<OgelRegistrationView> getOgelRegistration(String userId, String registrationReference) {
    String url = String.format(GET_OGEL_REGISTRATIONS_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter(PERMISSIONS_SERVICE, "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .addQueryParameter("registrationReference", registrationReference);

    return request.get().handleAsync((response, error) ->
            parse(request, response, error, PERMISSIONS_SERVICE, "getOgelRegistration", OgelRegistrationView[].class),
        context.current())
        .thenApplyAsync(ogelRegistrationViews -> {
          if (ogelRegistrationViews.length == 1) {
            return ogelRegistrationViews[0];
          } else {
            String message = "Expected 1 ogelRegistrationView but actual count was " + ogelRegistrationViews.length;
            throw new ClientException(message);
          }
        }, context.current());
  }

  public CompletionStage<LicenceView> getLicence(String userId, String reference) {
    String url = String.format(GET_LICENCES_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter(PERMISSIONS_SERVICE, "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout))
        .addQueryParameter("licenceReference", reference);
    return request.get().handleAsync((response, error) ->
            parse(request, response, error, PERMISSIONS_SERVICE, "getLicence", LicenceView[].class),
        context.current())
        .thenApplyAsync(licences -> {
          if (licences.length == 1) {
            return licences[0];
          } else {
            String message = "Expected 1 licenceView but actual count was " + licences.length;
            throw new ClientException(message);
          }
        }, context.current());
  }

  public CompletionStage<List<LicenceView>> getLicences(String userId) {
    String url = String.format(GET_LICENCES_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter(PERMISSIONS_SERVICE, "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parseList(request, response, error, PERMISSIONS_SERVICE, "getLicences", LicenceView[].class),
        context.current());
  }

}
