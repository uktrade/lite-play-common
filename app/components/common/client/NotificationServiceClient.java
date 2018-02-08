package components.common.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import play.Logger;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class NotificationServiceClient {

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient ws;
  private final int wsTimeout;
  private final String wsUrl;
  private final String credentials;

  private static final String TEMPLATE_QUERY_NAME = "template";
  private static final String RECIPIENT_EMAIL_QUERY_NAME = "recipientEmail";

  @Inject
  public NotificationServiceClient(HttpExecutionContext httpExecutionContext,
                                   WSClient ws,
                                   @Named("notificationServiceAddress") String wsAddress,
                                   @Named("notificationServiceTimeout") int wsTimeout,
                                   @Named("notificationServiceCredentials") String credentials) {
    this.httpExecutionContext = httpExecutionContext;
    this.ws = ws;
    this.wsTimeout = wsTimeout;
    this.wsUrl = wsAddress + "/notification/send-email";
    this.credentials = credentials;
  }

  /**
   * Sends an email using the Notification service.
   * @param templateName Template name of email to send. This must be registered on the Notification service.
   * @param emailAddress Recipient email address.
   * @param templateParams Name/value pairs of parameters for the given template. This map must match the parameter
   */
  public CompletionStage<Boolean> sendEmail(String templateName, String emailAddress, Map<String, String> templateParams) {

    Logger.info("notification [" + templateName + "|" + emailAddress + "]");

    ObjectNode nameValueJson = Json.newObject();
    templateParams.forEach((k, v) -> nameValueJson.put(k, v));

    CompletionStage<WSResponse> wsResponse = ws.url(wsUrl)
        .withRequestFilter(CorrelationId.requestFilter)
        .setHeader("Content-Type", "application/json")
        .setRequestTimeout(wsTimeout)
        .setQueryParameter(TEMPLATE_QUERY_NAME, templateName)
        .setQueryParameter(RECIPIENT_EMAIL_QUERY_NAME, emailAddress)
        .setAuth(credentials)
        .post(nameValueJson);

    CompletionStage<Boolean> result = wsResponse.thenComposeAsync(r -> {
      Logger.info("notification [response status: " + r.getStatus() + "]");
      if (r.getStatus() == 200) {
        return CompletableFuture.supplyAsync(() -> true);
      } else {
        return CompletableFuture.supplyAsync(() -> false);
      }
    }, httpExecutionContext.current());

    return result.handleAsync((responseIsOk, error) -> {
      if (error != null || !responseIsOk) {
        Logger.error("Notification service failure");
        if (error != null) {
          Logger.error(error.getMessage(), error);
        }
        return false;
      }
      else {
        return true;
      }
    }, httpExecutionContext.current());
  }

}
