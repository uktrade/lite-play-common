package components.common.client;

import static components.common.client.RequestUtil.parse;
import static components.common.client.RequestUtil.parseList;

import com.google.common.net.UrlEscapers;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import org.apache.commons.lang3.StringUtils;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.ogel.api.view.ApplicableOgelView;
import uk.gov.bis.lite.ogel.api.view.OgelFullView;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class OgelServiceClient {

  private static final String OGEL_SERVICE = "ogel-service";

  private static final String GET_OGEL_PATH = "%s/ogels/%s";
  private static final String GET_APPLICABLE_OGELS_PATH = "%s/applicable-ogels";

  private final String address;
  private final int timeout;
  private final String credentials;
  private final WSClient wsClient;
  private final HttpExecutionContext context;

  @Inject
  public OgelServiceClient(@Named("ogelServiceAddress") String address,
                           @Named("ogelServiceTimeout") int timeout,
                           @Named("ogelServiceCredentials") String credentials, WSClient wsClient,
                           HttpExecutionContext httpExecutionContext) {
    this.address = address;
    this.timeout = timeout;
    this.credentials = credentials;
    this.wsClient = wsClient;
    this.context = httpExecutionContext;
  }

  public CompletionStage<OgelFullView> getById(String ogelId) {
    String escapedId = UrlEscapers.urlFragmentEscaper().escape(ogelId);
    String url = String.format(GET_OGEL_PATH, address, escapedId);
    WSRequest request = wsClient.url(url)
        .setAuth(credentials)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("OGEL", "GET", context))
        .setRequestTimeout(Duration.ofMillis(timeout));

    return request.get().handleAsync((response, error) ->
            parse(request, response, error, OGEL_SERVICE, "getById", OgelFullView.class),
        context.current());
  }

  public CompletionStage<List<ApplicableOgelView>> get(String controlCode, String sourceCountry,
                                                       List<String> destinationCountries,
                                                       List<String> activityTypes, boolean showHistoricOgel) {
    String url = String.format(GET_APPLICABLE_OGELS_PATH, address);
    WSRequest request = wsClient.url(url)
        .setAuth(credentials)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("OGEL", "GET", context))
        .setRequestTimeout(Duration.ofMillis(timeout))
        .addQueryParameter("controlCode", controlCode)
        .addQueryParameter("sourceCountry", sourceCountry);
    destinationCountries.forEach(country -> request.addQueryParameter("destinationCountry", country));
    activityTypes.forEach(activityType -> request.addQueryParameter("activityType", activityType));

    return request.get().handleAsync((response, error) -> {
      List<ApplicableOgelView> applicableOgelViews =
          parseList(request, response, error, OGEL_SERVICE, "getById", ApplicableOgelView[].class);
      return filterHistoric(applicableOgelViews, showHistoricOgel);
    }, context.current());
  }

  /**
   * Removes historic ogels from results if required
   */
  private List<ApplicableOgelView> filterHistoric(List<ApplicableOgelView> views, boolean showHistoricOgel) {
    return views.stream()
        .filter(view -> showHistoricOgel || !StringUtils.containsIgnoreCase(view.getName(), "historic military goods"))
        .collect(Collectors.toList());
  }

}
