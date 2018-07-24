package components.common.client;

import static components.common.client.RequestUtil.parse;
import static components.common.client.RequestUtil.parseList;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import filters.common.JwtRequestFilter;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.customer.api.view.CustomerView;
import uk.gov.bis.lite.customer.api.view.SiteView;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class CustomerServiceClient {

  private static final String SERVICE_ADMIN_SERVLET_PING_PATH = "/admin/ping";
  private static final String CUSTOMER_SERVICE = "customer-service";

  private static final String GET_CUSTOMER_PATH = "%s/customers/%s";
  private static final String GET_CUSTOMERS_PATH = "%s/user-customers/user/%s";
  private static final String GET_SITE_PATH = "%s/sites/%s";
  private static final String GET_SITES_PATH = "%s/user-sites/customer/%s/user/%s";

  private final String address;
  private final int timeout;
  private final String credentials;
  private final WSClient wsClient;
  private final HttpExecutionContext context;
  private final JwtRequestFilter jwtRequestFilter;

  @Inject
  public CustomerServiceClient(@Named("customerServiceAddress") String address,
                               @Named("customerServiceTimeout") int timeout,
                               @Named("customerServiceCredentials") String credentials,
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
        .get()
        .handleAsync((response, error) -> response.getStatus() == 200);
  }

  public CompletionStage<List<SiteView>> getSitesByCustomerIdUserId(String customerId, String userId) {
    String url = String.format(GET_SITES_PATH, address, customerId, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(ServiceClientLogger.requestFilter("Customer", "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parseList(request, response, error, CUSTOMER_SERVICE, "getSitesByCustomerIdUserId", SiteView[].class),
        context.current());
  }

  public CompletionStage<List<CustomerView>> getCustomersByUserId(String userId) {
    String url = String.format(GET_CUSTOMERS_PATH, address, userId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(ServiceClientLogger.requestFilter("Customer", "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parseList(request, response, error, CUSTOMER_SERVICE, "getCustomersByUserId", CustomerView[].class),
        context.current());
  }

  public CompletionStage<CustomerView> getCustomer(String customerId) {
    String url = String.format(GET_CUSTOMER_PATH, address, customerId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("Customer", "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parse(request, response, error, CUSTOMER_SERVICE, "getCustomer", CustomerView.class),
        context.current());
  }

  public CompletionStage<SiteView> getSite(String siteId) {
    String url = String.format(GET_SITE_PATH, address, siteId);
    WSRequest request = wsClient.url(url)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("Customer", "GET", context))
        .setRequestFilter(jwtRequestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));
    return request.get().handleAsync((response, error) ->
            parse(request, response, error, CUSTOMER_SERVICE, "getSite", SiteView.class),
        context.current());
  }

}
