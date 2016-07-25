package components.common.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import play.Logger;

import java.util.UUID;

@Singleton
public class TransactionManager {

  private static final String TRANSACTION_ID_SESSION_ATTR = "transactionId";

  private final ContextAccessorStrategy contextAccessor;

  @Inject
  public TransactionManager(ContextAccessorStrategy contextAccessor) {
    this.contextAccessor = contextAccessor;
  }

  public String createTransaction() {

    String newTransactionId = UUID.randomUUID().toString();

    contextAccessor.getCurrentContext().session().put(TRANSACTION_ID_SESSION_ATTR, newTransactionId);

    Logger.info("Created transaction " + newTransactionId);

    return newTransactionId;
  }

  public String getTransactionId() {
    return contextAccessor.getCurrentContext().session().get(TRANSACTION_ID_SESSION_ATTR);
  }


}
