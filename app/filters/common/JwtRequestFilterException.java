package filters.common;

public class JwtRequestFilterException extends RuntimeException {
  public JwtRequestFilterException(String message) {
    super(message);
  }

  public JwtRequestFilterException(String message, Throwable cause) {
    super(message, cause);
  }
}
