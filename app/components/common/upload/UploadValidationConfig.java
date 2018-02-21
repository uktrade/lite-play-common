package components.common.upload;

public class UploadValidationConfig {

  private long maxSize = 0;
  private String disallowedExtensions = "";

  public UploadValidationConfig() {
  }

  public UploadValidationConfig(long maxSize, String disallowedExtensions) {
    this.maxSize = maxSize;
    this.disallowedExtensions = disallowedExtensions;
  }

  public long getMaxSize() {
    return maxSize;
  }

  public String getDisallowedExtensions() {
    return disallowedExtensions;
  }

}
