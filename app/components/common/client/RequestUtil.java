package components.common.client;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.Arrays;
import java.util.List;

public class RequestUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtil.class);

  public static <T> List<T> parseList(WSRequest request, WSResponse response, Throwable throwable, String service,
                                      String method, Class<T[]> clazz) {
    T[] array = parse(request, response, throwable, service, method, clazz);
    return Arrays.asList(array);
  }

  public static Boolean handleAsBoolean(WSResponse response, Throwable throwable, String service, String method) {
    if (throwable != null) {
      LOGGER.error(service + " failure [" + method + "]", throwable);
      return false;
    } else if (response.getStatus() != 200) {
      LOGGER.error(service + " failure [" + method + "] body [{}]", response.getBody());
      return false;
    }
    return true;
  }

  public static <T> T parse(WSRequest request, WSResponse response, Throwable throwable, String service, String method,
                            Class<T> clazz) {
    if (throwable != null) {
      String message = createMessage(request, service, method);
      LOGGER.error(message, throwable);
      throw new ClientException(message, throwable);
    } else if (response.getStatus() != 200) {
      String message = createMessage(request, response, service, method);
      LOGGER.error(message);
      throw new ClientException(message);
    } else {
      try {
        return Json.fromJson(response.asJson(), clazz);
      } catch (Exception exception) {
        String message = createMessageJsonException(request, response, service, method);
        LOGGER.error(message, exception);
        throw new ClientException(message, exception);
      }
    }
  }

  private static String createMessage(WSRequest request, String service, String method) {
    return String.format("Unable to execute request with path %s. %s %s failure.", request.getUrl(), service, method);
  }

  private static String createMessage(WSRequest request, WSResponse response, String service, String method) {
    return String.format("Unable to execute request with path %s. Unexpected status code %s. Body is %s. %s %s failure.",
        request.getUrl(), response.getStatus(), StringUtils.defaultIfEmpty(response.getBody(), "empty"), service, method);
  }

  private static String createMessageJsonException(WSRequest request, WSResponse response, String service,
                                                   String method) {
    return String.format("Unable to parse request with path %s. Unexpected status code %s. Body is %s. %s %s failure.",
        request.getUrl(), response.getStatus(), StringUtils.defaultIfEmpty(response.getBody(), "empty"), service, method);
  }

}
