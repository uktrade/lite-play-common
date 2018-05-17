package components.common.redis;

import com.google.inject.Inject;
import org.redisson.api.RedissonClient;
import play.cache.SyncCacheApi;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Allows Redisson to be used as a Play cache implementation.
 */
public class RedissonSyncCacheApi implements SyncCacheApi {

  private final RedissonClient redissonClient;
  private final String keyPrefix;

  @Inject
  public RedissonSyncCacheApi(RedissonClient redissonClient, String keyPrefix) {
    this.redissonClient = redissonClient;
    this.keyPrefix = keyPrefix;
  }

  @Override
  public <T> T get(String key) {
    return (T) readObject(key);
  }

  @Override
  public <T> T getOrElseUpdate(String key, Callable<T> block, int expiration) {
    return getOrElseUpdateInternal(key, block, expiration);
  }

  @Override
  public <T> T getOrElseUpdate(String key, Callable<T> block) {
    return getOrElseUpdateInternal(key, block, null);
  }

  private <T> T getOrElseUpdateInternal(String key, Callable<T> block, Integer expiration) {

    T object = (T) readObject(key);
    if (object == null) {
      try {
        object = block.call();
      } catch (Exception e) {
        throw new RuntimeException("Failed to retrieve cache value from Callable", e);
      }

      if (expiration != null) {
        set(key, object, expiration);
      } else {
        set(key, object);
      }
    }

    return object;
  }

  @Override
  public void set(String key, Object value, int expiration) {
    redissonClient.getBucket(prefixedKey(key)).set(value, expiration, TimeUnit.SECONDS);
  }

  @Override
  public void set(String key, Object value) {
    redissonClient.getBucket(prefixedKey(key)).set(value);
  }

  @Override
  public void remove(String key) {
    redissonClient.getBucket(prefixedKey(key)).delete();
  }

  private Object readObject(String key) {
    return redissonClient.getBucket(prefixedKey(key)).get();
  }

  private String prefixedKey(String key) {
    return keyPrefix + ":" + key;
  }
}
