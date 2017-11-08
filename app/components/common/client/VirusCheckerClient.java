package components.common.client;

import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Http.MultipartFormData.DataPart;
import play.mvc.Http.MultipartFormData.FilePart;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;

public class VirusCheckerClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(VirusCheckerClient.class);

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final String credentials;
  private final String address;
  private final int timeout;

  @Inject
  public VirusCheckerClient(HttpExecutionContext httpExecutionContext,
                            WSClient wsClient,
                            @Named("virusServiceCredentials") String credentials,
                            @Named("virusServiceAddress") String address,
                            @Named("virusServiceTimeout") int timeout) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.credentials = credentials;
    this.address = address;
    this.timeout = timeout;
  }

  public CompletionStage<Boolean> isOk(Path path) {
    WSRequest req = wsClient.url(address)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("VirusCheck", "POST", httpExecutionContext))
        .setAuth(credentials)
        .setRequestTimeout(Duration.ofMillis(timeout));
    // https://www.playframework.com/documentation/2.5.x/JavaWS#Submitting-multipart/form-data
    Source<ByteString, ?> file = FileIO.fromFile(path.toFile());
    FilePart<Source<ByteString, ?>> fp = new FilePart<>("file", "file.txt", "text/plain", file);
    DataPart dp = new DataPart("key", "value");
    CompletionStage<String> request = req.post(Source.from(Arrays.asList(fp, dp))).handle((response, error) -> {
      if (error != null) {
        String message = "Unable to virus check file " + path.toString();
        throw new RuntimeException(message, error);
      } else if (response.getStatus() != 200) {
        String message = String.format("Unexpected HTTP status code %d. Unable to virus check file  %s",
            response.getStatus(), path.toString());
        throw new RuntimeException(message);
      } else {
        return response.getBody();
      }
    });
    return request.thenApply("OK"::equals);
  }

}
