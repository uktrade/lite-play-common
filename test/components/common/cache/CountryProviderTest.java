package components.common.cache;

import components.common.client.CountryServiceClient;
import models.common.Country;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.cache.CacheApi;
import play.libs.concurrent.HttpExecutionContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountryProviderTest {

  private static final String COUNTRY_REF = "CTRY0";
  private static final String COUNTRY_NAME = "United Kingdom";

  @Mock
  private CacheApi cacheApi;
  @Mock
  private CountryServiceClient countryServiceClient;
  @Mock
  private HttpExecutionContext httpExecutionContext;

  @InjectMocks
  private CountryProvider countryProvider;

  @Mock
  private Country country;

  @Before
  public void setUp() throws Exception {
    when(countryServiceClient.getCountries()).thenReturn(completedFuture(getCountryList()));
    countryProvider = new CountryProvider(countryServiceClient);
  }

  @Test
  public void shouldGetCountryName() throws Exception {

    Country country = countryProvider.getCountry(COUNTRY_REF);

    assertThat(country).isNotNull();
    assertThat(country.getCountryName()).isEqualTo(COUNTRY_NAME);
    assertThat(country.getCountryRef()).isEqualTo(COUNTRY_NAME);
  }

  @Test
  public void shouldGetCountriesMap() throws Exception {

    Map<String, Country> countriesMap = countryProvider.getCountriesMap();

    assertThat(countriesMap).isNotEmpty();
  }

  @Test
  public void shouldGetCountries() throws Exception {

    Collection<Country> countries = countryProvider.getCountries();

    assertThat(countries).isEmpty();
  }

  private List<Country> getCountryList() {
    Country country = new Country() {
      @Override
      public String getCountryRef() {
        return COUNTRY_REF;
      }

      @Override
      public String getCountryName() {
        return COUNTRY_NAME;
      }
    };

    return Arrays.asList(country);
  }
}
