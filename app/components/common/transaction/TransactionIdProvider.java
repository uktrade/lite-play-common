package components.common.transaction;

@FunctionalInterface
public interface TransactionIdProvider {

  String getTransactionId();

}
