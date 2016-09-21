package components.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import models.common.Country;
import play.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static components.common.client.CountryServiceClient.CountryServiceResponse.failure;
import static components.common.client.CountryServiceClient.CountryServiceResponse.success;
import static components.common.client.CountryServiceClient.Status.ERROR;
import static components.common.client.CountryServiceClient.Status.SUCCESS;

public class CountryServiceClient {

  private final WSClient wsClient;
  private final String countryServiceHost;
  private final int countryServicePort;
  private final int countryServiceTimeout;
  private final ObjectMapper objectMapper;

  @Inject
  public CountryServiceClient(WSClient wsClient,
                              @Named("countryServiceHost") String countryServiceHost,
                              @Named("countryServicePort") int countryServicePort,
                              @Named("countryServiceTimeout") int countryServiceTimeout,
                              ObjectMapper objectMapper) {
    this.wsClient = wsClient;
    this.countryServiceHost = countryServiceHost;
    this.countryServicePort = countryServicePort;
    this.countryServiceTimeout = countryServiceTimeout;
    this.objectMapper = objectMapper;
  }

  public CompletionStage<CountryServiceResponse> getCountries() {
    WSRequest request = wsClient.url("http://" + countryServiceHost + ":" + countryServicePort + "/countries/set/export-control");
    request.setRequestTimeout(countryServiceTimeout);

    return request.get().handle((result, error) -> {
      if (error != null) {
        Logger.error("Country service client failure.", error);
      }
      else if (result.getStatus() != 200) {
        Logger.error("Country service error - {}", result.getBody());
      } else {
        try {
          String json = result.asJson().toString();
          List<Country> sites = objectMapper.readValue(json, new TypeReference<List<Country>>() {});
          return success(sites);
        } catch (IOException e) {
          Logger.error("Failed to parse Country service response as JSON.", e);
        }
      }

      return failure("Country service failure.");
    });
  }

  public static final class CountryServiceResponse {

    private final Status status;
    private final List<Country> countries;
    private final String errorMessage;

    private CountryServiceResponse(List<Country> countries) {
      this.status = SUCCESS;
      this.countries = countries;
      this.errorMessage = null;
    }

    private CountryServiceResponse(String errorMessage) {
      this.status = ERROR;
      this.errorMessage = errorMessage;
      this.countries = new ArrayList<>();
    }

    public static CountryServiceResponse success(List<Country> countries) {
      return new CountryServiceResponse(countries);
    }

    public static CountryServiceResponse failure(String errorMessage) {
      return new CountryServiceResponse(errorMessage);
    }

    public Status getStatus() {
      return status;
    }

    public boolean isOk() {
      return status == SUCCESS && !countries.isEmpty();
    }

    public List<Country> getCountries() {
      return countries;
    }

    public List<Country> getCountriesByRef(List<String> countryRefs) {
      // Returns in order of countryRefs
      List<Country> filteredCountries = new ArrayList<>();
      for (String countryRef : countryRefs) {
        Optional<Country> countryOptional = countries.stream()
            .filter(country -> countryRef.equals(country.getCountryRef()))
            .findFirst();
        if (countryOptional.isPresent()) {
          filteredCountries.add(countryOptional.get());
        }
      }
      return filteredCountries;
    }

    public Optional<Country> getCountryByRef(String countryRef) {
      if (countryRef == null || countryRef.isEmpty()) {
        return Optional.empty();
      }
      return countries.stream()
          .filter(country -> countryRef.equals(country.getCountryRef()))
          .findFirst();
    }

    public String getErrorMessage() {
      return errorMessage;
    }

  }

  public enum Status {
    SUCCESS, ERROR
  }

}

