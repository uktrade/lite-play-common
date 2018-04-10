package components.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Stopwatch;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import play.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StatelessRedisDao {

  private final RedissonClient redissonClient;
  private final RedisKeyConfig keyConfig;

  public StatelessRedisDao(RedisKeyConfig keyConfig, RedissonClient redissonClient) {
    this.keyConfig = keyConfig;
    this.redissonClient = redissonClient;
  }

  public void writeString(String transactionId, String fieldName, String value) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      writeInternal(transactionId, fieldName, value);
    } finally {
      log("writeString", fieldName, stopwatch);
    }
  }

  public void writeObject(String transactionId, String fieldName, Object object) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      writeInternal(transactionId, fieldName, object);
    } catch (Exception exception) {
      throw new RuntimeException("Unable to write object", exception);
    } finally {
      log("writeObject", fieldName, stopwatch);
    }
  }

  public String readString(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      return (String) getMap(transactionId).get(fieldName);
    } finally {
      log("readString", fieldName, stopwatch);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> readObject(String transactionId, String fieldName, Class<T> clazz) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      T object = (T) getMap(transactionId).get(fieldName);
      return Optional.ofNullable(object);
    } catch (Exception exception) {
      throw new RuntimeException("Unable to read object", exception);
    } finally {
      log("readObject", fieldName, stopwatch);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> readObject(String transactionId, String fieldName, TypeReference<T> typeReference) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      T object = (T) getMap(transactionId).get(fieldName);
      return Optional.ofNullable(object);
    } catch (Exception exception) {
      throw new RuntimeException("Unable to read object", exception);
    } finally {
      log("readObject", fieldName, stopwatch);
    }
  }

  public void deleteString(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      getMap(transactionId).remove(fieldName);
    } finally {
      log("deleteString", fieldName, stopwatch);
    }
  }

  public boolean transactionExists(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      return getMap(transactionId).get(fieldName) != null;
    } finally {
      log("transactionExists", fieldName, stopwatch);
    }
  }

  public void refreshTtl(String transactionId) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      expireInternal(transactionId);
    } finally {
      log("refreshTtl", "n/a", stopwatch);
    }
  }

  private void writeInternal(String transactionId, String fieldName, Object value) {
    expireInternal(transactionId);
    getMap(transactionId).put(fieldName, value);
  }

  private void expireInternal(String transactionId) {
    getMap(transactionId).expire(keyConfig.getHashTtlSeconds(), TimeUnit.SECONDS);
  }

  private RMap<Object, Object> getMap(String transactionId) {
    return redissonClient.getMap(hashKey(transactionId));
  }

  private String hashKey(String transactionId) {
    return keyConfig.getKeyPrefix() + ":" + transactionId + ":" + keyConfig.getHashName();
  }

  private void log(String message, String fieldName, Stopwatch stopwatch) {
    if (Logger.isDebugEnabled()) {
      Logger.debug("{} of {} completed in {}", message, fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

}
