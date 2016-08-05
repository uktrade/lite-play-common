package components.common.journey;

import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class JourneyStage {

  private final String mnemonic;
  private final String displayName;
  private final Supplier<CompletionStage<Result>> formRenderSupplier;

  public JourneyStage(String mnemonic, String displayName, Supplier<CompletionStage<Result>> formRenderSupplier) {
    this.mnemonic = mnemonic;
    this.displayName = displayName;
    this.formRenderSupplier = formRenderSupplier;

    //Stage mnem are serialised to a CSV list, so they cannot contain commas
    if(mnemonic.contains(",")) {
      throw new RuntimeException("Stage mnemonic cannot contain comma character");
    }
  }

  public Supplier<CompletionStage<Result>> getFormRenderSupplier() {
    return formRenderSupplier;
  }

  public String getMnemonic() {
    return mnemonic;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return "stage '" + mnemonic +  "'";
  }
}
