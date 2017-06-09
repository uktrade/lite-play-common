package utils.common;

import play.mvc.Http;

public class UploadUtil {

  public static boolean isMultipartRequest(Http.Request request) {
    return "multipart/form-data".equals(request.contentType().orElse(""));
  }

}
