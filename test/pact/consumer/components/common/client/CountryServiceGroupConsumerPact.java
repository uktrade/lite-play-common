package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
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


public class CountryServiceGroupConsumerPact {
  public static final String PROVIDER = "lite-play-common";
  public static final String CONSUMER = "lite-play-common";

  public static final String COUNTRY_REF = "CRTY0";
  public static final String COUNTRY_NAME = "United Kingdom";

  public static final String COUNTRY_GROUP_BASE_PATH = "/countries/group/";
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

  public static String getCountryGroupExistsPath() {
    return COUNTRY_GROUP_BASE_PATH + EXISTS;
  }

  public static String getCountryGroupDoesNotExistPath() {
    return COUNTRY_GROUP_BASE_PATH + DOES_NOT_EXIST;
  }

  public static CountryServiceClient buildCountryGroupExistsClient(PactProviderRule mockProvider, WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceGroupClient(new HttpExecutionContext(Runnable::run), wsClient,1000,
        mockProvider.getConfig().url(), EXISTS, Json.newDefaultMapper());
  }

  public static CountryServiceClient buildCountryGroupDoesNotExistClient(PactProviderRule mockProvider, WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceGroupClient(new HttpExecutionContext(Runnable::run), wsClient,1000,
        mockProvider.getConfig().url(), DOES_NOT_EXIST, Json.newDefaultMapper());
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static PactFragment countryGroupExists(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(0,3)
      .stringType("countryRef", COUNTRY_REF)
      .stringType("countryName", COUNTRY_NAME)
      .closeObject()
      .asArray();
    return builder
        .given("provided country group exists")
        .uponReceiving("a request for provided country group")
          .path(getCountryGroupExistsPath())
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(jsonHeader())
          .body(body)
        .toFragment();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static PactFragment countryGroupDoesNotExist(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .integerType("code", 404)
        .stringType("message", "Country group does not exist - " + DOES_NOT_EXIST)
        .asBody();
    return builder
        .given("provided country group does not exist")
        .uponReceiving("a request for provided country group")
          .path(getCountryGroupDoesNotExistPath())
          .method("GET")
        .willRespondWith()
          .status(404)
          .headers(jsonHeader())
          .body(body)
        .toFragment();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countryGroupExists")
  public void countryGroupExistsTest() {
    doCountryGroupExistsTest(buildCountryGroupExistsClient(mockProvider, ws));
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countryGroupDoesNotExist")
  public void countryGroupDoesNotExistTest() {
    doCountryGroupDoesNotExistTest(buildCountryGroupDoesNotExistClient(mockProvider, ws));
  }

  @SuppressWarnings("Duplicates")
  public static void doCountryGroupExistsTest(CountryServiceClient client) {
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
  public static void doCountryGroupDoesNotExistTest(CountryServiceClient client) {
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
