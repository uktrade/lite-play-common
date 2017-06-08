package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import components.common.client.CountryServiceClient;
import models.common.Country;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WS;
import play.libs.ws.WSClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class CountryServiceSetConsumerPact {
  public static final String PROVIDER = "lite-play-common";
  public static final String CONSUMER = "lite-play-common";

  public static final String COUNTRY_REF = "CRTY0";
  public static final String COUNTRY_NAME = "United Kingdom";

  public static final String COUNTRY_SET_BASE_PATH = "/countries/set/";
  public static final String EXISTS = "EXISTS";
  public static final String DOES_NOT_EXIST = "DOES_NOT_EXIST";

  public WSClient ws;

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(PROVIDER, this);

  @Before
  public void setUp() throws Exception {
    ws = WS.newClient(mockProvider.getConfig().getPort());
  }

  @After
  public void tearDown() throws Exception {
    ws.close();
  }

  private static Map<String, String> jsonHeader() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return headers;
  }

  public static String getCountrySetExistsPath() {
    return COUNTRY_SET_BASE_PATH + EXISTS;
  }

  public static String getCountrySetDoesNotExistPath() {
    return COUNTRY_SET_BASE_PATH + DOES_NOT_EXIST;
  }

  public static CountryServiceClient buildCountrySetExistsClient(PactProviderRule mockProvider, WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceSetClient(new HttpExecutionContext(Runnable::run), wsClient,1000,
        mockProvider.getConfig().url(), EXISTS, Json.newDefaultMapper());
  }

  public static CountryServiceClient buildCountrySetDoesNotExistClient(PactProviderRule mockProvider, WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceSetClient(new HttpExecutionContext(Runnable::run), wsClient,1000,
        mockProvider.getConfig().url(), DOES_NOT_EXIST, Json.newDefaultMapper());
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static PactFragment countrySetExists(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(0,3)
      .stringType("countryRef", COUNTRY_REF)
      .stringType("countryName", COUNTRY_NAME)
      .closeObject()
      .asArray();
    return builder
        .given("provided country set exists")
        .uponReceiving("a request for provided country set")
          .path(getCountrySetExistsPath())
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(jsonHeader())
          .body(body)
        .toFragment();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static PactFragment countrySetDoesNotExist(PactDslWithProvider builder) {
    return builder
        .given("provided country set does not exist")
        .uponReceiving("a request for provided country set")
          .path(getCountrySetDoesNotExistPath())
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(jsonHeader())
          .body(new PactDslJsonArray())
        .toFragment();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countrySetExists")
  public void countrySetExistsTest() {
    doCountrySetExistsTest(buildCountrySetExistsClient(mockProvider, ws));
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countrySetDoesNotExist")
  public void countrySetDoesNotExistTest() {
    doCountrySetDoesNotExistTest(buildCountrySetDoesNotExistClient(mockProvider, ws));
  }

  @SuppressWarnings("Duplicates")
  public static void doCountrySetExistsTest(CountryServiceClient client) {
    List<Country> countries;
    try {
      countries = client.getCountries().toCompletableFuture().get();
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isFalse();
    Country country = countries.get(0);
    assertThat(country.getCountryRef()).isEqualTo(COUNTRY_REF);
    assertThat(country.getCountryName()).isEqualTo(COUNTRY_NAME);
  }

  @SuppressWarnings("Duplicates")
  public static void doCountrySetDoesNotExistTest(CountryServiceClient client) {
    List<Country> countries;
    try {
      countries = client.getCountries().toCompletableFuture().get();
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isTrue();
  }

}