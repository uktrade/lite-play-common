package components.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CountryProviderTest {

  private static final String COUNTRY_REF = "CTRY0";
  private static final String COUNTRY_NAME = "United Kingdom";

  @InjectMocks
  private CountryProvider countryProvider;

  @Before
  public void setUp() {
    CountryView countryView = new CountryView(COUNTRY_REF, COUNTRY_NAME, new ArrayList<>());
    countryProvider.setCache(ImmutableMap.of(COUNTRY_REF, countryView));
  }

  @Test
  public void shouldGetCountryName() {

    CountryView countryView = countryProvider.getCountry(COUNTRY_REF);

    assertThat(countryView).isNotNull();
    assertThat(countryView.getCountryName()).isEqualTo(COUNTRY_NAME);
    assertThat(countryView.getCountryRef()).isEqualTo(COUNTRY_REF);
  }

  @Test
  public void shouldGetCountriesMap() {

    Map<String, CountryView> countriesMap = countryProvider.getCountriesMap();

    assertThat(countriesMap).isNotEmpty();
  }

  @Test
  public void shouldGetCountries() {

    Collection<CountryView> countries = countryProvider.getCountries();

    assertThat(countries).isNotEmpty();
  }

}
