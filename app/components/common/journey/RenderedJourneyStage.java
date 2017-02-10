package components.common.journey;

import play.mvc.Call;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class RenderedJourneyStage extends JourneyStage {

  private final Supplier<CompletionStage<Result>> formRenderSupplier;

  RenderedJourneyStage(String hash, String internalName, String backLinkPrompt,
                       Supplier<CompletionStage<Result>> formRenderSupplier) {
    super(hash, internalName, backLinkPrompt);
    this.formRenderSupplier = formRenderSupplier;
  }

  @Override
  public boolean isRendered() {
    return true;
  }

  @Override
  public Supplier<CompletionStage<Result>> getFormRenderSupplier() {
    return formRenderSupplier;
  }

  @Override
  public boolean isCallable() {
    return false;
  }

  @Override
  public Call getEntryCall() {
    throw new JourneyException("Stage " + getInternalName() + " is not callable");
  }
}
