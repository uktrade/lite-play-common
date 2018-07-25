package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pact.consumer.components.common.client.JwtTestHelper.JWT_AUTHORIZATION_HEADER;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.ImmutableMap;
import components.common.client.ClientException;
import components.common.client.PermissionsServiceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.test.WSTestClient;
import uk.gov.bis.lite.permissions.api.param.RegisterAddressParam;
import uk.gov.bis.lite.permissions.api.param.RegisterParam;
import uk.gov.bis.lite.permissions.api.view.LicenceView;
import uk.gov.bis.lite.permissions.api.view.OgelRegistrationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonPermissionsServiceConsumerPact {

  private static final String PROVIDER = "lite-play-common";
  private static final String CONSUMER = "lite-play-common";
  private static final String LICENCES_URL = "/licences/user/";
  private static final String REGISTRATIONS_URL = "/ogel-registrations/user/";
  private static final String USER_ID = "123456";
  private static final String LICENCE_REFERENCE = "LIC/123";
  private static final String REGISTRATION_REFERENCE = "REG/123";
  private static final String CALLBACK_URL = "callbackUrl";
  private static final Map<String, String> REQUEST_HEADERS;
  private static final Map<String, String> RESPONSE_HEADERS = ImmutableMap.of("Content-Type", "application/json");

  static {
    REQUEST_HEADERS = new HashMap<>();
    REQUEST_HEADERS.put("Content-Type", "application/json");
    REQUEST_HEADERS.putAll(JWT_AUTHORIZATION_HEADER);
  }

  @Rule
  public final PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2(PROVIDER, this);

  private WSClient wsClient;
  private PermissionsServiceClient client;

  @Before
  public void setup() {
    wsClient = WSTestClient.newClient(mockProvider.getPort());
    client = buildClient(wsClient, mockProvider);
  }

  @After
  public void teardown() throws Exception {
    wsClient.close();
  }

  public static PermissionsServiceClient buildClient(WSClient wsClient, PactProviderRuleMk2 mockProvider) {
    return new PermissionsServiceClient(mockProvider.getUrl(),
        1000, "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run),
        new JwtTestRequestFilter());
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact registerExistingCustomerExistingSite(PactDslWithProvider builder) {

    DslPart existingCustomer = new PactDslJsonBody()
        .stringType("userId", "EXISTING_USER")
        .stringType("existingCustomer", "EXISTING_CUSTOMER")
        .stringType("existingSite", "EXISTING_SITE")
        .stringType("ogelType", "OGL1")
        .stringValue("newSite", null)
        .stringValue("newCustomer", null)
        .object("adminApproval")
        .stringType("adminUserId", "5678")
        .closeObject();

    PactDslJsonBody response = new PactDslJsonBody()
        .stringType("requestId", "1234");

    return builder
        .uponReceiving("request to register OGEL for an existing customer and site")
        .path("/register-ogel")
        .method("POST")
        .matchQuery("callbackUrl", ".*")
        .headers(REQUEST_HEADERS)
        .body(existingCustomer)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(response)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact registerNewCustomer(PactDslWithProvider builder) {

    DslPart newCustomer = new PactDslJsonBody()
        .stringType("userId", "EXISTING_USER")
        .stringValue("existingCustomer", null)
        .stringValue("existingSite", null)
        .stringType("ogelType", "OGL1")
        .stringValue("adminApproval", null)
        .object("newCustomer")
        .stringType("customerName")
        .stringType("customerType")
        .stringType("chNumber")
        .booleanType("chNumberValidated")
        .stringType("eoriNumber")
        .booleanType("eoriNumberValidated")
        .stringType("website")
        .object("registeredAddress")
        .stringType("line1")
        .stringType("line2")
        .stringType("town")
        .stringType("county")
        .stringType("postcode")
        .stringType("country")
        .closeObject()
        .closeObject()
        .object("newSite")
        .stringType("siteName")
        .booleanType("useCustomerAddress", false)
        .object("address")
        .stringType("line1", "1 Some Building")
        .stringType("line2", "Some Street")
        .stringType("town", "London")
        .stringType("county", "County A")
        .stringType("postcode", "A1 1AA")
        .stringType("country", "UK")
        .closeObject()
        .closeObject();

    PactDslJsonBody response = new PactDslJsonBody()
        .stringType("requestId", "1234");

    return builder
        .uponReceiving("request to register OGEL for a new customer and site")
        .path("/register-ogel")
        .method("POST")
        .matchQuery("callbackUrl", ".*")
        .headers(REQUEST_HEADERS)
        .body(newCustomer)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(response)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact registerExistingCustomerNewSite(PactDslWithProvider builder) {

    DslPart newSite = new PactDslJsonBody()
        .stringType("userId", "EXISTING_USER")
        .stringType("existingCustomer", "SAR1")
        .stringValue("existingSite", null)
        .stringValue("newCustomer", null)
        .stringValue("adminApproval", null)
        .stringType("ogelType", "OGL1")
        .object("newSite")
        .stringType("siteName")
        .booleanType("useCustomerAddress", false)
        .object("address")
        .stringType("line1", "1 Some Building")
        .stringType("line2", "Some Street")
        .stringType("town", "London")
        .stringType("county", "County A")
        .stringType("postcode", "A1 1AA")
        .stringType("country", "UK")
        .closeObject()
        .closeObject();

    PactDslJsonBody response = new PactDslJsonBody()
        .stringType("requestId", "1234");

    return builder
        .uponReceiving("request to register OGEL for an existing customer and new site")
        .path("/register-ogel")
        .method("POST")
        .matchQuery("callbackUrl", ".*")
        .headers(REQUEST_HEADERS)
        .body(newSite)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(response)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact existingLicence(PactDslWithProvider builder) {
    DslPart licences = PactDslJsonArray.arrayMinLike(1)
        .stringType("licenceRef", LICENCE_REFERENCE)
        .stringType("originalAppId", "originalAppId")
        .stringType("originalExporterRef", "originalExporterRef")
        .stringType("customerId", "customerId")
        .stringType("siteId", "siteId")
        .stringType("type", "SIEL")
        .stringType("subType", null)
        .stringType("issueDate", "2010-04-21")
        .stringType("expiryDate", "2020-04-21")
        .stringType("status", "ACTIVE")
        .stringType("externalDocumentUrl", "externalDocumentUrl")
        .array("countryList")
        .stringType("Germany")
        .stringType("France")
        .closeArray()
        .closeObject();

    return builder
        .given("licences exist for provided user")
        .uponReceiving("request to get the licence with the given licence reference for the provided user ID")
        .path(LICENCES_URL + USER_ID)
        .query("licenceReference=" + LICENCE_REFERENCE)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(licences)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact noLicence(PactDslWithProvider builder) {
    return builder
        .given("no licences exist for provided user")
        .uponReceiving("request to get the licence with the given licence reference for the provided user ID")
        .path(LICENCES_URL + USER_ID)
        .query("licenceReference=" + LICENCE_REFERENCE)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(404)
        .headers(RESPONSE_HEADERS)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact existingLicences(PactDslWithProvider builder) {

    DslPart licences = PactDslJsonArray.arrayMinLike(1)
        .stringType("licenceRef", LICENCE_REFERENCE)
        .stringType("originalAppId", "originalAppId")
        .stringType("originalExporterRef", "originalExporterRef")
        .stringType("customerId", "customerId")
        .stringType("siteId", "siteId")
        .stringType("type", "SIEL")
        .stringType("subType", null)
        .stringType("issueDate", "2010-04-21")
        .stringType("expiryDate", "2020-04-21")
        .stringType("status", "ACTIVE")
        .stringType("externalDocumentUrl", "externalDocumentUrl")
        .array("countryList")
        .stringType("Germany")
        .stringType("France")
        .closeArray()
        .closeObject();

    return builder
        .given("licences exist for provided user")
        .uponReceiving("request to get licences by user ID")
        .path(LICENCES_URL + USER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(licences)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact noLicences(PactDslWithProvider builder) {

    PactDslJsonArray emptyArrayBody = new PactDslJsonArray();
    return builder
        .given("no licences exist for provided user")
        .uponReceiving("request to get licences by user ID")
        .path(LICENCES_URL + USER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(emptyArrayBody)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userNotFoundLicences(PactDslWithProvider builder) {

    return builder
        .given("provided user does not exist")
        .uponReceiving("request to get licences by user ID")
        .path(LICENCES_URL + USER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(404)
        .headers(RESPONSE_HEADERS)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact existingRegistration(PactDslWithProvider builder) {
    DslPart ogelRegistrations = PactDslJsonArray.arrayMinLike(1)
        .stringType("siteId", "SITE_1")
        .stringType("customerId", "CUSTOMER_1")
        .stringType("registrationReference", REGISTRATION_REFERENCE)
        .stringType("registrationDate", "2015-01-01")
        .stringType("status", "EXTANT")
        .stringType("ogelType", "OGL1")
        .closeObject();

    return builder
        .given("OGEL registrations exist for provided user")
        .uponReceiving("request to get the OGEL registration with the given registration reference for the provided user ID")
        .path(REGISTRATIONS_URL + USER_ID)
        .query("registrationReference=" + REGISTRATION_REFERENCE)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(ogelRegistrations)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact noRegistration(PactDslWithProvider builder) {
    return builder
        .given("no OGEL registrations exist for provided user")
        .uponReceiving("request to get the OGEL registration with the given registration reference for the provided user ID")
        .path(REGISTRATIONS_URL + USER_ID)
        .query("registrationReference=" + REGISTRATION_REFERENCE)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(404)
        .headers(RESPONSE_HEADERS)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact existingRegistrations(PactDslWithProvider builder) {

    DslPart ogelRegistrations = PactDslJsonArray.arrayMinLike(1)
        .stringType("siteId", "SITE_1")
        .stringType("customerId", "CUSTOMER_1")
        .stringType("registrationReference", "REG_123")
        .stringType("registrationDate", "2015-01-01")
        .stringType("status", "EXTANT")
        .stringType("ogelType", "OGL1")
        .closeObject();

    return builder
        .given("OGEL registrations exist for provided user")
        .uponReceiving("request to get OGEL registrations by user ID")
        .path(REGISTRATIONS_URL + USER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(ogelRegistrations)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact noRegistrations(PactDslWithProvider builder) {

    PactDslJsonArray emptyArrayBody = new PactDslJsonArray();
    return builder
        .given("no OGEL registrations exist for provided user")
        .uponReceiving("request to get OGEL registrations by user ID")
        .path(REGISTRATIONS_URL + USER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(200)
        .headers(RESPONSE_HEADERS)
        .body(emptyArrayBody)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact userNotFoundRegistrations(PactDslWithProvider builder) {

    return builder
        .given("provided user does not exist")
        .uponReceiving("request to get OGEL registrations by user ID")
        .path(REGISTRATIONS_URL + USER_ID)
        .method("GET")
        .headers(JWT_AUTHORIZATION_HEADER)
        .willRespondWith()
        .status(404)
        .headers(RESPONSE_HEADERS)
        .toPact();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "registerExistingCustomerExistingSite")
  public void registerExistingCustomerExistingSitePact() throws Exception {
    registerExistingCustomerExistingSite(client);
  }

  public static void registerExistingCustomerExistingSite(PermissionsServiceClient client) throws Exception {
    RegisterParam registerParam = new RegisterParam();
    registerParam.setUserId("EXISTING_USER1");
    registerParam.setExistingCustomer("EXISTING_CUSTOMER");
    registerParam.setExistingSite("EXISTING_SITE");
    registerParam.setOgelType("OGL1");
    RegisterParam.RegisterAdminApprovalParam adminApproval = new RegisterParam.RegisterAdminApprovalParam();
    adminApproval.setAdminUserId("5678");
    registerParam.setAdminApproval(adminApproval);

    String requestId = client.registerOgel(registerParam, CALLBACK_URL).toCompletableFuture().get();
    assertThat(requestId).isEqualTo("1234");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "registerNewCustomer")
  public void registerNewCustomerPact() throws Exception {
    registerNewCustomer(client);
  }

  public static void registerNewCustomer(PermissionsServiceClient client) throws Exception {
    RegisterParam registerParam = new RegisterParam();
    registerParam.setUserId("EXISTING_USER2");
    RegisterParam.RegisterCustomerParam newCustomer = new RegisterParam.RegisterCustomerParam();
    newCustomer.setCustomerName("Customer A");
    newCustomer.setCustomerType("LIMITED");
    RegisterAddressParam registeredAddress = new RegisterAddressParam();
    registeredAddress.setLine1("1 Some Building");
    registeredAddress.setLine2("Some Street");
    registeredAddress.setTown("London");
    registeredAddress.setPostcode("A1 AAA");
    registeredAddress.setCounty("County A");
    registeredAddress.setCountry("UK");
    newCustomer.setRegisteredAddress(registeredAddress);
    newCustomer.setChNumber("435787435");
    newCustomer.setEoriNumber("GB848989");
    newCustomer.setEoriNumberValidated(true);
    newCustomer.setWebsite("http://test.com");
    registerParam.setNewCustomer(newCustomer);
    registerParam.setNewSite(newSite());
    registerParam.setOgelType("OGL1");

    String requestId = client.registerOgel(registerParam, "callbackUrl2").toCompletableFuture().get();
    assertThat(requestId).isEqualTo("1234");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "registerExistingCustomerNewSite")
  public void registerExistingCustomerNewSitePact() throws Exception {
    registerExistingCustomerNewSite(client);
  }

  public static void registerExistingCustomerNewSite(PermissionsServiceClient client) throws Exception {
    RegisterParam registerParam = new RegisterParam();
    registerParam.setUserId("EXISTING_USER3");
    registerParam.setExistingCustomer("SAR1");
    registerParam.setNewSite(newSite());
    registerParam.setOgelType("OGL1");

    String requestId = client.registerOgel(registerParam, "callbackUrl3").toCompletableFuture().get();
    assertThat(requestId).isEqualTo("1234");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "existingLicence")
  public void existingLicencePact() throws Exception {
    existingLicence(client);
  }

  public static void existingLicence(PermissionsServiceClient client) throws Exception {
    LicenceView licenceView = client.getLicence(USER_ID, LICENCE_REFERENCE).toCompletableFuture().get();
    assertThat(licenceView).isNotNull();
    assertThat(licenceView.getLicenceRef()).isEqualTo(LICENCE_REFERENCE);
    assertThat(licenceView.getOriginalAppId()).isEqualTo("originalAppId");
    assertThat(licenceView.getOriginalExporterRef()).isEqualTo("originalExporterRef");
    assertThat(licenceView.getCustomerId()).isEqualTo("customerId");
    assertThat(licenceView.getSiteId()).isEqualTo("siteId");
    assertThat(licenceView.getType()).isEqualTo(LicenceView.Type.SIEL);
    assertThat(licenceView.getSubType()).isNull();
    assertThat(licenceView.getIssueDate()).isEqualTo("2010-04-21");
    assertThat(licenceView.getExpiryDate()).isEqualTo("2020-04-21");
    assertThat(licenceView.getStatus()).isEqualTo(LicenceView.Status.ACTIVE);
    assertThat(licenceView.getCountryList()).containsExactly("Germany", "France");
    assertThat(licenceView.getExternalDocumentUrl()).isEqualTo("externalDocumentUrl");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "noLicence")
  public void noLicencePact() throws Exception {
    noLicence(client);
  }

  public static void noLicence(PermissionsServiceClient client) throws Exception {
    assertThatThrownBy(() -> client.getLicence(USER_ID, LICENCE_REFERENCE).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. permissions-service getLicence failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "existingLicences")
  public void existingLicencesPact() throws Exception {
    existingLicences(client);
  }

  public static void existingLicences(PermissionsServiceClient client) throws Exception {
    List<LicenceView> licences = client.getLicences(USER_ID).toCompletableFuture().get();

    assertThat(licences).hasSize(1);
    LicenceView licenceView = licences.get(0);
    assertThat(licenceView).isNotNull();
    assertThat(licenceView.getLicenceRef()).isEqualTo(LICENCE_REFERENCE);
    assertThat(licenceView.getOriginalAppId()).isEqualTo("originalAppId");
    assertThat(licenceView.getOriginalExporterRef()).isEqualTo("originalExporterRef");
    assertThat(licenceView.getCustomerId()).isEqualTo("customerId");
    assertThat(licenceView.getSiteId()).isEqualTo("siteId");
    assertThat(licenceView.getType()).isEqualTo(LicenceView.Type.SIEL);
    assertThat(licenceView.getSubType()).isNull();
    assertThat(licenceView.getIssueDate()).isEqualTo("2010-04-21");
    assertThat(licenceView.getExpiryDate()).isEqualTo("2020-04-21");
    assertThat(licenceView.getStatus()).isEqualTo(LicenceView.Status.ACTIVE);
    assertThat(licenceView.getCountryList()).containsExactly("Germany", "France");
    assertThat(licenceView.getExternalDocumentUrl()).isEqualTo("externalDocumentUrl");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "noLicences")
  public void noLicencesPact() throws Exception {
    noLicences(client);
  }

  public static void noLicences(PermissionsServiceClient client) throws Exception {
    List<LicenceView> licences = client.getLicences(USER_ID).toCompletableFuture().get();
    assertThat(licences).hasSize(0);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userNotFoundLicences")
  public void userNotFoundLicencesPact() throws Exception {
    userNotFoundLicences(client);
  }

  public static void userNotFoundLicences(PermissionsServiceClient client) {
    assertThatThrownBy(() -> client.getLicences(USER_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. permissions-service getLicences failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "existingRegistration")
  public void existingRegistrationPact() throws Exception {
    existingRegistration(client);
  }

  public static void existingRegistration(PermissionsServiceClient client) throws Exception {
    OgelRegistrationView registration = client.getOgelRegistration(USER_ID, REGISTRATION_REFERENCE).toCompletableFuture().get();
    assertThat(registration).isNotNull();
    assertThat(registration.getCustomerId()).isEqualTo("CUSTOMER_1");
    assertThat(registration.getSiteId()).isEqualTo("SITE_1");
    assertThat(registration.getRegistrationReference()).isEqualTo(REGISTRATION_REFERENCE);
    assertThat(registration.getRegistrationDate()).isEqualTo("2015-01-01");
    assertThat(registration.getStatus()).isEqualTo(OgelRegistrationView.Status.EXTANT);
    assertThat(registration.getOgelType()).isEqualTo("OGL1");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "noRegistration")
  public void noRegistrationPact() {
    noRegistration(client);
  }

  public static void noRegistration(PermissionsServiceClient client) {
    assertThatThrownBy(() -> client.getOgelRegistration(USER_ID, REGISTRATION_REFERENCE).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. permissions-service getOgelRegistration failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "existingRegistrations")
  public void existingRegistrationsPact() throws Exception {
    existingRegistrations(client);
  }

  public static void existingRegistrations(PermissionsServiceClient client) throws Exception {
    List<OgelRegistrationView> registrations = client.getOgelRegistrations(USER_ID).toCompletableFuture().get();

    assertThat(registrations).hasSize(1);
    OgelRegistrationView registration = registrations.get(0);
    assertThat(registration.getCustomerId()).isEqualTo("CUSTOMER_1");
    assertThat(registration.getSiteId()).isEqualTo("SITE_1");
    assertThat(registration.getRegistrationReference()).isEqualTo("REG_123");
    assertThat(registration.getRegistrationDate()).isEqualTo("2015-01-01");
    assertThat(registration.getStatus()).isEqualTo(OgelRegistrationView.Status.EXTANT);
    assertThat(registration.getOgelType()).isEqualTo("OGL1");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "noRegistrations")
  public void noRegistrationsPact() throws Exception {
    noRegistrations(client);
  }

  public static void noRegistrations(PermissionsServiceClient client) throws Exception {
    List<OgelRegistrationView> registrations = client.getOgelRegistrations(USER_ID).toCompletableFuture().get();
    assertThat(registrations.size()).isEqualTo(0);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "userNotFoundRegistrations")
  public void userNotFoundRegistrationsPact() throws Exception {
    userNotFoundRegistrations(client);
  }

  public static void userNotFoundRegistrations(PermissionsServiceClient client) throws Exception {
    assertThatThrownBy(() -> client.getOgelRegistrations(USER_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. permissions-service getOgelRegistrations failure.");
  }

  private static RegisterParam.RegisterSiteParam newSite() {
    RegisterParam.RegisterSiteParam newSite = new RegisterParam.RegisterSiteParam();
    newSite.setUseCustomerAddress(false);
    RegisterAddressParam customerAddress = new RegisterAddressParam();
    customerAddress.setLine1("1 Some Building");
    customerAddress.setLine2("Some Street");
    customerAddress.setTown("London");
    customerAddress.setCounty("County A");
    customerAddress.setPostcode("A1 1AA");
    customerAddress.setCountry("UK");
    newSite.setAddress(customerAddress);
    newSite.setSiteName("Site 1");
    newSite.setUseCustomerAddress(false);
    return newSite;
  }

}
