package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.ImmutableMap;
import components.common.client.CountryServiceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.test.WSTestClient;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.util.List;
import java.util.Map;

public class CountryServiceDataConsumerPact {

  // service:password
  private static final Map<String, String> AUTH_HEADERS = ImmutableMap.of("Authorization", "Basic c2VydmljZTpwYXNzd29yZA==");
  private static final Map<String, String> CONTENT_TYPE_HEADERS = ImmutableMap.of("Content-Type", "application/json");

  private static final String COUNTRY_GROUP_BASE_PATH = "/country-data";
  private static final String PROVIDER = "lite-play-common";
  private static final String CONSUMER = "lite-play-common";
  private static final String COUNTRY_REF = "CRTY0";
  private static final String COUNTRY_NAME = "United Kingdom";

  private WSClient ws;

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

  public static CountryServiceClient buildCountryServiceAllClient(PactProviderRuleMk2 mockProvider, WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceAllClient(new HttpExecutionContext(Runnable::run),
        wsClient,
        1000,
        mockProvider.getUrl(),
        "service:password",
        Json.newDefaultMapper());
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact countryDataExists(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(0, 3)
        .stringType("countryRef", COUNTRY_REF)
        .stringType("countryName", COUNTRY_NAME)
        .closeObject()
        .asArray();
    return builder
        .given("country data exists")
        .uponReceiving("a request for country data")
        .headers(AUTH_HEADERS)
        .path(COUNTRY_GROUP_BASE_PATH)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countryDataExists")
  public void countryDataExistsTest() {
    doCountryDataExistsTest(buildCountryServiceAllClient(mockProvider, ws));
  }

  public static void doCountryDataExistsTest(CountryServiceClient client) {
    List<CountryView> countries;
    try {
      countries = client.getCountries().toCompletableFuture().get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(countries).isNotEmpty();
    CountryView countryView = countries.get(0);
    assertThat(countryView.getCountryRef()).isEqualTo(COUNTRY_REF);
    assertThat(countryView.getCountryName()).isEqualTo(COUNTRY_NAME);
  }

}
