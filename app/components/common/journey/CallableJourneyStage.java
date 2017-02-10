package components.common.journey;

import play.mvc.Call;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class CallableJourneyStage extends JourneyStage {

  private final Call entryCall;

  CallableJourneyStage(String hash, String internalName, String backLinkPrompt, Call entryCall) {
    super(hash, internalName, backLinkPrompt);
    this.entryCall = entryCall;
  }

  @Override
  public boolean isCallable() {
    return true;
  }

  @Override
  public Call getEntryCall() {
    return entryCall;
  }

  @Override
  public boolean isRendered() {
    return false;
  }

  @Override
  public Supplier<CompletionStage<Result>> getFormRenderSupplier() {
    throw new JourneyException("Stage " + getInternalName() + " is not rendered");
  }
}
