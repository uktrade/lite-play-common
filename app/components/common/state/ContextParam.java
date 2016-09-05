package components.common.state;

/**
 * Name/value tuple for a parameter retrieved from a {@link ContextParamManager}.
 */
public class ContextParam {

  private final String name;
  private final String value;

  public ContextParam(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
