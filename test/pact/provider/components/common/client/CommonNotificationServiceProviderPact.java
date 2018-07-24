package pact.provider.components.common.client;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableMap;
import components.common.client.NotificationServiceClient;
import org.mockito.ArgumentCaptor;

public class CommonNotificationServiceProviderPact {

  public static String validEmailNotification(AmazonSQS amazonSQS) {
    NotificationServiceClient notificationServiceClient = new NotificationServiceClient("url", amazonSQS);
    notificationServiceClient.sendEmail("validTemplate", "user@test.com",
        ImmutableMap.of("validParamOne", "valueOne", "validParamTwo", "valueTwo"));

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(amazonSQS).sendMessage(eq("url"), captor.capture());

    return captor.getValue();
  }

}
