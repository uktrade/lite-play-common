package components.common.transaction;

import components.common.state.ContextParamProvider;

public class TransactionContextParamProvider extends ContextParamProvider {

  @Override
  public String getParamName() {
    return "ctx_transaction";
  }
}
