package components.common.logging;

import com.google.common.base.Stopwatch;
import org.apache.http.client.utils.URIBuilder;
import play.Logger;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.WSRequestFilter;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Utility class providing logging of HTTP/HTTPS request and responses for use in service clients
 */
public class ServiceClientLogger {

  private ServiceClientLogger() {
  }

  /**
   * Builds a URL from a {@link play.libs.ws.WSRequest} object
   *
   * @param request
   * @return the URL with parameters, defaults to the requests {@link play.libs.ws.WSRequest#getUrl()} if the build is unsuccessful
   */
  private static String requestToURL(StandaloneWSRequest request) {
    // Default to request URL without parameters
    String url = request.getUrl();
    Map<String, List<String>> queryParameters = request.getQueryParameters();
    if (!queryParameters.isEmpty()) {
      // Build a URL which includes parameters
      try {
        URIBuilder uriBuilder = new URIBuilder(request.getUrl());
        // Loop through params
        for (Entry<String, List<String>> paramEntry : queryParameters.entrySet()) {
          // Loop through param values, add to URI builder
          for (String paramValue : paramEntry.getValue()) {
            uriBuilder.addParameter(paramEntry.getKey(), paramValue);
          }
        }
        // Build URI and construct URL
        url = uriBuilder.build().toURL().toString();
      } catch (URISyntaxException | MalformedURLException exception) {
        Logger.error("Failed to build URI for request", exception);
      }
    }
    return url;
  }

  /**
   * <p>Request filter for use with WSClient which logs outgoing http requests for use in service clients.</p>
   * <p>
   * <p>Example usage:</p>
   * <pre>{@code
   * ws.url("http://www.example.com/api")
   *  .setRequestFilter(serviceClientLogger.requestFilter("Example", "GET"))
   *  .setQueryParameter("version", 1)
   *  .get()
   *  .thenApplyAsync(response -> {
   *    if (response.getStatus() != 200) {
   *      throw new RuntimeException(String.format("Unexpected HTTP status code %s", response.getStatus()));
   *    }
   *    else {
   *      return response.asJson();
   *    }
   * }, httpExecutionContext.current());
   * }
   * </pre>
   *
   * @param serviceName          name of the service which the request is sent too (used for logging purposes only)
   * @param method               HTTP method used in this request (used for logging purposes only)
   * @param httpExecutionContext The HTTP context in which the logs will be written
   * @return a request filter which logs the request
   */
  public static WSRequestFilter requestFilter(String serviceName, String method, HttpExecutionContext httpExecutionContext) {
    return executor -> request -> {
      String url = requestToURL(request);
      Logger.info(String.format("%s service request - URL: %s, method: %s", serviceName, url, method));
      Stopwatch stopwatch = Stopwatch.createStarted();
      return executor.apply(request)
          .thenApplyAsync(response -> {
            Logger.info(String.format("%s service response - status code: %s, status text: %s, completed in %dms",
                serviceName, response.getStatus(), response.getStatusText(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
            return response;
          }, httpExecutionContext.current());
    };
  }
}
