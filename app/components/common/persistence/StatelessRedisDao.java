package components.common.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
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
 * <p>
 *   Base class for a stateless Redis DAO which stores all transaction data in a single Redis hash.
 *   The methods defined in this class to access the hash's data, but they are also free to use the pool and perform
 *   arbitrary commands within Redis as required.
 * </p>
 *
 * <p>
 *   "Stateless" meaning the transaction ID must be provided for every method call as it is not inferred from the
 *   context as in {@link CommonRedisDao}. Compare {@link #writeString} to {@link CommonRedisDao#writeString} for the
 *   expected difference in method signature.
 * </p>
 *
 * @see CommonRedisDao
 */
public abstract class StatelessRedisDao {

  private final RedisKeyConfig keyConfig;
  private final JedisPool pool;

  private final ObjectMapper objectMapper = Json.newDefaultMapper();

  public StatelessRedisDao(RedisKeyConfig keyConfig, JedisPool pool) {
    this.keyConfig = keyConfig;
    this.pool = pool;
  }

  /**
   * Writes a simple string value to the given field in the transaction data hash. This method resets the hash's TTL.
   * @param fieldName Hash field to be written to.
   * @param value String value to write.
   */
  protected final void writeString(String transactionId, String fieldName, String value) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      write(transactionId, fieldName, value);
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
  protected final void writeObject(String transactionId, String fieldName, Object object) {

    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      String objectJson;
      try {
        objectJson = objectMapper.writeValueAsString(object);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialise object to JSON for Redis write", e);
      }

      write(transactionId, fieldName, objectJson);
    }
    finally {
      Logger.debug(String.format("Write of '%s' object completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  private void write(String transactionId, String fieldName, String objectJson) {
    try (Jedis jedis = pool.getResource()) {
      Transaction multi = jedis.multi();
      multi.hset(hashKey(transactionId), fieldName, objectJson);
      multi.expire(hashKey(transactionId), keyConfig.getHashTtlSeconds());
      multi.exec();
    }
  }

  /**
   * Reads a simple string value from the given field in the transaction data hash.
   * @param fieldName Field to read.
   * @return Field value, or null if not defined.
   */
  protected final String readString(String transactionId, String fieldName) {

    Stopwatch stopwatch = Stopwatch.createStarted();
    try (Jedis jedis = pool.getResource()) {
      return jedis.hget(hashKey(transactionId), fieldName);
    }
    finally {
      Logger.debug(String.format("Read of '%s' string completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Deletes a simple string value from the given field in the transaction data hash.
   * @param fieldName Field to read.
   */
  protected final void deleteString(String transactionId, String fieldName) {

    Stopwatch stopwatch = Stopwatch.createStarted();
    try (Jedis jedis = pool.getResource()) {
      jedis.hdel(hashKey(transactionId), fieldName);
    }
    finally {
      Logger.debug(String.format("Delete of '%s' string completed in %d ms", fieldName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Reads an object stored as JSON from the given field in the transaction data hash.
   * @param fieldName Field to read.
   * @param objectClass Class of object being read.
   * @return The deserialised object, which may not exist if the field is empty.
   */
  protected final <T> Optional<T> readObject(String transactionId, String fieldName, Class<T> objectClass) {

    String objectJson = readField(transactionId, fieldName);

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
  protected final <T> Optional<T> readObject(String transactionId, String fieldName, TypeReference<T> typeReference) {

    String objectJson = readField(transactionId, fieldName);

    if (StringUtils.isBlank(objectJson)) {
      return Optional.empty();
    }

    try {
      return Optional.of(objectMapper.readValue(objectJson, typeReference));
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialise JSON object from Redis", e);
    }
  }

  private String readField(String transactionId, String fieldName) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      try (Jedis jedis = pool.getResource()) {
        return jedis.hget(hashKey(transactionId), fieldName);
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
  protected String hashKey(String transactionId) {
    return keyPrefix(transactionId) + ":" + keyConfig.getHashName();
  }

  /**
   * @return The prefix of any key to be used by this DAO, scoped to the current transaction.
   */
  protected String keyPrefix(String transactionId) {
    return keyConfig.getKeyPrefix() + ":" + transactionId;
  }

  /**
   * Refreshes the TTL of the key associated with this transaction, see {@link #hashKey(String)}
   */
  public void refreshTTL(String transactionId) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try (Jedis jedis = pool.getResource()) {
      int reply = jedis.expire(hashKey(transactionId), keyConfig.getHashTtlSeconds()).intValue();
      if (reply == 0) {
        Logger.error(String.format("Could not refresh TTL of '%s', key does not exist", hashKey(transactionId)));
      }
    }
    finally {
      Logger.debug(String.format("TTL of '%s' refresh completed in %d ms", hashKey(transactionId), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }
  }
}
