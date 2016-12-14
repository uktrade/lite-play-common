package components.common.cache;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.client.CountryServiceClient;
import models.common.Country;
import play.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class CountryProvider {

  private Map<String, Country> cache;
  private final CountryServiceClient countryServiceClient;

  @Inject
  public CountryProvider(CountryServiceClient countryServiceClient) {
    this.countryServiceClient = countryServiceClient;
  }

  public Country getCountry(String countryRef) {
    if (countryRef == null) {
      return null;
    }

    Map<String, Country> countries = getCountriesMap();
    return countries.get(countryRef);
  }

  public Collection<Country> getCountries() {
    Map<String, Country> countries = getCountriesMap();
    return countries.values();
  }

  public Map<String, Country> getCountriesMap() {
    return Collections.unmodifiableMap(cache);
  }

  public void loadCountries() {
    cache = new HashMap<>();
    Logger.info("Attempting to refresh the country cache....");
    countryServiceClient.getCountries()
      .thenAcceptAsync(c -> {
        if (c.getStatus() == CountryServiceClient.Status.SUCCESS) {
          cache = c.getCountries().stream()
            .collect(toMap(Country::getCountryRef, Function.identity()));
          Logger.info("Successfully refreshed the country cache.");
        } else {
          Logger.error("Country service error - Failed to get countries.");
          cache = new HashMap<>();
        }
      });
  }
}
