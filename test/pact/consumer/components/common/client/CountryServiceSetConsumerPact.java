package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.ImmutableMap;
import components.common.client.CountryServiceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.test.WSTestClient;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class CountryServiceSetConsumerPact {

  // service:password
  private static final Map<String, String> AUTH_HEADERS = ImmutableMap.of("Authorization", "Basic c2VydmljZTpwYXNzd29yZA==");
  private static final Map<String, String> CONTENT_TYPE_HEADERS = ImmutableMap.of("Content-Type", "application/json");

  public static final String PROVIDER = "lite-play-common";
  public static final String CONSUMER = "lite-play-common";

  public static final String COUNTRY_REF = "CRTY0";
  public static final String COUNTRY_NAME = "United Kingdom";

  public static final String COUNTRY_SET_BASE_PATH = "/countries/set/";
  public static final String EXISTS = "EXISTS";
  public static final String DOES_NOT_EXIST = "DOES_NOT_EXIST";

  public WSClient ws;

  @Rule
  public final PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2(PROVIDER, this);

  @Before
  public void setUp() throws Exception {
    ws = WSTestClient.newClient(mockProvider.getPort());
  }

  @After
  public void tearDown() throws Exception {
    ws.close();
  }

  public static String getCountrySetExistsPath() {
    return COUNTRY_SET_BASE_PATH + EXISTS;
  }

  public static String getCountrySetDoesNotExistPath() {
    return COUNTRY_SET_BASE_PATH + DOES_NOT_EXIST;
  }

  public static CountryServiceClient buildCountrySetExistsClient(PactProviderRuleMk2 mockProvider, WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceSetClient(mockProvider.getUrl(),
        1000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run),
        EXISTS);
  }

  public static CountryServiceClient buildCountrySetDoesNotExistClient(PactProviderRuleMk2 mockProvider,
                                                                       WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceSetClient(mockProvider.getUrl(),
        1000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run),
        DOES_NOT_EXIST);
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact countrySetExists(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(0, 3)
        .stringType("countryRef", COUNTRY_REF)
        .stringType("countryName", COUNTRY_NAME)
        .closeObject()
        .asArray();
    return builder
        .given("provided country set exists")
        .uponReceiving("a request for provided country set")
        .headers(AUTH_HEADERS)
        .path(getCountrySetExistsPath())
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact countrySetDoesNotExist(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .integerType("code", 404)
        .stringType("message", "Country set does not exist - " + DOES_NOT_EXIST)
        .asBody();
    return builder
        .given("provided country set does not exist")
        .uponReceiving("a request for provided country set")
        .headers(AUTH_HEADERS)
        .path(getCountrySetDoesNotExistPath())
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
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
    List<CountryView> countries;
    try {
      countries = client.getCountries().toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isFalse();
    CountryView countryView = countries.get(0);
    assertThat(countryView.getCountryRef()).isEqualTo(COUNTRY_REF);
    assertThat(countryView.getCountryName()).isEqualTo(COUNTRY_NAME);
  }

  @SuppressWarnings("Duplicates")
  public static void doCountrySetDoesNotExistTest(CountryServiceClient client) {
    List<CountryView> countries;
    try {
      countries = client.getCountries().toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isTrue();
  }

}