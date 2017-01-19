package components.common.journey;

import static components.common.journey.JourneyDefinitionTest.EventEnum.EV1;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV2;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV3;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class JourneyDefinitionTest {

  private JourneyStage STAGE_1;
  private JourneyStage STAGE_2;
  private JourneyStage STAGE_3;
  private JourneyStage UNKNOWN_STAGE = new CallableJourneyStage("UNKNOWN", "UNKNOWN", "UNKNOWN", null);

  private final JourneyEvent EVENT_1 = new JourneyEvent("E1");
  private final JourneyEvent EVENT_2 = new JourneyEvent("E2");
  private final JourneyEvent EVENT_3 = new JourneyEvent("E3");
  private HttpExecutionContext hec = new HttpExecutionContext(Runnable::run);

  enum EventEnum {
    EV1, EV2, EV3;
  }

  /**
   * Class to use as an event param with fixed toString method to assert comparisons are done on .equals() and not .toString()
   */
  private static class EventClass {
    int id;

    public EventClass(int id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      return this == o || o instanceof EventClass && id == ((EventClass) o).id;
    }

    @Override
    public int hashCode() {
      return id;
    }

    @Override
    public String toString() {
      return "FIXED_TOSTRING";
    }
  }

  private final ParameterisedJourneyEvent<EventEnum> ENUM_PARAM_EVENT = new ParameterisedJourneyEvent<>("PEE", EventEnum.class);
  private final ParameterisedJourneyEvent<EventClass> CLASS_PARAM_EVENT = new ParameterisedJourneyEvent<>("PCE", EventClass.class);
  private final ParameterisedJourneyEvent<String> STRING_PARAM_EVENT = new ParameterisedJourneyEvent<>("PSE", String.class);
  private final ParameterisedJourneyEvent<Boolean> BOOLEAN_PARAM_EVENT = new ParameterisedJourneyEvent<>("PBE", Boolean.class);
  private final ParameterisedJourneyEvent<Integer> INT_PARAM_EVENT = new ParameterisedJourneyEvent<>("PIE", Integer.class);

  //Builder which defines stages for tests to subclass with event definitions
  private abstract class BaseStageBuilder extends JourneyDefinitionBuilder {
    BaseStageBuilder() {
      STAGE_1 = defineStage("S1", "S1", () -> null);
      STAGE_2 = defineStage("S2", "S2", () -> null);
      STAGE_3 = defineStage("S3", "S3", () -> null);
    }
  }

  @Test
  public void testSimpleJourney() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {

        atStage(STAGE_1)
            .onEvent(EVENT_1)
            .then(moveTo(STAGE_2));

        atStage(STAGE_2)
            .onEvent(EVENT_2)
            .then(moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition =  new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.startJourney(hec).getImmediateResult();
    assertEquals(STAGE_1, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), EVENT_1).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_2.getHash(), EVENT_2).getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());

    //No transition for event
    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_1.getHash(), EVENT_3))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("No transition defined for stage 'S1', event 'E3'");

    //No transition for stage
    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_3.getHash(), EVENT_1))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("No transition defined for stage 'S3', event 'E1'");

    //Stage not defined
    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, UNKNOWN_STAGE.getHash(), EVENT_1))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Stage 'UNKNOWN' is not defined");

  }

  @Test
  public void testNullValidation() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(ENUM_PARAM_EVENT)
            .branchWith(e -> null)
            .when(EV1, moveTo(STAGE_2))
            .when(EV2, moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    //Event argument is null
    assertThatThrownBy(() ->  journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, null))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Event argument cannot be null");

    //Converter function produces null
    assertThatThrownBy(() ->  journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Transition argument cannot be null");
  }

  @Test
  public void testEnumParamBranch() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(ENUM_PARAM_EVENT)
            .branch()
            .when(EV1, moveTo(STAGE_2))
            .when(EV2, moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV2).getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());

    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_1.getHash(),  ENUM_PARAM_EVENT, EV3))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Branch not matched with argument EV3");

  }

  @Test
  public void testClassParamBranch() throws ExecutionException, InterruptedException {

    EventClass o1 = new EventClass(1);
    EventClass o2 = new EventClass(2);

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(CLASS_PARAM_EVENT)
            .branch()
            .when(o1, moveTo(STAGE_2))
            .when(o2, moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), CLASS_PARAM_EVENT, o1).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), CLASS_PARAM_EVENT, o2).getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());

    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_1.getHash(), CLASS_PARAM_EVENT, new EventClass(3)).getImmediateResult())
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Branch not matched with argument FIXED_TOSTRING");
  }

  @Test
  public void testStringParamBranch() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(STRING_PARAM_EVENT)
            .branch()
            .when("S2", moveTo(STAGE_2))
            .when("S3", moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), STRING_PARAM_EVENT, "S2").getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), STRING_PARAM_EVENT, "S3").getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());

    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_1.getHash(), STRING_PARAM_EVENT, "UNKNOWN"))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Branch not matched with argument UNKNOWN");
  }

  @Test
  public void testBooleanParamBranch() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(BOOLEAN_PARAM_EVENT)
            .branch()
            .when(true, moveTo(STAGE_2));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), BOOLEAN_PARAM_EVENT, true).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_1.getHash(), BOOLEAN_PARAM_EVENT, false))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Branch not matched with argument false");
  }

  @Test
  public void testIntParamBranch() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(INT_PARAM_EVENT)
            .branch()
            .when(2, moveTo(STAGE_2))
            .when(3, moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), INT_PARAM_EVENT, 2).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), INT_PARAM_EVENT, 3).getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());

    assertThatThrownBy(() -> journeyDefinition.fireEvent(hec, STAGE_1.getHash(), INT_PARAM_EVENT, 4))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("Branch not matched with argument 4");
  }

  @Test
  public void testOtherwiseBranch() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(ENUM_PARAM_EVENT)
            .branch()
            .when(EV1, moveTo(STAGE_2))
            .otherwise(moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV2).getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());
  }

  @Test
  public void testParamEventWorksWithoutBranch() throws ExecutionException, InterruptedException {

    //Using a ParameterisedEvent but not doing a branch (all param values do the same thing)
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(ENUM_PARAM_EVENT)
            .then(moveTo(STAGE_2));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV2).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());
  }

  @Test
  public void testParamBranchDefinitionWithConverter() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(ENUM_PARAM_EVENT)
            .branchWith(e -> "_" + e.toString())
            .when("_EV1", moveTo(STAGE_2))
            .when("_EV2", moveTo(STAGE_3));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1).getImmediateResult();
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), ENUM_PARAM_EVENT, EV2).getImmediateResult();
    assertEquals(STAGE_3, transitionResult.getNewStage());
  }

  @Test
  public void testDecisions() throws ExecutionException, InterruptedException {

    Decider<String> decider1 = () -> completedFuture("YES");
    Decider<EventEnum> decider2 = () -> completedFuture(EV1);

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {

        DecisionStage<String> decisionStage1 = defineDecisionStage("decision1", decider1);
        DecisionStage<EventEnum> decisionStage2 = defineDecisionStage("decision2", decider2);
        DecisionStage<Boolean> decisionStage3 = defineDecisionStage("decision3", decider2, e -> e == EV1);
        DecisionStage<Boolean> decisionStage4 = defineDecisionStage("decision4", decider2, e -> e != EV1);

        atDecisionStage(decisionStage1)
            .decide()
            .when("YES", moveTo(decisionStage2));

        atDecisionStage(decisionStage2)
            .decide()
            .when(EV1, moveTo(decisionStage3));

        atDecisionStage(decisionStage3)
            .decide()
            .when(true, moveTo(STAGE_1));

        defineJourney("default", decisionStage1);

        atStage(STAGE_1)
            .onEvent(EVENT_1)
            .then(moveTo(decisionStage4));

        //Tests "otherwise"
        atDecisionStage(decisionStage4)
            .decide()
            .when(true, moveTo(STAGE_1))
            .otherwise(moveTo(STAGE_2));
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();

    //Starting the journey should trigger decision chain and end up on STAGE_1
    EventResult eventResult = journeyDefinition.startJourney(hec);

    //Decision event results should not be immediate
    assertFalse(eventResult.isImmediate());

    JourneyStage initialStage = eventResult.getCompletableResult().toCompletableFuture().get().getNewStage();

    assertEquals(STAGE_1, initialStage);

    //Should pass through decisionStage4 to arrive at STAGE_2
    TransitionResult transitionResult = journeyDefinition.fireEvent(hec, STAGE_1.getHash(), EVENT_1)
        .getCompletableResult().toCompletableFuture().get();
    assertEquals(STAGE_2, transitionResult.getNewStage());
  }

  @Test
  public void testGraphViewOutput() throws ExecutionException, InterruptedException {

    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {

        DecisionStage<Boolean> decisionStage1 = defineDecisionStage("decision1", () -> completedFuture(true));
        DecisionStage<EventEnum> decisionStage2 = defineDecisionStage("decision2", () -> completedFuture(EV1));

        atStage(STAGE_1)
            .onEvent(EVENT_1)
            .then(moveTo(STAGE_2));

        atStage(STAGE_2)
            .onEvent(ENUM_PARAM_EVENT)
            .branch()
            .when(EV1, moveTo(STAGE_3))
            .when(EV2, moveTo(decisionStage1));

        atDecisionStage(decisionStage1)
            .decide()
            .when(true, moveTo(STAGE_3))
            .when(false, moveTo(STAGE_1));

        atDecisionStage(decisionStage2)
            .decide()
            .when(EV1, moveTo(STAGE_1))
            .when(EV2, moveTo(STAGE_2));

        defineJourney("default", STAGE_1);
      }
    }

    JourneyDefinition journeyDefinition = new TestBuilder().buildAll().iterator().next();
    Collection<GraphViewTransition> transitions = journeyDefinition.asGraphViewTransitions();

    assertThat(transitions).hasSize(7);

    Set<String> transitionStrings = transitions.stream()
        .map(e -> e.getStartStage().getInternalName() + " -> " + e.getEventName() + " -> " + e.getEndStage().getInternalName())
        .collect(Collectors.toSet());

    assertThat(transitionStrings).containsOnly(
        "S1 -> E1 -> S2",
        "S2 -> PEE -> S3",
        "S2 -> PEE -> decision1",
        "decision1 -> true -> S3",
        "decision1 -> false -> S1",
        "decision2 -> EV1 -> S1",
        "decision2 -> EV2 -> S2"
    );

    Set<GraphViewTransition> branchTransitions = transitions.stream()
        .filter(e -> e.getStartStage().getInternalName().equals(STAGE_2.getInternalName())).collect(Collectors.toSet());

    //Transitions from S2 should be marked as conditional as they represent branches
    assertThat(branchTransitions).extracting(GraphViewTransition::isConditional).allMatch(e -> e);
    assertThat(branchTransitions).extracting(GraphViewTransition::getConditionValue).containsOnly("EV1", "EV2");
  }

}