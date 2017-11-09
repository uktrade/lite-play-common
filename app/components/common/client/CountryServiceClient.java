package components.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import components.common.logging.CorrelationId;
import models.common.Country;
import play.Logger;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class CountryServiceClient {

  public enum CountryServiceEndpoint {
    SET,
    GROUP
  }

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final int countryServiceTimeout;
  private final String countryServiceUrl;
  private final CountryServiceEndpoint countryServiceEndpoint;
  private final String countryParamName;
  private final ObjectMapper objectMapper;

  public CountryServiceClient(HttpExecutionContext httpExecutionContext,
                              WSClient wsClient,
                              int countryServiceTimeout,
                              String countryServiceUrl,
                              CountryServiceEndpoint countryServiceEndpoint,
                              String countryParamName,
                              ObjectMapper objectMapper) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.countryServiceTimeout = countryServiceTimeout;
    this.countryServiceUrl = countryServiceUrl;
    this.countryServiceEndpoint = countryServiceEndpoint;
    this.countryParamName = countryParamName;
    this.objectMapper = objectMapper;
  }

  public CompletionStage<List<Country>> getCountries() {
    return wsClient.url(buildUrl())
      .setRequestFilter(CorrelationId.requestFilter)
      .setRequestTimeout(countryServiceTimeout)
      .get().handleAsync((result, error) -> {
        if (error != null) {
          Logger.error("Country service client failure.", error);
        } else if (result.getStatus() != 200) {
          Logger.error("Country service error - {}", result.getBody());
        } else {
          try {
            String json = result.asJson().toString();
            return objectMapper.readValue(json, new TypeReference<List<Country>>() {
            });
          } catch (IOException e) {
            Logger.error("Failed to parse Country service response as JSON.", e);
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
      default:
        return null;
    }
  }

  public static CountryServiceClient buildCountryServiceGroupClient(HttpExecutionContext httpExecutionContext, WSClient wsClient, int countryServiceTimeout, String countryServiceUrl, String countryParamName, ObjectMapper objectMapper) {
    return new CountryServiceClient(httpExecutionContext, wsClient, countryServiceTimeout, countryServiceUrl, CountryServiceEndpoint.GROUP, countryParamName, objectMapper);
  }

  public static CountryServiceClient buildCountryServiceSetClient(HttpExecutionContext httpExecutionContext, WSClient wsClient, int countryServiceTimeout, String countryServiceUrl, String countryParamName, ObjectMapper objectMapper) {
    return new CountryServiceClient(httpExecutionContext, wsClient, countryServiceTimeout, countryServiceUrl, CountryServiceEndpoint.SET, countryParamName, objectMapper);
  }

}

