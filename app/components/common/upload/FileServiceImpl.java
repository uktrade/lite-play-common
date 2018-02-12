package components.common.upload;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.spotify.futures.CompletableFutures;
import components.common.client.VirusCheckerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class FileServiceImpl implements FileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

  private final String awsBucketName;
  private final AmazonS3 amazonS3;
  private final VirusCheckerClient virusCheckerClient;
  private final HttpExecutionContext context;

  @Inject
  public FileServiceImpl(@Named("awsBucketName") String awsBucketName,
                         AmazonS3 amazonS3,
                         VirusCheckerClient virusCheckerClient,
                         HttpExecutionContext httpExecutionContext) {
    this.awsBucketName = awsBucketName;
    this.amazonS3 = amazonS3;
    this.virusCheckerClient = virusCheckerClient;
    this.context = httpExecutionContext;
  }

  @Override
  public CompletionStage<List<UploadResult>> processUpload(String folder, Http.Request request) {
    return getMultipartResultsFromRequest(request).stream()
        .map(multipartResult -> processUpload(folder, multipartResult))
        .collect(CompletableFutures.joinList());
  }

  @Override
  public InputStream retrieveFile(String id, String bucket, String folder) {
    return amazonS3.getObject(bucket, folder + "/" + id).getObjectContent();
  }

  @Override
  public void deleteFile(String id, String bucket, String folder) {
    try {
      amazonS3.deleteObject(bucket, folder + "/" + id);
    } catch (Exception exception) {
      String message = String.format("Unable to delete file with id %s and bucket %s and folder %s", id, bucket, folder);
      throw new RuntimeException(message, exception);
    }
  }

  @Override
  public UploadResult uploadToS3(String folder, String filename, Path path) {
    String id = generateFileId();
    File file = path.toFile();
    try {
      amazonS3.putObject(new PutObjectRequest(awsBucketName, folder + "/" + id, file));
    } catch (Exception exception) {
      LOGGER.error("Unable to upload file with filename {} and path {} to amazon s3", filename, path, exception);
      return UploadResult.failedUpload(filename, "An unexpected error occurred.");
    }
    long size = file.length();
    return UploadResult.successfulUpload(id, filename, awsBucketName, folder, size, null);
  }

  private CompletionStage<UploadResult> processUpload(String folder, MultipartResult multipartResult) {
    if (multipartResult.isValid()) {
      return checkForVirus(multipartResult).thenApplyAsync(result -> uploadToS3AndDeleteMultipartResult(folder, result), context.current());
    } else {
      return completedFuture(UploadResult.failedUpload(multipartResult.getFilename(), multipartResult.getError()));
    }
  }

  private UploadResult uploadToS3AndDeleteMultipartResult(String folder, MultipartResult multipartResult) {
    try {
      if (multipartResult.isValid()) {
        return uploadToS3(folder, multipartResult.getFilename(), multipartResult.getPath());
      } else {
        return UploadResult.failedUpload(multipartResult.getFilename(), multipartResult.getError());
      }
    } finally {
      deleteMultipartResult(multipartResult);
    }
  }

  private void deleteMultipartResult(MultipartResult multipartResult) {
    if (multipartResult.getPath() != null) {
      try {
        Files.delete(multipartResult.getPath());
      } catch (Exception exception) {
        LOGGER.error("Unable to delete temp file with filename {} and path {}", multipartResult.getFilename(),
            multipartResult.getPath(), exception);
      }
    }
  }

  private CompletionStage<MultipartResult> checkForVirus(MultipartResult multipartResult) {
    if (multipartResult.isValid()) {
      return virusCheckerClient.isOk(multipartResult.getPath())
          .thenApply(isOk -> {
            if (isOk) {
              return multipartResult;
            } else {
              LOGGER.error("File with filename {} and path {} did not pass virus check", multipartResult.getFilename(), multipartResult.getPath());
              return new MultipartResult(multipartResult.getFilename(), multipartResult.getPath(), "Invalid file");
            }
          })
          .exceptionally(error -> {
            LOGGER.error("A network exception occurred while virus checking file with filename {} and path {}",
                multipartResult.getFilename(), multipartResult.getPath(), error);
            return new MultipartResult(multipartResult.getFilename(), multipartResult.getPath(), "An unexpected error occurred.");
          });
    } else {
      return CompletableFuture.completedFuture(multipartResult);
    }
  }

  /**
   * Null check on 'uploadFile' is workaround for bug https://github.com/playframework/playframework/issues/6203
   */
  private List<MultipartResult> getMultipartResultsFromRequest(Http.Request request) {
    Http.MultipartFormData<MultipartResult> body = request.body().asMultipartFormData();
    return body.getFiles().stream()
        .map(Http.MultipartFormData.FilePart::getFile)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String generateFileId() {
    return "fil_" + UUID.randomUUID().toString().replace("-", "");
  }

}
