package components.common.upload;

public class FileUploadResponseItem {

  private final String name;
  private final String url;
  private final String error;
  private final String size;
  private final String fileProperties;

  public FileUploadResponseItem(String name, String url, String error, String size, String fileProperties) {
    this.name = name;
    this.url = url;
    this.error = error;
    this.size = size;
    this.fileProperties = fileProperties;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getError() {
    return error;
  }

  public String getSize() {
    return size;
  }

  public String getFileProperties() {
    return fileProperties;
  }

}
