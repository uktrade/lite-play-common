package components.common.cache;

import com.google.common.collect.ImmutableMap;
import models.common.Country;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CountryProviderTest {

  private static final String COUNTRY_REF = "CTRY0";
  private static final String COUNTRY_NAME = "United Kingdom";

  @InjectMocks
  private CountryProvider countryProvider;

  @Before
  public void setUp() throws Exception {
    countryProvider.setCache(ImmutableMap.of(COUNTRY_REF, getCountry()));
  }

  @Test
  public void shouldGetCountryName() throws Exception {

    Country country = countryProvider.getCountry(COUNTRY_REF);

    assertThat(country).isNotNull();
    assertThat(country.getCountryName()).isEqualTo(COUNTRY_NAME);
    assertThat(country.getCountryRef()).isEqualTo(COUNTRY_REF);
  }

  @Test
  public void shouldGetCountriesMap() throws Exception {

    Map<String, Country> countriesMap = countryProvider.getCountriesMap();

    assertThat(countriesMap).isNotEmpty();
  }

  @Test
  public void shouldGetCountries() throws Exception {

    Collection<Country> countries = countryProvider.getCountries();

    assertThat(countries).isNotEmpty();
  }

  private Country getCountry() {
    return new Country() {
      @Override
      public String getCountryRef() {
        return COUNTRY_REF;
      }

      @Override
      public String getCountryName() {
        return COUNTRY_NAME;
      }
    };
  }
}
