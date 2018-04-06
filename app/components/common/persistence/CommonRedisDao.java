package components.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import components.common.transaction.TransactionIdProvider;
import org.redisson.api.RedissonClient;

import java.util.Optional;

public class CommonRedisDao {

  private final StatelessRedisDao statelessRedisDao;
  private final TransactionIdProvider transactionIdProvider;

  public CommonRedisDao(RedisKeyConfig keyConfig, RedissonClient redissonClient,
                        TransactionIdProvider transactionIdProvider) {
    this.statelessRedisDao = new StatelessRedisDao(keyConfig, redissonClient);
    this.transactionIdProvider = transactionIdProvider;
  }

  public final void writeString(String fieldName, String value) {
    statelessRedisDao.writeString(transactionId(), fieldName, value);
  }

  public final void writeObject(String fieldName, Object object) {
    statelessRedisDao.writeObject(transactionId(), fieldName, object);
  }

  public final String readString(String fieldName) {
    return statelessRedisDao.readString(transactionId(), fieldName);
  }

  public final void deleteString(String fieldName) {
    statelessRedisDao.deleteString(transactionId(), fieldName);
  }

  public final <T> Optional<T> readObject(String fieldName, Class<T> objectClass) {
    return statelessRedisDao.readObject(transactionId(), fieldName, objectClass);
  }

  public final <T> Optional<T> readObject(String fieldName, TypeReference<T> typeReference) {
    return statelessRedisDao.readObject(transactionId(), fieldName, typeReference);
  }

  public boolean transactionExists(String transactionId, String fieldName) {
    return statelessRedisDao.transactionExists(transactionId, fieldName);
  }

  public void refreshTTL() {
    statelessRedisDao.refreshTtl(transactionId());
  }

  private String transactionId() {
    return transactionIdProvider.getTransactionId();
  }

}
