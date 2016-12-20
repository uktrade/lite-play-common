package components.common.journey;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The result of firing a journey event. A result is considered "immediate" if it does not require any decisions to be
 * made. If a result is not immediate it must be processed asynchronously using the completable result.
 */
class EventResult {

  private final CompletionStage<TransitionResult> completableResult;

  private final TransitionResult immediateResult;

  public EventResult(CompletionStage<TransitionResult> completableResult) {
    this.completableResult = completableResult;
    immediateResult = null;
  }

  public EventResult(TransitionResult immediateResult) {
    this.immediateResult = immediateResult;
    completableResult = null;
  }

  /**
   * @return True if this event completed immediately, without any decisions.
   */
  public boolean isImmediate() {
    return immediateResult != null;
  }

  /**
   * @return A CompletionStage which will be able to retrieve the transition result for this event. If this event result
   * is immediate, this method returns a CompletedFuture containing the result.
   */
  public CompletionStage<TransitionResult> getCompletableResult() {
    return completableResult != null ? completableResult : CompletableFuture.completedFuture(immediateResult);
  }

  /**
   * @return The transition result for this event, if {@link #isImmediate} is true (null otherwise).
   */
  public TransitionResult getImmediateResult() {
    return immediateResult;
  }
}
