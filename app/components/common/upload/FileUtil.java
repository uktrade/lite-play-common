package components.common.upload;

import static play.mvc.Controller.flash;

import play.data.Form;
import play.mvc.Http;

import java.text.DecimalFormat;
import java.util.List;

public class FileUtil {

  private static final String[] UNITS = new String[]{" bytes", "KB", "MB", "GB", "TB"};

  public static String getReadableFileSize(long size) {
    if (size <= 0) {
      return "0";
    } else {
      int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
      return new DecimalFormat("#,##0").format(size / Math.pow(1024, digitGroups)) + UNITS[digitGroups];
    }
  }

  public static void addUploadErrorsToForm(Form form, List<UploadResult> uploadResults) {
    uploadResults.stream()
        .filter(uploadResult -> !uploadResult.isValid())
        .forEach(uploadResult -> {
          form.reject(FileService.FILE_UPLOAD_FORM_FIELD,
              "Error for file " + uploadResult.getFilename() + ": " + uploadResult.getError());
        });
  }

  public static void addFlash(Http.Request request, UploadValidationConfig uploadValidationConfig) {
    if (Boolean.parseBoolean(request.getQueryString(FileService.INVALID_FILE_SIZE_QUERY_PARAM))) {
      flash("message", "Unable to upload");
      flash("detail", "The maximum file size is " + getReadableFileSize(uploadValidationConfig.getMaxSize()));
    }
  }

}
