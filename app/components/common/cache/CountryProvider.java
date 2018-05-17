package components.common.cache;

import static java.util.stream.Collectors.toMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import components.common.client.CountryServiceClient;
import play.Logger;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CountryProvider {

  private volatile Map<String, CountryView> cache = new HashMap<>();
  private final CountryServiceClient countryServiceClient;

  @Inject
  public CountryProvider(CountryServiceClient countryServiceClient) {
    this.countryServiceClient = countryServiceClient;
  }

  public CountryView getCountry(String countryRef) {
    if (countryRef == null) {
      return null;
    } else {
      return cache.get(countryRef);
    }
  }

  public Collection<CountryView> getCountries() {
    return Collections.unmodifiableCollection(cache.values());
  }

  public Map<String, CountryView> getCountriesMap() {
    return Collections.unmodifiableMap(cache);
  }

  public void loadCountries() {

    Logger.info("Attempting to refresh the country cache....");
    countryServiceClient.getCountries()
        .thenAcceptAsync(countries -> {
          if (!countries.isEmpty()) {
            cache = countries.stream()
                .collect(toMap(CountryView::getCountryRef, Function.identity()));
            Logger.info("Successfully refreshed the country cache.");
          } else {
            Logger.error("Failed to refresh country cache - Country Service Client getCountries error occurred.");
          }
        });
  }

  @VisibleForTesting
  void setCache(Map<String, CountryView> cache) {
    this.cache = cache;
  }
}
