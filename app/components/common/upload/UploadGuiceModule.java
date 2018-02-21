package components.common.upload;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;

public class UploadGuiceModule extends AbstractModule {

  private final Configuration configuration;

  public UploadGuiceModule(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    // S3
    String region = configuration.getString("aws.region");
    AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
        .withCredentials(getAwsCredentialsProvider())
        .withRegion(region)
        .build();
    bind(AmazonS3.class).toInstance(amazonS3);
    bindConstant().annotatedWith(Names.named("awsBucketName")).to(configuration.getString("aws.bucketName"));
    // FileService
    bind(FileService.class).to(FileServiceImpl.class);
    // VirusCheckerClient
    bindConstant().annotatedWith(Names.named("virusServiceAddress")).to(configuration.getString("virusService.address"));
    bindConstant().annotatedWith(Names.named("virusServiceTimeout")).to(configuration.getString("virusService.timeout"));
    bindConstant().annotatedWith(Names.named("virusServiceCredentials")).to(configuration.getString("virusService.credentials"));
  }

  @Provides
  UploadValidationConfig provideUploadValidationConfig() {
    long maxSize = configuration.getLong("upload.validation.maxSize");
    String disallowedExtensions = configuration.getString("upload.validation.disallowedExtensions");
    return new UploadValidationConfig(maxSize, disallowedExtensions);
  }

  private AWSCredentialsProvider getAwsCredentialsProvider() {
    String profileName = configuration.getString("aws.credentials.profileName");
    String accessKey = configuration.getString("aws.credentials.accessKey");
    String secretKey = configuration.getString("aws.credentials.secretKey");
    if (StringUtils.isNoneBlank(profileName)) {
      return new ProfileCredentialsProvider(profileName);
    } else if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
      throw new RuntimeException("accessKey and secretKey must both be specified if no profile name is specified");
    } else {
      return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }
  }

}
