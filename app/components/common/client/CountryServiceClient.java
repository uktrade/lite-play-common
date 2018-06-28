package components.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import components.common.logging.CorrelationId;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class CountryServiceClient {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CountryServiceClient.class);

  public enum CountryServiceEndpoint {
    SET,
    GROUP,
    ALL
  }

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final int countryServiceTimeout;
  private final String countryServiceUrl;
  private final String credentials;
  private final CountryServiceEndpoint countryServiceEndpoint;
  private final String countryParamName;
  private final ObjectMapper objectMapper;

  public CountryServiceClient(HttpExecutionContext httpExecutionContext,
                              WSClient wsClient,
                              int countryServiceTimeout,
                              String countryServiceUrl,
                              String credentials,
                              CountryServiceEndpoint countryServiceEndpoint,
                              String countryParamName,
                              ObjectMapper objectMapper) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.countryServiceTimeout = countryServiceTimeout;
    this.countryServiceUrl = countryServiceUrl;
    this.credentials = credentials;
    this.countryServiceEndpoint = countryServiceEndpoint;
    this.countryParamName = countryParamName;
    this.objectMapper = objectMapper;
  }

  public CompletionStage<List<CountryView>> getCountries() {
    return wsClient.url(buildUrl())
        .setAuth(credentials)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestTimeout(Duration.ofMillis(countryServiceTimeout))
        .get().handleAsync((result, error) -> {
          if (error != null) {
            LOGGER.error("Country service client failure.", error);
          } else if (result.getStatus() != 200) {
            LOGGER.error("Country service error - {}", result.getBody());
          } else {
            try {
              String json = result.asJson().toString();
              return objectMapper.readValue(json, new TypeReference<List<CountryView>>() {
              });
            } catch (IOException e) {
              LOGGER.error("Failed to parse Country service response as JSON.", e);
            }
          }

          return new ArrayList<>();
        }, httpExecutionContext.current());
  }

  private String buildUrl() {
    switch (countryServiceEndpoint) {
      case SET:
        return countryServiceUrl + "/countries/set/" + countryParamName;
      case GROUP:
        return countryServiceUrl + "/countries/group/" + countryParamName;
      case ALL:
        return countryServiceUrl + "/country-data";
      default:
        return null;
    }
  }

  public static CountryServiceClient buildCountryServiceAllClient(HttpExecutionContext httpExecutionContext,
                                                                  WSClient wsClient,
                                                                  int countryServiceTimeout,
                                                                  String countryServiceUrl,
                                                                  String credentials,
                                                                  ObjectMapper objectMapper) {
    return new CountryServiceClient(httpExecutionContext,
        wsClient,
        countryServiceTimeout,
        countryServiceUrl,
        credentials,
        CountryServiceEndpoint.ALL,
        null,
        objectMapper);
  }

  public static CountryServiceClient buildCountryServiceGroupClient(HttpExecutionContext httpExecutionContext,
                                                                    WSClient wsClient,
                                                                    int countryServiceTimeout,
                                                                    String countryServiceUrl,
                                                                    String credentials,
                                                                    String countryParamName,
                                                                    ObjectMapper objectMapper) {
    return new CountryServiceClient(httpExecutionContext,
        wsClient,
        countryServiceTimeout,
        countryServiceUrl,
        credentials,
        CountryServiceEndpoint.GROUP,
        countryParamName,
        objectMapper);
  }

  public static CountryServiceClient buildCountryServiceSetClient(HttpExecutionContext httpExecutionContext,
                                                                  WSClient wsClient,
                                                                  int countryServiceTimeout,
                                                                  String countryServiceUrl,
                                                                  String credentials,
                                                                  String countryParamName,
                                                                  ObjectMapper objectMapper) {
    return new CountryServiceClient(httpExecutionContext,
        wsClient,
        countryServiceTimeout,
        countryServiceUrl,
        credentials,
        CountryServiceEndpoint.SET,
        countryParamName,
        objectMapper);
  }

}

