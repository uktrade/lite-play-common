import components.common.logging.CorrelationId;
import io.netty.handler.codec.http.HttpResponseStatus;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.api.OptionalSourceMapper;
import play.api.PlayException;
import play.api.http.HttpErrorHandlerExceptions;
import play.http.HttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;
import scala.Option;
import views.html.common.error.serverError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Custom error handler that renders using a Gov UK styled page
 *
 * Set config 'errorDetailEnabled = true' to get error detail output even when running in production mode.
 */
@Singleton
public class ErrorHandler implements HttpErrorHandler {
  private final Environment environment;
  private final OptionalSourceMapper sourceMapper;
  private final boolean isErrorDetailEnabled;

  @Inject
  public ErrorHandler(Environment environment,
                      OptionalSourceMapper sourceMapper,
                      Configuration configuration) {
    this.environment = environment;
    this.sourceMapper = sourceMapper;
    // Error detail is enabled if configured to true or if the environment is running in dev mode
    this.isErrorDetailEnabled = configuration.getBoolean("errorDetailEnabled", false) || environment.isDev();
  }


  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.
   * @param message The error message.
   */
  public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
    Option<Exception> optionalException = Option.empty();

    String outputMessage = HttpResponseStatus.valueOf(statusCode).toString();
    if (message != null && !message.isEmpty()) {
      outputMessage += " - " + message;
    }
    MessageOnlyException cause = new MessageOnlyException(outputMessage);

    Logger.error(String.format("\n\n! @%s - Client error, for (%s) [%s] ->\n",
        CorrelationId.get(), request.method(), request.uri()), cause
    );

    if (isErrorDetailEnabled) {
      PlayException exception = new PlayException("Client Error", outputMessage, cause);
      optionalException = Option.apply(exception);
    }

    return CompletableFuture.completedFuture(Results.status(statusCode, serverError.render(optionalException, Option.empty(), CorrelationId.get())));
  }

  /**
   * Invoked when a server error occurs.
   *
   * @param request The request that triggered the server error.
   * @param exception The server error.
   */
  public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
    Option<Exception> optionalException = Option.empty();
    Option<String> optionalFormattedStackTrace = Option.empty();

    Logger.error(String.format("\n\n! @%s - Internal server error, for (%s) [%s] ->\n",
        CorrelationId.get(), request.method(), request.uri()), exception
    );

    if (isErrorDetailEnabled) {
      optionalException = Option.apply(HttpErrorHandlerExceptions.throwableToUsefulException(sourceMapper.sourceMapper(), environment.isProd(), exception));

      // Get stack trace
      StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));
      optionalFormattedStackTrace = Option.apply(sw.toString());
    }

    return CompletableFuture.completedFuture(Results.internalServerError(serverError.render(optionalException, optionalFormattedStackTrace, CorrelationId.get())));
  }

  /**
   * Exception that will only display the message and not show a stack trace.
   */
  private class MessageOnlyException extends Exception {
    MessageOnlyException(String message) {
      super(message, null, true, false);
    }

    @Override
    public String toString() {
      return getLocalizedMessage();
    }
  }
}