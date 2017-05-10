package components.common.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import components.common.client.CountryServiceClient;
import models.common.Country;
import play.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class CountryProvider {

  private Map<String, Country> cache = new HashMap<>();
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

  /**
   * Get a collection of countries
   * @return a collection of Country
   */
  public Collection<Country> getCountries() {
    Map<String, Country> countries = getCountriesMap();
    return countries.values();
  }

  /**
   * Get a list a countries ordered by {@code countryComparator}
   * @param countryComparator the Country comparator
   * @return an ordered list of Country
   */
  public List<Country> getCountries(Comparator<Country> countryComparator) {
    return getCountries().stream().sorted(countryComparator).collect(Collectors.toList());
  }

  /**
   * Get a list of countries ordered by {@link Country#getCountryName()}
   * @return an ordered list of Country
   */
  public List<Country> getCountriesOrderedByName() {
    return getCountries(Comparator.comparing(Country::getCountryName));
  }

  public Map<String, Country> getCountriesMap() {
    return Collections.unmodifiableMap(cache);
  }

  public void loadCountries() {

    Logger.info("Attempting to refresh the country cache....");
    countryServiceClient.getCountries()
      .thenAcceptAsync(countries -> {
        if (!countries.isEmpty()) {
          cache = countries.stream()
            .collect(toMap(Country::getCountryRef, Function.identity()));
          Logger.info("Successfully refreshed the country cache.");
        } else {
          Logger.error("Failed to refresh country cache - Country Service Client getCountries error occurred.");
        }
      });
  }

  @VisibleForTesting
  void setCache(Map<String, Country> cache) {
    this.cache = cache;
  }
}
