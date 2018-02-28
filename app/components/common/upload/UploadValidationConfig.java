package components.common.upload;

public class UploadValidationConfig {

  private long maxSize = 0;
  private String allowedExtensions = "";

  public UploadValidationConfig() {
  }

  public UploadValidationConfig(long maxSize, String allowedExtensions) {
    this.maxSize = maxSize;
    this.allowedExtensions = allowedExtensions;
  }

  public long getMaxSize() {
    return maxSize;
  }

  public String getAllowedExtensions() {
    return allowedExtensions;
  }

}
