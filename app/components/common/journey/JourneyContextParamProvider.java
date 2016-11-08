package components.common.journey;

import components.common.state.ContextParamProvider;

public class JourneyContextParamProvider extends ContextParamProvider {

  public static final String JOURNEY_CONTEXT_PARAM_NAME = "ctx_journey";

  @Override
  public String getParamName() {
    return JOURNEY_CONTEXT_PARAM_NAME;
  }

}
