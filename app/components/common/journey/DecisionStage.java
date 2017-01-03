package components.common.journey;

import java.util.function.Function;

public class DecisionStage<T> implements CommonStage {

  private final String internalName;
  private final Decider<?> decider;
  private final Function<?, T> converter;

  DecisionStage(String internalName, Decider<?> decider, Function<?, T> converter) {
    this.internalName = internalName;
    this.decider = decider;
    this.converter = converter;
  }

  @Override
  public String getInternalName() {
    return internalName;
  }

  Decider<?> getDecider() {
    return decider;
  }

  Function<?, T> getConverter() {
    return converter;
  }
}
