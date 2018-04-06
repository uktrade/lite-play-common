package components.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RedissonClient;
import play.Logger;
import play.libs.Json;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StatelessRedisDao {

  private static final ObjectMapper OBJECT_MAPPER = Json.newDefaultMapper();

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
      writeInternal(transactionId, fieldName, OBJECT_MAPPER.writeValueAsString(object));
    } catch (Exception exception) {
      throw new RuntimeException("Unable to write object", exception);
    } finally {
      log("writeObject", fieldName, stopwatch);
    }
  }

  public String readString(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      return readInternal(transactionId, fieldName);
    } finally {
      log("readString", fieldName, stopwatch);
    }
  }

  public <T> Optional<T> readObject(String transactionId, String fieldName, Class<T> clazz) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      String value = readInternal(transactionId, fieldName);
      if (StringUtils.isBlank(value)) {
        return Optional.empty();
      } else {
        return Optional.of(OBJECT_MAPPER.readValue(value, clazz));
      }
    } catch (Exception exception) {
      throw new RuntimeException("Unable to read object", exception);
    } finally {
      log("readObject", fieldName, stopwatch);
    }
  }

  public <T> Optional<T> readObject(String transactionId, String fieldName, TypeReference<T> typeReference) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      String value = readInternal(transactionId, fieldName);
      if (StringUtils.isBlank(value)) {
        return Optional.empty();
      } else {
        return Optional.of(OBJECT_MAPPER.readValue(value, typeReference));
      }
    } catch (Exception exception) {
      throw new RuntimeException("Unable to read object", exception);
    } finally {
      log("readObject", fieldName, stopwatch);
    }
  }

  public void deleteString(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      redissonClient.getMap(hashKey(transactionId)).remove(fieldName);
    } finally {
      log("deleteString", fieldName, stopwatch);
    }
  }

  public boolean transactionExists(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      return redissonClient.getMap(hashKey(transactionId)).get(fieldName) != null;
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

  private void writeInternal(String transactionId, String fieldName, String value) {
    expireInternal(transactionId);
    redissonClient.getMap(hashKey(transactionId)).put(fieldName, value);
  }

  private String readInternal(String transactionId, String fieldName) {
    return (String) redissonClient.getMap(hashKey(transactionId)).get(fieldName);
  }

  private void expireInternal(String transactionId) {
    redissonClient.getMap(hashKey(transactionId)).expire(keyConfig.getHashTtlSeconds(), TimeUnit.SECONDS);
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
