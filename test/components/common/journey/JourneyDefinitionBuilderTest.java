package components.common.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JourneyDefinitionBuilderTest {

  //TODO Test to test build edge cases / errors

  private enum SimpleEnum {
    V1, V2
  }

  private JourneyStage STAGE_1;
  private JourneyStage STAGE_2;
  private DecisionStage<Boolean> BOOLEAN_DECISION_STAGE;
  private DecisionStage<SimpleEnum> ENUM_DECISION_STAGE;

  private final JourneyStage UNKNOWN_STAGE_1 = new RenderedJourneyStage("US1", "US1", "US1", () -> null);
  private final JourneyStage UNKNOWN_STAGE_2 = new RenderedJourneyStage("US2", "US2", "US2", () -> null);
  private final DecisionStage<Boolean> UNKNOWN_DECISION_STAGE = new DecisionStage<>("UDS", () -> null, Function.identity());

  private final JourneyEvent EVENT_1 = new JourneyEvent("E1");

  private final ParameterisedJourneyEvent<Boolean> BOOLEAN_PARAM_EVENT = new ParameterisedJourneyEvent<>("BPE", Boolean.class);
  private final ParameterisedJourneyEvent<SimpleEnum> SIMPLE_ENUM_PARAM_EVENT = new ParameterisedJourneyEvent<>("SEPE", SimpleEnum.class);
  private final ParameterisedJourneyEvent<Object> OBJECT_PARAM_EVENT = new ParameterisedJourneyEvent<>("OPE", Object.class);

  private abstract class BaseStageBuilder extends JourneyDefinitionBuilder {
    public BaseStageBuilder() {
      STAGE_1 = defineStage("S1", "S1", () -> null);
      STAGE_2 = defineStage("S2", "S2", () -> null);
      BOOLEAN_DECISION_STAGE = defineDecisionStage("BDS", () -> null);
      ENUM_DECISION_STAGE = defineDecisionStage("EDS", () -> null);
      defineJourney("defaultJourney", STAGE_1);
    }
  }

  /*============ Stage registration validation ============*/

  @Test
  public void testUnregisteredOriginStagesFail() {
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(UNKNOWN_STAGE_1) //"origin" stage
            .onEvent(EVENT_1)
            .then(moveTo(STAGE_1));
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Unknown stage US1");
  }

  @Test
  public void testUnregisteredDestinationStagesFail() {
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(EVENT_1)
            .then(moveTo(UNKNOWN_STAGE_1)); //"destination" stage
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Stages are not registered: US1");
  }

  @Test
  public void testUnregisteredBranchDestinationStagesFail() {
    //A branch cannot go to unknown stages
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(BOOLEAN_PARAM_EVENT)
            .branch()
            .when(true, moveTo(UNKNOWN_STAGE_1))
            .otherwise(moveTo(UNKNOWN_STAGE_2));
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Stages are not registered: US1, US2");
  }

  @Test
  public void testUnregisteredOriginDecisionStagesFail() {
    //A transition cannot start at an unknown decision stage
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atDecisionStage(UNKNOWN_DECISION_STAGE) //"origin" stage
            .decide()
            .when(true, moveTo(STAGE_1))
            .when(false, moveTo(STAGE_2));
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Unknown stage UDS");
  }


  @Test
  public void testUnregisteredDestinationDecisionStagesFail() {
    //A transition cannot go to an unknown decision stage
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(EVENT_1)
            .then(moveTo(UNKNOWN_DECISION_STAGE)); //"destination" stage
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Stages are not registered: UDS");
  }

  @Test
  public void testUnregisteredDecisionDestinationStagesFail() {
    //A decision cannot go to unknown stages
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atDecisionStage(BOOLEAN_DECISION_STAGE) //"origin" stage
            .decide()
            .when(true, moveTo(UNKNOWN_STAGE_1))
            .otherwise(moveTo(UNKNOWN_STAGE_2));
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Stages are not registered: US1, US2");
  }

  @Test
  public void testDuplicateTransitionDefinitionFails() {
    //A decision cannot go to unknown stages
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(EVENT_1)
            .then(moveTo(STAGE_2));

        atStage(STAGE_1)
            .onEvent(EVENT_1) //duplicate definition of STAGE_1 + EVENT_1
            .then(moveTo(BOOLEAN_DECISION_STAGE));
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Transition for stage 'S1', event 'E1' has already been defined");
  }

  @Test
  public void testDuplicateStageRegistrationFails() {
    class TestBuilder extends JourneyDefinitionBuilder {
      @Override
      protected void journeys() {
        defineStage("S1", "S1", () -> null);
        defineStage("S1", "S1", () -> null);
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Stage named S1 is already registered");
  }

  @Test
  public void testDuplicateDecisionStageRegistrationFails() {
    class TestBuilder extends JourneyDefinitionBuilder {
      @Override
      protected void journeys() {
        defineDecisionStage("S1", () -> null);
        defineDecisionStage("S1", () -> null);
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Duplicate definition of decision stage with name S1");
  }

  /*============ Branch logic validation ============*/

  @Test
  public void testDuplicateEnumBranchConditionsFail() {
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(SIMPLE_ENUM_PARAM_EVENT)
            .branch()
            .when(SimpleEnum.V1, moveTo(STAGE_2))
            .when(SimpleEnum.V1, moveTo(STAGE_2)); //V1 branch deliberately duplicated
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Duplicate branch condition value");
  }

  @Test
  public void testDuplicateObjectBranchConditionsFail() {
    Object compareObject = new Object();
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atStage(STAGE_1)
            .onEvent(OBJECT_PARAM_EVENT)
            .branch()
            .when(compareObject, moveTo(STAGE_2))
            .when(compareObject, moveTo(STAGE_2)); //branch deliberately duplicated
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Duplicate branch condition value");
  }

  @Test
  public void testDuplicateEnumDecisionConditionsFail() {
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        atDecisionStage(ENUM_DECISION_STAGE)
            .decide()
            .when(SimpleEnum.V1, moveTo(STAGE_2))
            .when(SimpleEnum.V1, moveTo(STAGE_2)); //V1 branch deliberately duplicated
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Action for branch value V1 already defined");
  }

  /*============ Journey definition validation ============*/

  @Test
  public void testDuplicateJourneyDefinitionFails() {
    class TestBuilder extends BaseStageBuilder {      @Override
      protected void journeys() {
        defineJourney("J1", STAGE_1);
        defineJourney("J1", STAGE_1); //deliberately duplicated
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Journey J1 is already defined in this Builder");
  }

  @Test
  public void testDuplicateJourneyDefinitionForUnknownStageFails() {
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        defineJourney("J1", UNKNOWN_STAGE_1);
      }
    }

    assertThatThrownBy(() -> new TestBuilder().buildAll())
        .isInstanceOf(JourneyDefinitionException.class)
        .hasMessageContaining("Unknown stage US1");
  }

  /*============ Multiple journeys ============*/

  @Test
  public void testMultipleJourneyDefinitions() {
    class TestBuilder extends BaseStageBuilder {
      @Override
      protected void journeys() {
        defineJourney("J2", STAGE_2);
        defineJourney("J3", ENUM_DECISION_STAGE, BackLink.to(null, "backprompt"));
      }
    }

    Map<String, JourneyDefinition> definitions = new TestBuilder().buildAll()
        .stream().collect(Collectors.toMap(JourneyDefinition::getJourneyName, Function.identity()));

    assertThat(definitions).containsOnlyKeys("defaultJourney", "J2", "J3");

    assertEquals(STAGE_1.getInternalName(), definitions.get("defaultJourney").getStartStage().getInternalName());
    assertEquals(STAGE_2.getInternalName(), definitions.get("J2").getStartStage().getInternalName());
    assertEquals(ENUM_DECISION_STAGE.getInternalName(), definitions.get("J3").getStartStage().getInternalName());

    assertThat(definitions.get("J2").getExitBackLink()).isEmpty();
    assertThat(definitions.get("J3").getExitBackLink().map(BackLink::getPrompt)).isPresent().contains("backprompt");

  }
}
