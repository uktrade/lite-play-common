package components.common.journey;

import components.common.state.ContextParamProvider;

public class JourneyContextParamProvider extends ContextParamProvider {

  @Override
  public String getParamName() {
    return "ctx_journey";
  }

}
