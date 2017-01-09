package components.common.journey;

import play.mvc.Call;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * A stage in a journey which the user can navigate to and which renders a page, either via a URI redirection call
 * (preferred) or a render method. C.f. {@link DecisionStage}, which are transitional stages not accessed by a user.
 */
public abstract class JourneyStage implements CommonStage {

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

  /**
   * @return True if this stage is "rendered", i.e. not a URI redirect but instead a direct invocation of a render method.
   */
  public abstract boolean isRendered();

  /**
   * @return A proxy for a render method which renders a form, if this stage is "rendered".
   */
  public abstract Supplier<CompletionStage<Result>> getFormRenderSupplier();

  /**
   * @return True if this stage is "callable", i.e. can be called directly from a URI.
   */
  public abstract boolean isCallable();

  /**
   * @return The URI (Call) for this stage, if it is callable.
   */
  public abstract Call getEntryCall();

  @Override
  public String toString() {
    return "stage '" + internalName +  "'";
  }
}
