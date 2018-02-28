package components.common.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import play.Logger;
import play.libs.ws.WSRequestExecutor;
import play.libs.ws.WSRequestFilter;
import play.mvc.Http;

import java.util.UUID;

/**
 * Utility class providing support for Correlation ID related functions.
 *
 * A Correlation ID is an ID value to uniquely identify an entity across a varying array of systems.
 */
public class CorrelationId {
  /**
   * HTTP Header that should contain the Correlation ID when receiving or creating a request
   */
  private static final String HTTP_HEADER_NAME = "X-CorrelationId";

  /**
   * Key to store the Correlation ID against in MDC for logging, use <code>{@code %mdc{corrID} }</code> in a log pattern to log the ID on all messages
   */
  private static final String MDC_KEY = "corrID";

  /**
   * This function should be called at the start of the request to either set an existing Correlation ID
   * (looked for in the request headers) on the thread for later use or to create a new Correlation ID just in time.
   *
   * @param requestHeader HTTP Request headers which may contain a header called {@link CorrelationId#HTTP_HEADER_NAME}
   *                      with an existing Correlation ID in it
   */
  public static void setUp(Http.RequestHeader requestHeader) {
    String header = requestHeader.getHeader(HTTP_HEADER_NAME);
    if (StringUtils.isNoneBlank(header)) {
      MDC.put(MDC_KEY, header);
    }
    else {
      createCorrelationId();
    }
  }

  /**
   * Get the Correlation ID for this request, or create one if one couldn't be found.
   *
   * @return Correlation ID
   */
  public static String get() {
    // Note that the Correlation ID storage is using MDC (http://logback.qos.ch/manual/mdc.html) which is backed by a
    //   ThreadLocal and considerations should be made when creating async code
    String correlationId = MDC.get(MDC_KEY);
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = createCorrelationId();
    }
    return correlationId;
  }

  /**
   * Create a new Correlation ID
   */
  private static String createCorrelationId() {
    String newCorrelationId = UUID.randomUUID().toString();
    MDC.put(MDC_KEY, newCorrelationId);
    Logger.debug("Correlation ID not found in headers, created new correlation id: " + newCorrelationId);
    return newCorrelationId;
  }

  /**
   * <p>Request filter for use with WSClient which will apply the current Correlation ID to outgoing requests.</p>
   *
   * <p>Example Usage:</p>
   * <pre>{@code
   * ws.url("http://www.example.com/api/v1/example")
   *  .withRequestFilter(CorrelationId.requestFilter)
   *  .get()
   *  .handleAsync((response, error) -> {
   *    if (error != null) {
   *      Logger.error(error.getMessage(), error);
   *      return Response.failure(ServiceResponseStatus.UNCHECKED_EXCEPTION);
   *    } else if (response.getStatus() != 200) {
   *      return Response.failure(ServiceResponseStatus.UNEXPECTED_HTTP_STATUS_CODE);
   *    } else {
   *      return Response.success(response.asJson());
   *    }
   *  }, httpExecutionContext.current());
   * }</pre>
   */
  public static final WSRequestFilter requestFilter = executor -> {
    WSRequestExecutor next = request -> {
      request.setHeader(HTTP_HEADER_NAME, CorrelationId.get());
      return executor.apply(request);
    };
    return next;
  };
}
