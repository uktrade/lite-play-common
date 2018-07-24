package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pact.consumer.components.common.client.JwtTestHelper.JWT_AUTHORIZATION_HEADER;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.ImmutableMap;
import components.common.client.ClientException;
import components.common.client.CustomerServiceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.test.WSTestClient;
import uk.gov.bis.lite.customer.api.view.CustomerView;
import uk.gov.bis.lite.customer.api.view.SiteView;

import java.util.Map;

public class CommonCustomerServiceConsumerPact {

  private static final String PROVIDER = "lite-play-common";
  private static final String CONSUMER = "lite-play-common";
  private static final String CUSTOMER_ID = "CUSTOMER_289";
  private static final String SITE_ID = "SITE_887";
  private static final Map<String, String> RESPONSE_HEADERS = ImmutableMap.of("Content-Type", "application/json");

  @Rule
  public PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2(PROVIDER, this);

  private WSClient wsClient;
  private CustomerServiceClient client;

  @Before
  public void setup() {
    wsClient = WSTestClient.newClient(mockProvider.getPort());
    client = buildClient(wsClient, mockProvider);
  }

  @After
  public void teardown() throws Exception {
    wsClient.close();
  }

  public static CustomerServiceClient buildClient(WSClient wsClient, PactProviderRuleMk2 mockProvider) {
    return new CustomerServiceClient(mockProvider.getUrl(),
        1000,
        wsClient,
        new HttpExecutionContext(Runnable::run),
        new JwtTestRequestFilter());
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact existingCustomer(PactDslWithProvider builder) {
    PactDslJsonBody customer = new PactDslJsonBody()
        .stringType("customerId", CUSTOMER_ID)
        .stringType("companyName", "Acme Ltd")
        .stringType("companyNumber", "876543")
        .stringType("shortName", "ACL")
        .stringType("organisationType")
        .stringType("registrationStatus")
        .stringType("applicantType")
        .minArrayLike("websites", 0, PactDslJsonRootValue.stringType());

    return builder
        .given("provided customer ID exists")
        .uponReceiving("request for a customer by ID")
        .path("/customers/" + CUSTOMER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(customer)
        .toPact();
  }


  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact missingCustomer(PactDslWithProvider builder) {
    return builder
        .given("provided customer ID does not exist")
        .uponReceiving("request for a customer by ID")
        .path("/customers/" + CUSTOMER_ID)
        .headers(JWT_AUTHORIZATION_HEADER)
        .method("GET")
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact existingSite(PactDslWithProvider builder) {
    DslPart site = new PactDslJsonBody()
        .stringType("customerId", "CUSTOMER_626")
        .stringType("siteId", "SITE_887")
        .stringType("siteName", "Main Site")
        .object("address")
        .stringType("plainText", "1 Roadish Avenue, Townston")
        .stringType("country", "UK")
        .closeObject();

    return builder
        .given("provided site ID exists")
        .uponReceiving("request for site by ID")
        .path("/sites/" + SITE_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(site)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact missingSite(PactDslWithProvider builder) {
    return builder
        .given("provided site ID does not exist")
        .uponReceiving("request for site by ID")
        .path("/sites/" + SITE_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "existingCustomer")
  public void existingCustomerPact() throws Exception {
    existingCustomer(client);
  }

  public static void existingCustomer(CustomerServiceClient client) throws Exception {
    CustomerView customer = client.getCustomer(CUSTOMER_ID).toCompletableFuture().get();
    assertThat(customer.getCompanyName()).isEqualTo("Acme Ltd");
    assertThat(customer.getCompanyNumber()).isEqualTo("876543");
    assertThat(customer.getShortName()).isEqualTo("ACL");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "missingCustomer")
  public void missingCustomerPact() throws Exception {
    missingCustomer(client);
  }

  public static void missingCustomer(CustomerServiceClient client) {
    assertThatThrownBy(() -> client.getCustomer(CUSTOMER_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. customer-service getCustomer failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "existingSite")
  public void existingSitePact() throws Exception {
    existingSite(client);
  }

  public static void existingSite(CustomerServiceClient client) throws Exception {
    SiteView site = client.getSite(SITE_ID).toCompletableFuture().get();
    assertThat(site.getCustomerId()).isEqualTo("CUSTOMER_626");
    assertThat(site.getSiteId()).isEqualTo("SITE_887");
    assertThat(site.getSiteName()).isEqualTo("Main Site");
    assertThat(site.getAddress().getPlainText()).isEqualTo("1 Roadish Avenue, Townston");
    assertThat(site.getAddress().getCountry()).isEqualTo("UK");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "missingSite")
  public void missingSitePact() throws Exception {
    missingSite(client);
  }

  public static void missingSite(CustomerServiceClient client) throws Exception {
    assertThatThrownBy(() -> client.getSite(SITE_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageContaining("Unexpected status code 404. Body is empty. customer-service getSite failure.");
  }

}
