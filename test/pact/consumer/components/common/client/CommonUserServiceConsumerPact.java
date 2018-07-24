package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static pact.consumer.components.common.client.TestHelper.BASIC_AUTH_HEADERS;
import static pact.consumer.components.common.client.TestHelper.CONSUMER;
import static pact.consumer.components.common.client.TestHelper.CONTENT_TYPE_HEADERS;
import static pact.consumer.components.common.client.TestHelper.JWT_AUTH_HEADERS;
import static pact.consumer.components.common.client.TestHelper.PROVIDER;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import components.common.client.ClientException;
import components.common.client.UserServiceClientBasicAuth;
import components.common.client.UserServiceClientJwt;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.test.WSTestClient;
import uk.gov.bis.lite.user.api.view.CustomerView;
import uk.gov.bis.lite.user.api.view.SiteView;
import uk.gov.bis.lite.user.api.view.UserAccountTypeView;
import uk.gov.bis.lite.user.api.view.UserDetailsView;
import uk.gov.bis.lite.user.api.view.UserPrivilegesView;
import uk.gov.bis.lite.user.api.view.enums.AccountStatus;
import uk.gov.bis.lite.user.api.view.enums.AccountType;
import uk.gov.bis.lite.user.api.view.enums.Role;

public class CommonUserServiceConsumerPact {

  private static final String USER_ACCOUNT_TYPE_PATH = "/user-account-type/";
  private static final String USER_PRIVILEGES_PATH = "/user-privileges/";
  private static final String USER_DETAILS_PATH = "/user-details/";
  private static final String USER_ID = "123456";
  private static final String CUSTOMER_ID = "CUSTOMER_289";
  private static final String SITE_ID = "SITE_887";
  private static final Role ROLE = Role.SUBMITTER;
  private static final String TITLE = "title";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String FULL_NAME = "fullName";
  private static final String CONTACT_EMAIL_ADDRESS = "test@test.com";
  private static final String CONTACT_PHONE_NUMBER = "012345678";
  private static final AccountStatus ACCOUNT_STATUS = AccountStatus.ACTIVE;

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

  public static UserServiceClientBasicAuth buildBasicAuthClient(WSClient wsClient, PactProviderRuleMk2 mockProvider) {
    return new UserServiceClientBasicAuth(mockProvider.getUrl(),
        10000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run));
  }

  public static UserServiceClientJwt buildJwtClient(WSClient wsClient, PactProviderRuleMk2 mockProvider) {
    return new UserServiceClientJwt(mockProvider.getUrl(),
        10000,
        new JwtTestRequestFilter(),
        wsClient,
        new HttpExecutionContext(Runnable::run));
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userAccountTypeUserExists(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .stringType("accountType", "EXPORTER")
        .asBody();

    return builder
        .given("provided user exists")
        .uponReceiving("a request for user account type")
        .headers(BASIC_AUTH_HEADERS)
        .path(USER_ACCOUNT_TYPE_PATH + USER_ID)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userAccountTypeUserDoesNotExist(PactDslWithProvider builder) {
    return builder
        .given("provided user does not exist")
        .uponReceiving("a request for user account type")
        .headers(BASIC_AUTH_HEADERS)
        .path(USER_ACCOUNT_TYPE_PATH + USER_ID)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(CONTENT_TYPE_HEADERS)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userPrivilegeViewUserExists(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .array("customers")
        .object()
        .stringType("customerId", CUSTOMER_ID)
        .stringType("role", ROLE.toString())
        .closeObject()
        .closeArray()
        .array("sites")
        .object()
        .stringType("siteId", SITE_ID)
        .stringType("role", ROLE.toString())
        .closeObject()
        .closeArray()
        .asBody();

    return builder
        .given("provided user exists")
        .uponReceiving("a request for user privilege view")
        .headers(JWT_AUTH_HEADERS)
        .path(USER_PRIVILEGES_PATH + USER_ID)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userPrivilegeViewUserDoesNotExist(PactDslWithProvider builder) {
    return builder
        .given("provided user does not exist")
        .uponReceiving("a request for user privilege view")
        .headers(JWT_AUTH_HEADERS)
        .path(USER_PRIVILEGES_PATH + USER_ID)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(CONTENT_TYPE_HEADERS)
        .toPact();
  }


  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userDetailsViewUserExists(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .stringType("title", TITLE)
        .stringType("firstName", FIRST_NAME)
        .stringType("lastName", LAST_NAME)
        .stringType("fullName", FULL_NAME)
        .stringType("contactEmailAddress", CONTACT_EMAIL_ADDRESS)
        .stringType("contactPhoneNumber", CONTACT_PHONE_NUMBER)
        .stringType("accountStatus", ACCOUNT_STATUS.toString())
        .asBody();

    return builder
        .given("provided user exists")
        .uponReceiving("a request for user privilege view")
        .headers(JWT_AUTH_HEADERS)
        .path(USER_DETAILS_PATH + USER_ID)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userDetailsViewUserDoesNotExist(PactDslWithProvider builder) {
    return builder
        .given("provided user does not exist")
        .uponReceiving("a request for user details view")
        .headers(JWT_AUTH_HEADERS)
        .path(USER_DETAILS_PATH + USER_ID)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(CONTENT_TYPE_HEADERS)
        .toPact();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userAccountTypeUserExists")
  public void userAccountTypeUserExistsPact() throws Exception {
    userAccountTypeUserExists(buildBasicAuthClient(wsClient, mockProvider));
  }

  public static void userAccountTypeUserExists(UserServiceClientBasicAuth client) throws Exception {
    UserAccountTypeView userAccountTypeView = client.getUserAccountTypeView(USER_ID).toCompletableFuture().get();
    assertThat(userAccountTypeView.getAccountType()).isEqualTo(AccountType.EXPORTER);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userAccountTypeUserDoesNotExist")
  public void userAccountTypeUserDoesNotExistPact() {
    userAccountTypeUserDoesNotExist(buildBasicAuthClient(wsClient, mockProvider));
  }

  public static void userAccountTypeUserDoesNotExist(UserServiceClientBasicAuth client) {
    assertThatThrownBy(() -> client.getUserAccountTypeView(USER_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. user-service getUserAccountTypeView failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userPrivilegeViewUserExists")
  public void userPrivilegeViewUserExistsPact() throws Exception {
    userPrivilegeViewUserExists(buildJwtClient(wsClient, mockProvider));
  }

  public static void userPrivilegeViewUserExists(UserServiceClientJwt client) throws Exception {
    UserPrivilegesView userPrivilegesView = client.getUserPrivilegeView(USER_ID).toCompletableFuture().get();
    assertThat(userPrivilegesView.getCustomers()).hasSize(1);
    CustomerView customerView = userPrivilegesView.getCustomers().get(0);
    assertThat(customerView.getCustomerId()).isEqualTo(CUSTOMER_ID);
    assertThat(customerView.getRole()).isEqualTo(ROLE);
    assertThat(userPrivilegesView.getSites()).hasSize(1);
    SiteView siteView = userPrivilegesView.getSites().get(0);
    assertThat(siteView.getSiteId()).isEqualTo(SITE_ID);
    assertThat(siteView.getRole()).isEqualTo(ROLE);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userPrivilegeViewUserDoesNotExist")
  public void userPrivilegeViewUserDoesNotExistPact() throws Exception {
    userPrivilegeViewUserDoesNotExist(buildJwtClient(wsClient, mockProvider));
  }

  public static void userPrivilegeViewUserDoesNotExist(UserServiceClientJwt client) throws Exception {
    assertThatThrownBy(() -> client.getUserPrivilegeView(USER_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. user-service getUserPrivilegeView failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userDetailsViewUserExists")
  public void userDetailsViewUserExistsPact() throws Exception {
    userDetailsViewUserExists(buildJwtClient(wsClient, mockProvider));
  }

  public static void userDetailsViewUserExists(UserServiceClientJwt client) throws Exception {
    UserDetailsView userDetailsView = client.getUserDetailsView(USER_ID).toCompletableFuture().get();

    assertThat(userDetailsView.getTitle()).isEqualTo(TITLE);
    assertThat(userDetailsView.getFirstName()).isEqualTo(FIRST_NAME);
    assertThat(userDetailsView.getLastName()).isEqualTo(LAST_NAME);
    assertThat(userDetailsView.getFullName()).isEqualTo(FULL_NAME);
    assertThat(userDetailsView.getContactEmailAddress()).isEqualTo(CONTACT_EMAIL_ADDRESS);
    assertThat(userDetailsView.getContactPhoneNumber()).isEqualTo(CONTACT_PHONE_NUMBER);
    assertThat(userDetailsView.getAccountStatus()).isEqualTo(ACCOUNT_STATUS);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userDetailsViewUserDoesNotExist")
  public void userDetailsViewUserDoesNotExistPact() throws Exception {
    userDetailsViewUserDoesNotExist(buildJwtClient(wsClient, mockProvider));
  }

  public static void userDetailsViewUserDoesNotExist(UserServiceClientJwt client) throws Exception {
    assertThatThrownBy(() -> client.getUserDetailsView(USER_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. user-service getUserDetailsView failure.");
  }

}
