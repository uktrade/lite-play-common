package pact.consumer.components.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import components.common.client.NotificationServiceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.test.WSTestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;


public class NotificationClientConsumerPact {
  public static final String PROVIDER = "lite-play-common";
  public static final String CONSUMER = "lite-play-common";

  public WSClient ws;

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(PROVIDER, this);

  @Before
  public void setUp() throws Exception {
    ws = WSTestClient.newClient(mockProvider.getConfig().getPort());
  }

  @After
  public void tearDown() throws Exception {
    ws.close();
  }

  private static Map<String, String> jsonHeaderRequest() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Authorization", "Basic c2VydmljZTpwYXNzd29yZA==");
    return headers;
  }

  private static Map<String, String> jsonHeaderResponse() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return headers;
  }

  public static NotificationServiceClient buildNotificationServiceClient(PactProviderRule mockProvider, WSClient wsClient) {
    return new NotificationServiceClient(new HttpExecutionContext(Runnable::run), wsClient, mockProvider.getConfig().url(), 1000, "service:password");
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static PactFragment successfulNotification(PactDslWithProvider builder) {
    PactDslJsonBody requestBody = new PactDslJsonBody()
      .stringType("valid_param_1", "param1")
      .stringType("valid_param_2", "param2");

    return builder
        .given("provided template name and parameters are valid")
        .uponReceiving("a request to send an email notification")
          .path("/notification/send-email")
          .query("template=validTemplate&recipientEmail=recipient@email.com")
          .method("POST")
          .headers(jsonHeaderRequest())
          .body(requestBody)
        .willRespondWith()
          .status(200)
          .headers(jsonHeaderResponse())
          .body(new PactDslJsonBody().stringValue("status", "success"))
        .toFragment();
  }

  @Pact(provider = PROVIDER, consumer = CONSUMER)
  public static PactFragment unsuccessfulNotification(PactDslWithProvider builder) {
    return builder
        .given("provided template information is invalid")
        .uponReceiving("a request to send an email notification")
          .path("/notification/send-email")
          .query("template=invalidTemplate&recipientEmail=recipient@email.com")
          .method("POST")
          .headers(jsonHeaderRequest())
        .willRespondWith()
          .status(400)
          .headers(jsonHeaderResponse())
          .body(new PactDslJsonBody().numberValue("code", 400).stringType("message", "incorrect template"))
        .toFragment();
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "successfulNotification")
  public void successfulNotificationTest() {
    doSuccessfulNotificationSend(buildNotificationServiceClient(mockProvider, ws));
  }

  public static void doSuccessfulNotificationSend(NotificationServiceClient client) {

    Map<String, String> paramMap = new HashMap<>();
    paramMap.put("valid_param_1", "param1");
    paramMap.put("valid_param_2", "param2");

    CompletionStage<Boolean> result = client.sendEmail("validTemplate", "recipient@email.com", paramMap);

    try {
      assertThat(result.toCompletableFuture().get()).isTrue();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @PactVerification(value = PROVIDER, fragment = "unsuccessfulNotification")
  public void unsuccessfulNotificationTest() {
    doUnsuccessfulNotificationSend(buildNotificationServiceClient(mockProvider, ws));
  }

  public static void doUnsuccessfulNotificationSend(NotificationServiceClient client) {

    Map<String, String> paramMap = new HashMap<>();
    paramMap.put("valid_param_1", "param1");
    paramMap.put("valid_param_2", "param2");

    CompletionStage<Boolean> result = client.sendEmail("invalidTemplate", "recipient@email.com", paramMap);

    try {
      assertThat(result.toCompletableFuture().get()).isFalse();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
