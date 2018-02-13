package components.common.upload;

public class FileUploadResponseItem {

  private String name;
  private String url;
  private String error;
  private String size;
  private String param1;
  private String param2;
  private String param3;

  public FileUploadResponseItem(String name, String url, String error, String size, String param1, String param2, String param3) {
    this.name = name;
    this.url = url;
    this.error = error;
    this.size = size;
    this.param1 = param1;
    this.param2 = param2;
    this.param3 = param3;
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

  public String getParam1() {
    return param1;
  }

  public String getParam2() {
    return param2;
  }

  public String getParam3() {
    return param3;
  }

}
