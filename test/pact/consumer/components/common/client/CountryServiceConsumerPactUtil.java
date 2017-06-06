package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import models.common.Country;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryServiceConsumerPactUtil {
  public static final String PROVIDER = "lite-country-service";
  public static final String COUNTRY_REF = "CRTY0";
  public static final String COUNTRY_NAME = "United Kingdom";

  public static PactFragment buildCountriesExistFragment(PactDslWithProvider builder, String path, String given, String uponReceiving) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(0,3)
        .stringType("countryRef", "CRTY0")
        .stringType("countryName", "United Kingdom")
        .closeObject()
        .asArray();

    return builder
        .given(given)
        .uponReceiving(uponReceiving)
          .path(path)
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(CountryServiceConsumerPactUtil.jsonHeader())
          .body(body)
        .toFragment();
  }

   public static PactFragment buildCountriesDoNotExistFragment(PactDslWithProvider builder, String path, String given, String uponReceiving) {
    PactDslJsonArray body = new PactDslJsonArray();
    return builder
        .given(given)
        .uponReceiving(uponReceiving)
          .path(path)
          .method("GET")
          .willRespondWith()
            .status(200)
            .headers(jsonHeader())
            .body(body)
        .toFragment();
  }

  public static void countriesExistAssertions(List<Country> countries) {
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isFalse();
    Country country = countries.get(0);
    assertThat(country.getCountryRef()).isEqualTo(COUNTRY_REF);
    assertThat(country.getCountryName()).isEqualTo(COUNTRY_NAME);
  }

  public static void countriesDoNotExistAssertions(List<Country> countries) {
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isTrue();
  }

  private static Map<String, String> jsonHeader() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return headers;
  }
}
