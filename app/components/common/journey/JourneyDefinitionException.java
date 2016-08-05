package components.common.journey;

public class JourneyDefinitionException
extends RuntimeException {

  public JourneyDefinitionException(String message) {
    super(message);
  }

  public JourneyDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }
}
