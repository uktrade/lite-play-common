package components.common.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Singleton
public class TransactionManager implements TransactionIdProvider {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);

  private final TransactionContextParamProvider transactionContextParamProvider;

  @Inject
  public TransactionManager(TransactionContextParamProvider transactionContextParamProvider) {
    this.transactionContextParamProvider = transactionContextParamProvider;
  }

  public static String generateTransactionId() {
    return UUID.randomUUID().toString();
  }

  public String createTransaction() {
    String newTransactionId = generateTransactionId();

    transactionContextParamProvider.updateParamValueOnContext(newTransactionId);

    LOGGER.info("Created transaction " + newTransactionId);

    return newTransactionId;
  }

  public void setTransaction(String newTransactionId) {
    transactionContextParamProvider.updateParamValueOnContext(newTransactionId);
  }

  public boolean isTransactionIdAvailable() {
    return StringUtils.isNoneBlank(transactionContextParamProvider.getParamValueFromContext());
  }

  @Override
  public String getTransactionId() {
    String transactionId = transactionContextParamProvider.getParamValueFromContext();

    if (StringUtils.isBlank(transactionId)) {
      throw new RuntimeException("Transaction ID is not available");
    } else {
      return transactionId;
    }
  }

}
