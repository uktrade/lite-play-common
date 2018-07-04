package components.common.logging;

import org.slf4j.LoggerFactory;
import play.libs.ws.WSRequestFilter;
import play.mvc.Http;

import java.util.UUID;

/**
 * Utility class providing support for Correlation ID related functions.
 * <p>
 * A Correlation ID is an ID value to uniquely identify an entity across a varying array of systems.
 */
public class CorrelationId {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorrelationId.class);

  /**
   * HTTP Header that should contain the Correlation ID when receiving or creating a request
   */
  private static final String HTTP_HEADER_NAME = "X-CorrelationId";

  /**
   * Key to store the Correlation ID against in MDC for logging, use <code>{@code %mdc{corrID} }</code> in a log pattern to log the ID on all messages
   */
  private static final String LOGGING_KEY = "corId";

  /**
   * Get the Correlation ID for this request, or create one if one couldn't be found.
   *
   * @return Correlation ID
   */
  public static String get() {
    try {
      Http.Context context = Http.Context.current();
      String correlationId = (String) context.args.get(LOGGING_KEY);
      if (correlationId == null) {
        correlationId = UUID.randomUUID().toString();
        context.args.put(LOGGING_KEY, correlationId);
      }
      return correlationId;
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * <p>Request filter for use with WSClient which will apply the current Correlation ID to outgoing requests.</p>
   * <p>
   * <p>Example Usage:</p>
   * <pre>{@code
   * ws.url("http://www.example.com/api/v1/example")
   *  .setRequestFilter(CorrelationId.requestFilter)
   *  .get()
   *  .handleAsync((response, error) -> {
   *    if (error != null) {
   *      LOGGER.error(error.getMessage(), error);
   *      return Response.failure(ServiceResponseStatus.UNCHECKED_EXCEPTION);
   *    } else if (response.getStatus() != 200) {
   *      return Response.failure(ServiceResponseStatus.UNEXPECTED_HTTP_STATUS_CODE);
   *    } else {
   *      return Response.success(response.asJson());
   *    }
   *  }, httpExecutionContext.current());
   * }</pre>
   */
  public static final WSRequestFilter requestFilter = executor -> request -> {
    request.addHeader(HTTP_HEADER_NAME, CorrelationId.get());
    return executor.apply(request);
  };
}
