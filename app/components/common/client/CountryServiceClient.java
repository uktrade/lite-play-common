package components.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
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

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final String countryServiceUrl;
  private final int countryServiceTimeout;
  private final ObjectMapper objectMapper;

  public CountryServiceClient(HttpExecutionContext httpExecutionContext,
                              WSClient wsClient, String countryServiceUrl,
                              int countryServiceTimeout, ObjectMapper objectMapper) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.countryServiceUrl = countryServiceUrl;
    this.countryServiceTimeout = countryServiceTimeout;
    this.objectMapper = objectMapper;
  }

  public CompletionStage<List<Country>> getCountries() {
    return wsClient.url(countryServiceUrl)
      .withRequestFilter(CorrelationId.requestFilter)
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

  // sf for group, for set. Pass base url (like other clients), specific endpoint as enum (request type), endpoint as free String

}

