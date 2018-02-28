package components.common.upload;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

public class UploadGuiceModule extends AbstractModule {

  private final Config config;

  public UploadGuiceModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    // S3
    String region = config.getString("aws.region");
    AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
        .withCredentials(getAwsCredentialsProvider())
        .withRegion(region)
        .build();
    bind(AmazonS3.class).toInstance(amazonS3);
    bindConstant().annotatedWith(Names.named("awsBucketName")).to(config.getString("aws.bucketName"));
    // FileService
    bind(FileService.class).to(FileServiceImpl.class);
    // VirusCheckerClient
    bindConstant().annotatedWith(Names.named("virusServiceAddress")).to(config.getString("virusService.address"));
    bindConstant().annotatedWith(Names.named("virusServiceTimeout")).to(config.getString("virusService.timeout"));
    bindConstant().annotatedWith(Names.named("virusServiceCredentials")).to(config.getString("virusService.credentials"));
  }

  @Singleton
  @Provides
  UploadValidationConfig provideUploadValidationConfig() {
    long maxSize = config.getLong("upload.validation.maxSize");
    String allowedExtensions = config.getString("upload.validation.allowedExtensions");
    return new UploadValidationConfig(maxSize, allowedExtensions);
  }

  private AWSCredentialsProvider getAwsCredentialsProvider() {
    String profileName = config.getString("aws.credentials.profileName");
    String accessKey = config.getString("aws.credentials.accessKey");
    String secretKey = config.getString("aws.credentials.secretKey");
    if (StringUtils.isNoneBlank(profileName)) {
      return new ProfileCredentialsProvider(profileName);
    } else if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
      throw new RuntimeException("accessKey and secretKey must both be specified if no profile name is specified");
    } else {
      return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }
  }

}
