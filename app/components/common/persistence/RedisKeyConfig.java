package components.common.persistence;

public class RedisKeyConfig {

  private final String keyPrefix;
  private final String hashName;
  private final int hashTtlSeconds;

  public RedisKeyConfig(String keyPrefix, String hashName, int hashTtlSeconds) {
    this.keyPrefix = keyPrefix;
    this.hashName = hashName;
    this.hashTtlSeconds = hashTtlSeconds;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  public String getHashName() {
    return hashName;
  }

  public int getHashTtlSeconds() {
    return hashTtlSeconds;
  }
}

