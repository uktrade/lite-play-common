package components.common.journey;

import java.util.concurrent.CompletionStage;

/**
 * A component of a JourneyDefinition which is used by decision stages to decide how a journey should be branched.
 * Deciders should be injected into the JourneyDefinitionBuilders where they are required. The <tt>decide</tt> method
 * may read stateful information (e.g. from a DAO), but it must NOT change any state. <br><br>
 *
 * Decisions are processed asynchronously as part of a CompletionStage chain.
 *
 * @param <T> Result type of the decision. Typically this should be an Enum or similar for code readability, although
 *           any object type with a sensible toString() method can be used.
 */
@FunctionalInterface
public interface Decider<T> {

  /**
   * Performs arbitrary actions in order to produce a decision result. Typically a decision implementation will need to
   * do some asynchronous work such as a web service request based on a DAO value.
   * @return A CompletionStage which can be resolved into a result object. This may be chained together with other
   * CompletionStages in order to complete a journey transition.
   */
  CompletionStage<T> decide();

}
