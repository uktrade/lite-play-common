package components.common.upload;


public class FileView {

  private final String id;
  private final String name;
  private final String link;
  private final String size;
  private final String jsDeleteLink;

  public FileView(String id, String name, String link, String size, String jsDeleteLink) {
    this.id = id;
    this.name = name;
    this.link = link;
    this.size = size;
    this.jsDeleteLink = jsDeleteLink;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLink() {
    return link;
  }

  public String getSize() {
    return size;
  }

  public String getJsDeleteLink() {
    return jsDeleteLink;
  }

}
