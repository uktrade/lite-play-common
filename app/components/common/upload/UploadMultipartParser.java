package components.common.upload;

import static play.mvc.Results.badRequest;

import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.http.HttpConfiguration;
import play.api.http.HttpErrorHandler;
import play.core.parsers.Multipart;
import play.libs.F;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import scala.Option;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UploadMultipartParser extends BodyParser.DelegatingMultipartFormDataBodyParser<MultipartResult> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UploadMultipartParser.class);
  private static final Pattern FILENAME_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-_]");
  private static final Pattern FILE_ENDING_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

  private final long maxSize;
  private final List<String> allowedExtensions;

  @Inject
  public UploadMultipartParser(Materializer materializer,
                               HttpConfiguration httpConfig,
                               HttpErrorHandler errorHandler,
                               UploadValidationConfig uploadValidationConfig) {
    super(materializer, httpConfig.parser().maxDiskBuffer(), errorHandler);
    this.maxSize = uploadValidationConfig.getMaxSize();
    this.allowedExtensions = createAllowedExtensions(uploadValidationConfig);
  }

  private List<String> createAllowedExtensions(UploadValidationConfig uploadValidationConfig) {
    List<String> normalized = Arrays.stream(uploadValidationConfig.getAllowedExtensions().split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
    if (normalized.stream().anyMatch(extension -> !FILE_ENDING_PATTERN.matcher(extension).matches())) {
      throw new RuntimeException("All extensions must be alphanumeric");
    } else if (normalized.stream().anyMatch(extension -> extension.length() > 10)) {
      throw new RuntimeException("All extensions must have length less than 11");
    } else {
      return normalized.stream()
          .map(extension -> "." + extension)
          .collect(Collectors.toList());
    }
  }

  @Override
  public Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<MultipartResult>>> apply(
      Http.RequestHeader request) {
    Optional<String> headerOptional = request.header("Content-Length");
    if (!headerOptional.isPresent()) {
      LOGGER.warn("Rejecting request with no Content-Length header");
      return redirect(request);
    } else {
      String header = headerOptional.get();
      long contentLength;
      try {
        contentLength = Long.parseLong(header);
      } catch (NumberFormatException nfe) {
        LOGGER.warn("Rejecting request of size {}", header);
        return redirect(request);
      }
      if (contentLength < 1) {
        LOGGER.warn("Rejecting request of size {}", header);
        return Accumulator.done(F.Either.Left(badRequest("bad content length")));
      } else if (contentLength > maxSize) {
        LOGGER.warn("Rejecting request of size {}", header);
        return redirect(request);
      } else {
        return super.apply(request);
      }
    }
  }

  private Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<MultipartResult>>> redirect(
      Http.RequestHeader request) {
    String modifier;
    if (request.uri().contains("?")) {
      modifier = "&";
    } else {
      modifier = "?";
    }
    String url = request.uri() + modifier + FileService.INVALID_FILE_SIZE_QUERY_PARAM + "=true";
    return Accumulator.done(F.Either.Left(Results.redirect(url)));
  }

  /**
   * Creates a file part handler that uses a custom accumulator.
   */
  @Override
  public Function<Multipart.FileInfo, Accumulator<ByteString, Http.MultipartFormData.FilePart<MultipartResult>>> createFilePartHandler() {
    return (Multipart.FileInfo fileInfo) -> {
      String filename = fileInfo.fileName();
      String partName = fileInfo.partName();
      String contentType = parse(fileInfo.contentType());
      // Workaround for bug https://github.com/playframework/playframework/issues/6203, i.e.
      // even if the user doesn't select any file, this code is called with filename as empty string
      if ("".equals(filename)) {
        return Accumulator.done(new Http.MultipartFormData.FilePart<>(partName, filename, contentType, null));
      } else if (StringUtils.isBlank(filename)) {
        String errorMessage = "Blank filename not allowed";
        MultipartResult multipartResult = new MultipartResult(filename, null, errorMessage);
        return Accumulator.done(new Http.MultipartFormData.FilePart<>(partName, filename, contentType, multipartResult));
      } else {
        if (!isExtensionAllowed(filename)) {
          String errorMessage = "File ending not allowed";
          MultipartResult multipartResult = new MultipartResult(filename, null, errorMessage);
          return Accumulator.done(new Http.MultipartFormData.FilePart<>(partName, filename, contentType, multipartResult));
        } else {
          Optional<String> normalizedFilename = normalizeFilename(filename);
          if (!normalizedFilename.isPresent()) {
            String errorMessage = "Filename not allowed";
            MultipartResult multipartResult = new MultipartResult(filename, null, errorMessage);
            return Accumulator.done(new Http.MultipartFormData.FilePart<>(partName, filename, contentType, multipartResult));
          } else {
            Path path;
            try {
              path = Files.createTempFile("lite", null);
            } catch (IOException ioe) {
              throw new RuntimeException("Unable to create temp file", ioe);
            }
            MultipartResult multipartResult = new MultipartResult(normalizedFilename.get(), path, null);
            Sink<ByteString, CompletionStage<IOResult>> sink = StreamConverters.fromOutputStream(() -> new FileOutputStream(path.toFile()));
            return Accumulator.fromSink(
                sink.mapMaterializedValue(completionStage ->
                    completionStage.thenApplyAsync(results ->
                        new Http.MultipartFormData.FilePart<>(partName, normalizedFilename.get(), contentType, multipartResult))
                ));
          }
        }
      }
    };
  }

  @VisibleForTesting
  protected Optional<String> normalizeFilename(String filename) {
    int lastDot = filename.lastIndexOf(".");
    String fileStart = filename.substring(0, lastDot);
    String fileEnding = filename.substring(lastDot + 1, filename.length());
    String fileStartClean = FILENAME_PATTERN.matcher(fileStart).replaceAll("");
    if (fileStartClean.length() < 1 || fileStartClean.length() > 240) {
      return Optional.empty();
    } else {
      return Optional.of(fileStartClean + "." + fileEnding);
    }
  }

  private boolean isExtensionAllowed(String filename) {
    String lowercase = filename.toLowerCase();
    return allowedExtensions.stream()
        .anyMatch(lowercase::endsWith);
  }

  private String parse(Option<String> stringOption) {
    return stringOption.isDefined() ? stringOption.get() : null;
  }

}
