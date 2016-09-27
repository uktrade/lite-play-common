package components.common.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import play.Logger;

import java.util.UUID;

@Singleton
public class TransactionManager {

  private final TransactionContextParamProvider transactionContextParamProvider;

  @Inject
  public TransactionManager(TransactionContextParamProvider transactionContextParamProvider) {
    this.transactionContextParamProvider = transactionContextParamProvider;
  }

  public String createTransaction() {

    String newTransactionId = UUID.randomUUID().toString();

    return createNewTransaction(newTransactionId);
  }

  public String createTransaction(String newTransactionId) {

    return createNewTransaction(newTransactionId);
  }

  public String getTransactionId() {
    String transactionId = transactionContextParamProvider.getParamValueFromContext();

    if (StringUtils.isBlank(transactionId)) {
      throw new RuntimeException("Transaction ID is not available");
    }

    return transactionId;
  }

  private String createNewTransaction(String newTransactionId) {
    transactionContextParamProvider.updateParamValueOnContext(newTransactionId);

    Logger.info("Created transaction " + newTransactionId);

    return newTransactionId;

  }
}
