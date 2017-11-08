package components.common.upload;

import play.data.Form;

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

  public static <T> Form<T> addUploadErrorsToForm(Form<T> form, List<UploadResult> uploadResults) {
    for (UploadResult uploadResult : uploadResults) {
      if (!uploadResult.isValid()) {
        String error = String.format("Error for file %s: %s", uploadResult.getFilename(), uploadResult.getError());
        form = form.withError("fileupload", error);
      }
    }
    return form;
  }

}
