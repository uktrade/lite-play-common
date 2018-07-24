package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pact.consumer.components.common.client.TestHelper.BASIC_AUTH_HEADERS;
import static pact.consumer.components.common.client.TestHelper.CONSUMER;
import static pact.consumer.components.common.client.TestHelper.CONTENT_TYPE_HEADERS;
import static pact.consumer.components.common.client.TestHelper.PROVIDER;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import components.common.client.ClientException;
import components.common.client.OgelServiceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.test.WSTestClient;
import uk.gov.bis.lite.ogel.api.view.ApplicableOgelView;
import uk.gov.bis.lite.ogel.api.view.OgelFullView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommonOgelServiceConsumerPact {

  private static final String OGEL_ID = "OGL1";
  private static final String OGEL_NAME = "name";
  private static final String OGEL_LINK = "http://example.org";
  private static final String OGEL_CAN = "can";
  private static final String OGEL_CANT = "can't";
  private static final String OGEL_MUST = "must";
  private static final String OGEL_HOW_TO_USE = "how to use";
  private static final String OGEL_USAGE_SUMMARY = "can";
  private static final String CONTROL_CODE = "ML1a";
  private static final String SOURCE_COUNTRY = "CTRY0";
  private static final String DESTINATION_COUNTRY = "CTRY3";
  private static final String OGEL_ACTIVITY_TYPE_INVALID = "INVALID";
  private static final String OGEL_ACTIVITY_TYPE_DU_ANY = "DU_ANY";
  private static final String OGEL_ACTIVITY_TYPE_MIL_GOV = "MIL_GOV";
  private static final String OGEL_ACTIVITY_TYPE_MIL_ANY = "MIL_ANY";

  @Rule
  public final PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2(PROVIDER, this);

  private WSClient wsClient;
  private OgelServiceClient client;

  @Before
  public void setUp() throws Exception {
    wsClient = WSTestClient.newClient(mockProvider.getPort());
    client = buildClient(wsClient, mockProvider);
  }

  @After
  public void tearDown() throws Exception {
    wsClient.close();
  }

  public static OgelServiceClient buildClient(WSClient wsClient, PactProviderRuleMk2 mockProvider) {
    return new OgelServiceClient(mockProvider.getUrl(),
        10000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run));
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact applicableOgelsExist(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(1, 3)
        .stringType("id", OGEL_ID)
        .stringType("name", OGEL_NAME)
        .array("usageSummary")
        .string(OGEL_USAGE_SUMMARY)
        .closeArray()
        .closeObject()
        .asArray();

    return builder
        .given("applicable ogels exist for given parameters")
        .uponReceiving("a request for applicable ogels")
        .headers(BASIC_AUTH_HEADERS)
        .path("/applicable-ogels")
        .query("controlCode=" + CONTROL_CODE + "&sourceCountry=" + SOURCE_COUNTRY + "&activityType=" + OGEL_ACTIVITY_TYPE_DU_ANY + "&destinationCountry=" + DESTINATION_COUNTRY)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact applicableOgelsDoNotExist(PactDslWithProvider builder) {
    PactDslJsonArray body = new PactDslJsonArray();

    return builder
        .given("no applicable ogels exist for given parameters")
        .uponReceiving("a request for applicable ogels")
        .headers(BASIC_AUTH_HEADERS)
        .path("/applicable-ogels")
        .query("controlCode=" + CONTROL_CODE + "&sourceCountry=" + SOURCE_COUNTRY + "&activityType=" + OGEL_ACTIVITY_TYPE_DU_ANY + "&destinationCountry=" + DESTINATION_COUNTRY)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact applicableOgelsInvalidActivityType(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .integerType("code", 400)
        .stringType("message", "Invalid activity type: " + OGEL_ACTIVITY_TYPE_INVALID)
        .asBody();

    return builder
        .given("activity type does not exist")
        .uponReceiving("a request for applicable ogels")
        .headers(BASIC_AUTH_HEADERS)
        .path("/applicable-ogels")
        .query("controlCode=" + CONTROL_CODE + "&sourceCountry=" + SOURCE_COUNTRY + "&activityType=" + OGEL_ACTIVITY_TYPE_INVALID + "&destinationCountry=" + DESTINATION_COUNTRY)
        .method("GET")
        .willRespondWith()
        .status(400)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact applicableOgelsExistForMultipleActivityTypes(PactDslWithProvider builder) {
    PactDslJsonArray body = PactDslJsonArray.arrayMinLike(1, 3)
        .stringType("id", OGEL_ID)
        .stringType("name", OGEL_NAME)
        .array("usageSummary")
        .string(OGEL_USAGE_SUMMARY)
        .closeArray()
        .closeObject()
        .asArray();

    return builder
        .given("applicable ogels exist for multiple activity types")
        .uponReceiving("a request for applicable ogels")
        .headers(BASIC_AUTH_HEADERS)
        .path("/applicable-ogels")
        .query("controlCode=" + CONTROL_CODE + "&sourceCountry=" + SOURCE_COUNTRY +
            "&activityType=" + OGEL_ACTIVITY_TYPE_MIL_GOV + "&activityType=" + OGEL_ACTIVITY_TYPE_MIL_ANY +
            "&destinationCountry=" + DESTINATION_COUNTRY)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact ogelExists(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .stringType("id", OGEL_ID)
        .stringType("name", OGEL_NAME)
        .stringType("link", OGEL_LINK)
        .object("summary")
        .minArrayLike("canList", 0, PactDslJsonRootValue.stringType(OGEL_CAN), 3)
        .minArrayLike("cantList", 0, PactDslJsonRootValue.stringType(OGEL_CANT), 3)
        .minArrayLike("mustList", 0, PactDslJsonRootValue.stringType(OGEL_MUST), 3)
        .minArrayLike("howToUseList", 0, PactDslJsonRootValue.stringType(OGEL_HOW_TO_USE), 3)
        .closeObject()
        .asBody();

    return builder
        .given("provided OGEL exists")
        .uponReceiving("a request for a given ogel id")
        .headers(BASIC_AUTH_HEADERS)
        .path("/ogels/" + OGEL_ID)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static RequestResponsePact ogelDoesNotExist(PactDslWithProvider builder) {
    PactDslJsonBody body = new PactDslJsonBody()
        .integerType("code", 404)
        .stringType("message", "No Ogel Found With Given Ogel ID: " + OGEL_ID)
        .asBody();

    return builder
        .given("provided OGEL does not exist")
        .uponReceiving("a request for a given ogel id")
        .headers(BASIC_AUTH_HEADERS)
        .path("/ogels/" + OGEL_ID)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(CONTENT_TYPE_HEADERS)
        .body(body)
        .toPact();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "applicableOgelsExist")
  public void applicableOgelsExistPact() throws Exception {
    applicableOgelsExist(client);
  }

  public static void applicableOgelsExist(OgelServiceClient client) throws Exception {
    List<String> activityTypes = Collections.singletonList(OGEL_ACTIVITY_TYPE_DU_ANY);
    List<String> destinationCountries = Collections.singletonList(DESTINATION_COUNTRY);
    List<ApplicableOgelView> result = client.get(CONTROL_CODE, SOURCE_COUNTRY, destinationCountries, activityTypes, true)
        .toCompletableFuture().get();

    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(3);
    ApplicableOgelView ogel = result.get(0);
    assertThat(ogel.getId()).isEqualTo(OGEL_ID);
    assertThat(ogel.getName()).isEqualTo(OGEL_NAME);
    assertThat(ogel.getUsageSummary().size()).isEqualTo(1);
    assertThat(ogel.getUsageSummary().get(0)).isEqualTo(OGEL_USAGE_SUMMARY);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "applicableOgelsDoNotExist")
  public void applicableOgelsDoNotExistPact() throws Exception {
    applicableOgelsDoNotExist(client);
  }

  public static void applicableOgelsDoNotExist(OgelServiceClient client) throws Exception {
    List<String> activityTypes = Collections.singletonList(OGEL_ACTIVITY_TYPE_DU_ANY);
    List<String> destinationCountries = Collections.singletonList(DESTINATION_COUNTRY);
    List<ApplicableOgelView> result = client.get(CONTROL_CODE, SOURCE_COUNTRY, destinationCountries, activityTypes, true)
        .toCompletableFuture().get();

    assertThat(result).isNotNull();
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "applicableOgelsInvalidActivityType")
  public void invalidActivityTypePact() throws Exception {
    applicableOgelsInvalidActivityType(client);
  }

  public static void applicableOgelsInvalidActivityType(OgelServiceClient client) {
    List<String> activityTypes = Collections.singletonList(OGEL_ACTIVITY_TYPE_INVALID);
    List<String> destinationCountries = Collections.singletonList(DESTINATION_COUNTRY);
    assertThatThrownBy(() ->
        client.get(CONTROL_CODE, SOURCE_COUNTRY, destinationCountries, activityTypes, true).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 400. Body is {\"code\":400,\"message\":\"Invalid activity type: INVALID\"}. ogel-service get failure.");
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "applicableOgelsExistForMultipleActivityTypes")
  public void applicableOgelsExistForMultipleActivityTypesTest() throws Exception {
    applicableOgelsExistForMultipleActivityTypes(client);
  }

  public static void applicableOgelsExistForMultipleActivityTypes(OgelServiceClient client) throws Exception {
    List<String> activityTypes = Arrays.asList(OGEL_ACTIVITY_TYPE_MIL_GOV, OGEL_ACTIVITY_TYPE_MIL_ANY);
    List<String> destinationCountries = Collections.singletonList(DESTINATION_COUNTRY);
    List<ApplicableOgelView> result = client.get(CONTROL_CODE, SOURCE_COUNTRY, destinationCountries, activityTypes, true)
        .toCompletableFuture().get();

    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(3);
    ApplicableOgelView ogel = result.get(0);
    assertThat(ogel.getId()).isEqualTo(OGEL_ID);
    assertThat(ogel.getName()).isEqualTo(OGEL_NAME);
    assertThat(ogel.getUsageSummary().size()).isEqualTo(1);
    assertThat(ogel.getUsageSummary().get(0)).isEqualTo(OGEL_USAGE_SUMMARY);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "ogelExists")
  public void ogelExistsPact() throws Exception {
    ogelExists(client);
  }

  public static void ogelExists(OgelServiceClient client) throws Exception {
    OgelFullView result = client.getById(OGEL_ID).toCompletableFuture().get();
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(OGEL_ID);
    assertThat(result.getName()).isEqualTo(OGEL_NAME);
    assertThat(result.getLink()).isEqualTo(OGEL_LINK);
    OgelFullView.OgelConditionSummary summary = result.getSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.getCanList().size()).isEqualTo(3);
    assertThat(summary.getCanList().get(0)).isEqualTo(OGEL_CAN);
    assertThat(summary.getCantList().size()).isEqualTo(3);
    assertThat(summary.getCantList().get(0)).isEqualTo(OGEL_CANT);
    assertThat(summary.getMustList().size()).isEqualTo(3);
    assertThat(summary.getMustList().get(0)).isEqualTo(OGEL_MUST);
    assertThat(summary.getHowToUseList().size()).isEqualTo(3);
    assertThat(summary.getHowToUseList().get(0)).isEqualTo(OGEL_HOW_TO_USE);
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "ogelDoesNotExist")
  public void ogelDoesNotExistPact() throws Exception {
    ogelDoesNotExist(client);
  }

  public static void ogelDoesNotExist(OgelServiceClient client) {
    assertThatThrownBy(() -> client.getById(OGEL_ID).toCompletableFuture().get())
        .hasCauseInstanceOf(ClientException.class)
        .hasMessageEndingWith("Unexpected status code 404. Body is {\"code\":404,\"message\":\"No Ogel Found With Given Ogel ID: OGL1\"}. ogel-service getById failure.");
  }

}
