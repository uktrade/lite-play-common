package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static pact.consumer.components.common.client.TestHelper.BASIC_AUTH_HEADERS;
import static pact.consumer.components.common.client.TestHelper.CONSUMER;
import static pact.consumer.components.common.client.TestHelper.CONTENT_TYPE_HEADERS;
import static pact.consumer.components.common.client.TestHelper.PROVIDER;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
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

public class CommonCountryServiceDataConsumerPact {

  private static final String COUNTRY_GROUP_BASE_PATH = "/country-data";
  private static final String COUNTRY_REF = "CRTY0";
  private static final String COUNTRY_NAME = "United Kingdom";

  @Rule
  public PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2(PROVIDER, this);

  private WSClient wsClient;
  private CountryServiceClient client;

  @Before
  public void setup() {
    wsClient = WSTestClient.newClient(mockProvider.getPort());
    client = buildCountryServiceAllClient(wsClient, mockProvider);
  }

  @After
  public void teardown() throws Exception {
    wsClient.close();
  }

  public static CountryServiceClient buildCountryServiceAllClient(WSClient wsClient, PactProviderRuleMk2 mockProvider) {
    return CountryServiceClient.buildCountryServiceAllClient(mockProvider.getUrl(),
        1000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run));
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
        .headers(BASIC_AUTH_HEADERS)
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
  public void countryDataExistsPact() throws Exception {
    countryDataExists(client);
  }

  public static void countryDataExists(CountryServiceClient client) throws Exception {
    List<CountryView> countries = client.getCountries().toCompletableFuture().get();
    assertThat(countries).isNotEmpty();
    CountryView countryView = countries.get(0);
    assertThat(countryView.getCountryRef()).isEqualTo(COUNTRY_REF);
    assertThat(countryView.getCountryName()).isEqualTo(COUNTRY_NAME);
  }

}
