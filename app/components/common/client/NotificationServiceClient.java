package components.common.client;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.LoggerFactory;
import uk.gov.bis.lite.notification.api.EmailNotification;

import java.util.Map;

public class NotificationServiceClient {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NotificationServiceClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final AmazonSQS amazonSQS;
  private final String notificationServiceAwsSqsQueueUrl;

  @Inject
  public NotificationServiceClient(@Named("notificationServiceAwsSqsQueueUrl") String notificationServiceAwsSqsQueueUrl,
                                   AmazonSQS amazonSQS) {
    this.notificationServiceAwsSqsQueueUrl = notificationServiceAwsSqsQueueUrl;
    this.amazonSQS = amazonSQS;
  }

  /**
   * Sends an email using the Notification service.
   *
   * @param templateName    Template name of email to send. This must be registered on the Notification service.
   * @param emailAddress    Recipient email address.
   * @param personalisation Name/value pairs of parameters for the given template. This map must match the parameter
   */
  public void sendEmail(String templateName, String emailAddress, Map<String, String> personalisation) {
    EmailNotification emailNotification = new EmailNotification();
    emailNotification.setTemplate(templateName);
    emailNotification.setEmailAddress(emailAddress);
    emailNotification.setPersonalisation(personalisation);
    String message;
    try {
      message = MAPPER.writeValueAsString(emailNotification);
    } catch (JsonProcessingException jpe) {
      throw new RuntimeException("Unable to write email notification as string ", jpe);
    }
    try {
      LOGGER.info("Sending message type {} to {}", templateName, emailAddress);
      amazonSQS.sendMessage(notificationServiceAwsSqsQueueUrl, message);
    } catch (Exception exception) {
      LOGGER.error("Unable to send message {}", message, exception);
    }
  }

}
