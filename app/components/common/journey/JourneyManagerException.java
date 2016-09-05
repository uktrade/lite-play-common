package components.common.journey;

public class JourneyManagerException extends RuntimeException {

  public JourneyManagerException(String message) {
    super(message);
  }

  public JourneyManagerException(String message, Throwable cause) {
    super(message, cause);
  }
}
