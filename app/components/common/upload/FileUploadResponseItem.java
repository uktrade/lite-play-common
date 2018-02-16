package components.common.upload;

public class FileUploadResponseItem {

  private final String name;
  private final String url;
  private final String size;
  private final String jsDeleteLink;
  private final String error;

  public FileUploadResponseItem(String name, String url, String size, String jsDeleteLink, String error) {
    this.name = name;
    this.url = url;
    this.size = size;
    this.jsDeleteLink = jsDeleteLink;
    this.error = error;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getSize() {
    return size;
  }

  public String getJsDeleteLink() {
    return jsDeleteLink;
  }

  public String getError() {
    return error;
  }

}
