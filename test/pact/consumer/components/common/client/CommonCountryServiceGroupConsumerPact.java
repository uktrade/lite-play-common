package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static pact.consumer.components.common.client.TestHelper.CONSUMER;
import static pact.consumer.components.common.client.TestHelper.PROVIDER;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
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

public class CommonCountryServiceGroupConsumerPact {

  private static final String COUNTRY_GROUP_BASE_PATH = "/countries/group/";
  private static final String EXISTS = "EXISTS";
  private static final String DOES_NOT_EXIST = "DOES_NOT_EXIST";
  private static final String COUNTRY_REF = "CRTY0";
  private static final String COUNTRY_NAME = "United Kingdom";

  @Rule
  public final PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2(PROVIDER, this);

  private WSClient wsClient;

  @Before
  public void setUp() throws Exception {
    wsClient = WSTestClient.newClient(mockProvider.getPort());
  }

  @After
  public void tearDown() throws Exception {
    wsClient.close();
  }

  public static CountryServiceClient buildCountryServiceGroupExistsClient(PactProviderRuleMk2 mockProvider,
                                                                          WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceGroupClient(
        mockProvider.getUrl(),
        1000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run),
        EXISTS);
  }

  public static CountryServiceClient buildCountryServiceGroupDoesNotExistClient(PactProviderRuleMk2 mockProvider,
                                                                                WSClient wsClient) {
    return CountryServiceClient.buildCountryServiceGroupClient(mockProvider.getUrl(),
        1000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run),
        DOES_NOT_EXIST);
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact countryGroupExists(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(0, 3)
        .stringType("countryRef", COUNTRY_REF)
        .stringType("countryName", COUNTRY_NAME)
        .closeObject()
        .asArray();
    return builder
        .given("provided country group exists")
        .uponReceiving("a request for provided country group")
        .headers(TestHelper.BASIC_AUTH_HEADERS)
        .path(COUNTRY_GROUP_BASE_PATH + EXISTS)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(TestHelper.CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact countryGroupDoesNotExist(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .integerType("code", 404)
        .stringType("message", "Country group does not exist - " + DOES_NOT_EXIST)
        .asBody();
    return builder
        .given("provided country group does not exist")
        .uponReceiving("a request for provided country group")
        .headers(TestHelper.BASIC_AUTH_HEADERS)
        .path(COUNTRY_GROUP_BASE_PATH + DOES_NOT_EXIST)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(TestHelper.CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countryGroupExists")
  public void countryGroupExistsPact() throws Exception {
    countryGroupExists(buildCountryServiceGroupExistsClient(mockProvider, wsClient));
  }

  @SuppressWarnings("Duplicates")
  public static void countryGroupExists(CountryServiceClient client) throws Exception {
    List<CountryView> countries = client.getCountries().toCompletableFuture().get();
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isFalse();
    CountryView countryView = countries.get(0);
    assertThat(countryView.getCountryRef()).isEqualTo(COUNTRY_REF);
    assertThat(countryView.getCountryName()).isEqualTo(COUNTRY_NAME);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "countryGroupDoesNotExist")
  public void countryGroupDoesNotExistPact() throws Exception {
    countryGroupDoesNotExist(buildCountryServiceGroupDoesNotExistClient(mockProvider, wsClient));
  }

  @SuppressWarnings("Duplicates")
  public static void countryGroupDoesNotExist(CountryServiceClient client) throws Exception {
    List<CountryView> countries = client.getCountries().toCompletableFuture().get();
    assertThat(countries).isNotNull();
    assertThat(countries.isEmpty()).isTrue();
  }

}
