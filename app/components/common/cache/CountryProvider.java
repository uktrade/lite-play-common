package components.common.cache;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.client.CountryServiceClient;
import models.common.Country;
import play.Logger;
import play.cache.CacheApi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class CountryProvider {

  private final CacheApi cache;
  private final CountryServiceClient countryServiceClient;
  private final int countryCacheExpiry;
  private final String countryCacheKey;

  @Inject
  public CountryProvider(CacheApi cache, CountryServiceClient countryServiceClient,
                         @Named("countryCacheExpiry") int countryCacheExpiry,
                         @Named("countryCacheKey") String countryCacheKey) {
    this.cache = cache;
    this.countryServiceClient = countryServiceClient;
    this.countryCacheExpiry = countryCacheExpiry;
    this.countryCacheKey = countryCacheKey;
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
    return Collections.unmodifiableMap(cache.get(countryCacheKey));
  }

  public void loadCountries() {
    Logger.info("Attempting to refresh the country cache....");
    countryServiceClient.getCountries()
      .thenAcceptAsync(c -> {
        if (c.getStatus() == CountryServiceClient.Status.SUCCESS) {
          Map<String, String> countries = c.getCountries().stream()
            .collect(toMap(Country::getCountryRef, Country::getCountryName));
          cache.set(countryCacheKey, countries, countryCacheExpiry);
          Logger.info("Successfully refreshed the country cache.");
        } else {
          Logger.error("Country service error - Failed to get countries.");
          cache.set(countryCacheKey, new HashMap<>(), countryCacheExpiry);
        }
      });
  }
}
