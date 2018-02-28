package components.common.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import play.api.http.HttpConfiguration;
import play.api.http.ParserConfiguration;

import java.util.Optional;

public class UploadMultipartParserTest {

  private static final HttpConfiguration HTTP_CONFIGURATION = new HttpConfiguration(null,
      new ParserConfiguration(1, 1), null, null, null, null);
  private static final long MAX_SIZE = 10485760;

  @Test
  public void extensionListCannotBeEmpty() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "");
    assertThatThrownBy(() -> new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("All extensions must be alphanumeric");
  }

  @Test
  public void extensionCannotBeEmpty() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "txt, ,pdf");
    assertThatThrownBy(() -> new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("All extensions must be alphanumeric");
  }

  @Test
  public void extensionCannotBeMoreThanTenCharacters() {
    String longString = StringUtils.repeat("a", 11);
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, longString);
    assertThatThrownBy(() -> new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("All extensions must have length less than 11");
  }

  @Test
  public void normalizeFilenameShouldRemoveSpaces() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "txt");
    UploadMultipartParser uploadMultipartParser = new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig);
    Optional<String> normalizedFilename = uploadMultipartParser.normalizeFilename("a b c.txt");
    assertThat(normalizedFilename).hasValue("abc.txt");
  }

  @Test
  public void normalizeFilenameShouldRemoveSpecialCharacters() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "txt");
    UploadMultipartParser uploadMultipartParser = new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig);
    Optional<String> normalizedFilename = uploadMultipartParser.normalizeFilename("a*b.c.txt");
    assertThat(normalizedFilename).hasValue("abc.txt");
  }

  @Test
  public void normalizeFilenameShouldLeaveDashesAndUnderscores() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "txt");
    UploadMultipartParser uploadMultipartParser = new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig);
    Optional<String> normalizedFilename = uploadMultipartParser.normalizeFilename("-_-_.txt");
    assertThat(normalizedFilename).hasValue("-_-_.txt");
  }

  @Test
  public void normalizeFilenameShouldReturnEmptyOptionalForEmptyFilename() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "txt");
    UploadMultipartParser uploadMultipartParser = new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig);
    Optional<String> normalizedFilename = uploadMultipartParser.normalizeFilename("*&%.txt");
    assertThat(normalizedFilename).isEmpty();
  }

  @Test
  public void normalizeFilenameShouldReturnEmptyOptionalForTooLongFilename() {
    UploadValidationConfig uploadValidationConfig = new UploadValidationConfig(MAX_SIZE, "txt");
    UploadMultipartParser uploadMultipartParser = new UploadMultipartParser(null, HTTP_CONFIGURATION, uploadValidationConfig);
    String longString = StringUtils.repeat("a", 241) + ".txt";
    Optional<String> normalizedFilename = uploadMultipartParser.normalizeFilename(longString);
    assertThat(normalizedFilename).isEmpty();
  }

}
