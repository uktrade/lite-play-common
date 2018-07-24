package components.common.client;

import static components.common.client.RequestUtil.parseList;

import components.common.logging.CorrelationId;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class CountryServiceClient {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CountryServiceClient.class);

  private static final String COUNTRY_SERVICE = "country-service";

  private static final String GET_COUNTRIES_SET_URL = "%s/countries/set/%s";
  private static final String GET_COUNTRIES_GROUP_URL = "%s/countries/group/%s";
  private static final String GET_COUNTRY_DATA_URL = "%s/country-data";

  public enum CountryServiceEndpoint {
    SET,
    GROUP,
    ALL
  }

  private final String url;
  private final int timeout;
  private final String credentials;
  private final WSClient wsClient;
  private final HttpExecutionContext context;

  public CountryServiceClient(String address, int timeout, String credentials,
                              WSClient wsClient, HttpExecutionContext httpExecutionContext,
                              CountryServiceEndpoint countryServiceEndpoint,
                              String countryParamName) {
    this.timeout = timeout;
    this.credentials = credentials;
    this.wsClient = wsClient;
    this.context = httpExecutionContext;
    this.url = buildUrl(address, countryServiceEndpoint, countryParamName);
  }

  public CompletionStage<List<CountryView>> getCountries() {
    WSRequest request = wsClient.url(url)
        .setAuth(credentials)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestTimeout(Duration.ofMillis(timeout));

    return request.get().handleAsync((response, error) ->
            parseList(request, response, error, COUNTRY_SERVICE, "getCountries", CountryView[].class),
        context.current())
        .exceptionally(error -> {
          LOGGER.error("Unable to get countries.", error);
          return new ArrayList<>();
        });
  }

  private String buildUrl(String address, CountryServiceEndpoint countryServiceEndpoint, String countryParamName) {
    switch (countryServiceEndpoint) {
      case SET:
        return String.format(GET_COUNTRIES_SET_URL, address, countryParamName);
      case GROUP:
        return String.format(GET_COUNTRIES_GROUP_URL, address, countryParamName);
      case ALL:
        return String.format(GET_COUNTRY_DATA_URL, address);
      default:
        return null;
    }
  }

  public static CountryServiceClient buildCountryServiceAllClient(String countryServiceUrl, int countryServiceTimeout,
                                                                  String credentials, WSClient wsClient,
                                                                  HttpExecutionContext httpExecutionContext) {
    return new CountryServiceClient(countryServiceUrl, countryServiceTimeout, credentials, wsClient, httpExecutionContext,
        CountryServiceEndpoint.ALL, null);
  }

  public static CountryServiceClient buildCountryServiceGroupClient(String countryServiceUrl, int countryServiceTimeout,
                                                                    String credentials, WSClient wsClient,
                                                                    HttpExecutionContext httpExecutionContext,
                                                                    String countryParamName) {
    return new CountryServiceClient(countryServiceUrl, countryServiceTimeout, credentials, wsClient, httpExecutionContext,
        CountryServiceEndpoint.GROUP, countryParamName);
  }

  public static CountryServiceClient buildCountryServiceSetClient(String countryServiceUrl, int countryServiceTimeout,
                                                                  String credentials, WSClient wsClient,
                                                                  HttpExecutionContext httpExecutionContext,
                                                                  String countryParamName) {
    return new CountryServiceClient(countryServiceUrl, countryServiceTimeout, credentials, wsClient, httpExecutionContext,
        CountryServiceEndpoint.SET, countryParamName);
  }

}

