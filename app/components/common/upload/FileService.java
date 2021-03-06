package components.common.upload;

import play.mvc.Http;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface FileService {

  String FILE_UPLOAD_FORM_FIELD = "fileupload";

  String INVALID_FILE_SIZE_QUERY_PARAM = "invalidFileSize";

  CompletionStage<List<UploadResult>> processUpload(String folder, Http.Request request);

  InputStream retrieveFile(String id, String bucket, String folder);

  void deleteFile(String id, String bucket, String folder);

  UploadResult uploadToS3(String folder, String filename, Path path);

}
