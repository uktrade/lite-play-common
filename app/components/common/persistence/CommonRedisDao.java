package components.common.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import components.common.transaction.TransactionManager;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.libs.Json;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Base class for a Redis DAO which stores all transaction data in a single Redis hash. Subclasses should primarily use
 * the methods defined in this class to access the hash's data, but they are also free to use the pool and perform
 * arbitrary commands within Redis as required.
 */
public abstract class CommonRedisDao {

  private final RedisKeyConfig keyConfig;
  private final JedisPool pool;
  private final TransactionManager transactionManager;

  private final ObjectMapper objectMapper = Json.newDefaultMapper();

  public CommonRedisDao(RedisKeyConfig keyConfig, JedisPool pool, TransactionManager transactionManager) {
    this.keyConfig = keyConfig;
    this.pool = pool;
    this.transactionManager = transactionManager;
  }

  /**
   * Writes a simple string value to the given field in the transaction data hash. This method resets the hash's TTL.
   * @param fieldName Hash field to be written to.
   * @param value String value to write.
   */
  public final void writeString(String fieldName, String value) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      write(fieldName, value);
    }
    finally {
      Logger.debug(String.format("Write of '%s' string completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Writes an object to the given field in the transaction data hash. The object is serialised to JSON using a default
   * Jackson ObjectMapper. This method resets the hash's TTL.
   * @param fieldName Hash field to be written to.
   * @param object Object to write as JSON.
   */
  protected final void writeObject(String fieldName, Object object) {

    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      String objectJson;
      try {
        objectJson = objectMapper.writeValueAsString(object);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialise object to JSON for Redis write", e);
      }

      write(fieldName, objectJson);
    }
    finally {
      Logger.debug(String.format("Write of '%s' object completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  private void write(String fieldName, String objectJson) {
    try (Jedis jedis = pool.getResource()) {
      Transaction multi = jedis.multi();
      multi.hset(hashKey(), fieldName, objectJson);
      multi.expire(hashKey(), keyConfig.getHashTtlSeconds());
      multi.exec();
    }
  }

  /**
   * Reads a simple string value from the given field in the transaction data hash.
   * @param fieldName Field to read.
   * @return Field value, or null if not defined.
   */
  public final String readString(String fieldName) {

    Stopwatch stopwatch = Stopwatch.createStarted();
    try (Jedis jedis = pool.getResource()) {
      return jedis.hget(hashKey(), fieldName);
    }
    finally {
      Logger.debug(String.format("Read of '%s' string completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Reads an object stored as JSON from the given field in the transaction data hash.
   * @param fieldName Field to read.
   * @param objectClass Class of object being read.
   * @return The deserialised object, which may not exist if the field is empty.
   */
  protected final <T> Optional<T> readObject(String fieldName, Class<T> objectClass) {

    String objectJson = readField(fieldName);

    if (StringUtils.isBlank(objectJson)) {
      return Optional.empty();
    }

    try {
      return Optional.of(objectMapper.readValue(objectJson, objectClass));
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialise JSON object from Redis", e);
    }
  }

  /**
   * Reads an object stored as JSON from the given field in the transaction data hash.
   * @param fieldName Field to read.
   * @param typeReference Type reference of object being read.
   * @return The deserialised object, which may not exist if the field is empty.
   */
  protected final <T> Optional<T> readObject(String fieldName, TypeReference<T> typeReference) {

    String objectJson = readField(fieldName);

    if (StringUtils.isBlank(objectJson)) {
      return Optional.empty();
    }

    try {
      return Optional.of(objectMapper.readValue(objectJson, typeReference));
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialise JSON object from Redis", e);
    }
  }

  private String readField(String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      try (Jedis jedis = pool.getResource()) {
        return jedis.hget(hashKey(), fieldName);
      }
    }
    finally {
      Logger.debug(String.format("Read of '%s' object completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }


  /**
   * @return The current Jedis connection pool, for execution of arbitrary Redis commands.
   */
  protected final JedisPool getPool() {
    return pool;
  }

  /**
   * @return The full Redis key used to address the current transaction's data hash.
   */
  protected String hashKey() {
    return keyPrefix() + ":" + keyConfig.getHashName();
  }

  /**
   * @return The prefix of any key to be used by this DAO, scoped to the current transaction.
   */
  protected String keyPrefix() {
    return keyConfig.getKeyPrefix() + ":" + transactionManager.getTransactionId();
  }
}
