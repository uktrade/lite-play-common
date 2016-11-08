package components.common.journey;

import play.mvc.Call;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public abstract class JourneyStage {

  private final String hash;
  private final String internalName;
  private final String displayName;

  protected JourneyStage(String hash, String internalName, String displayName) {
    this.hash = hash;
    this.internalName = internalName;
    this.displayName = displayName;

    //Stage hashes are serialised to a hyphen separated list, so they cannot contain hyphens
    if(hash.contains(JourneyManager.JOURNEY_STAGE_SEPARATOR_CHAR)) {
      throw new RuntimeException("Stage hash cannot contain separator character");
    }
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

  public abstract boolean isRendered();

  public abstract Supplier<CompletionStage<Result>> getFormRenderSupplier();

  public abstract boolean isCallable();

  public abstract Call getEntryCall();

  @Override
  public String toString() {
    return "stage '" + internalName +  "'";
  }
}
