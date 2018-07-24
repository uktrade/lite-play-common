package components.common.client;

import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
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

  private static final String VIRUS_CHECKER_SERVICE = "virus-checker-service";

  private final HttpExecutionContext context;
  private final WSClient wsClient;
  private final String credentials;
  private final String address;
  private final int timeout;

  @Inject
  public VirusCheckerClient(@Named("virusServiceAddress") String address,
                            @Named("virusServiceTimeout") int timeout,
                            @Named("virusServiceCredentials") String credentials,
                            WSClient wsClient, HttpExecutionContext httpExecutionContext) {
    this.address = address;
    this.timeout = timeout;
    this.credentials = credentials;
    this.wsClient = wsClient;
    this.context = httpExecutionContext;
  }

  public CompletionStage<Boolean> isOk(Path path) {
    WSRequest request = wsClient.url(address)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("VirusCheck", "POST", context))
        .setAuth(credentials)
        .setRequestTimeout(Duration.ofMillis(timeout));
    // https://www.playframework.com/documentation/2.5.x/JavaWS#Submitting-multipart/form-data
    Source<ByteString, ?> file = FileIO.fromFile(path.toFile());
    FilePart<Source<ByteString, ?>> fp = new FilePart<>("file", "file.txt", "text/plain", file);
    DataPart dp = new DataPart("key", "value");
    return request.post(Source.from(Arrays.asList(fp, dp))).handleAsync((response, error) ->
            "OK".equals(RequestUtil.parse(request, response, error, VIRUS_CHECKER_SERVICE, "isOk", String.class)),
        context.current());
  }

}
