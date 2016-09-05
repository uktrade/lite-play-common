package components.common.journey;

import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class JourneyStage {

  private final String hash;
  private final String internalName;
  private final String displayName;
  private final Supplier<CompletionStage<Result>> formRenderSupplier;

  JourneyStage(String hash, String internalName, String displayName,
               Supplier<CompletionStage<Result>> formRenderSupplier) {
    this.hash = hash;
    this.internalName = internalName;
    this.displayName = displayName;
    this.formRenderSupplier = formRenderSupplier;

    //Stage hashes are serialised to a hyphen separated list, so they cannot contain hyphens
    if(hash.contains(JourneyManager.JOURNEY_STAGE_SEPARATOR_CHAR)) {
      throw new RuntimeException("Stage hash cannot contain separator character");
    }
  }

  public Supplier<CompletionStage<Result>> getFormRenderSupplier() {
    return formRenderSupplier;
  }

  public String getHash() {
    return hash;
  }

  public String getInternalName() {
    return internalName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return "stage '" + internalName +  "'";
  }
}
