package components.common.journey;

import static components.common.journey.JourneyDefinitionBuilder.moveTo;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV1;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV2;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV3;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JourneyDefinitionTest {

  //TODO JourneyDefinitionBuilderTest to test build edge cases / errors

  private final JourneyStage STAGE_1 = new JourneyStage("S1", "S1", () -> null);
  private final JourneyStage STAGE_2 = new JourneyStage("S2", "S2", () -> null);
  private final JourneyStage STAGE_3 = new JourneyStage("S3", "S3", () -> null);
  private final JourneyStage STAGE_4 = new JourneyStage("S4", "S4", () -> null);
  private final JourneyStage STAGE_5 = new JourneyStage("S5", "S5", () -> null);

  private final JourneyEvent EVENT_1 = new JourneyEvent("E1");
  private final JourneyEvent EVENT_2 = new JourneyEvent("E2");
  private final JourneyEvent EVENT_3 = new JourneyEvent("E3");

  enum EventEnum {
    EV1, EV2, EV3;
  }

  private final ParameterisedJourneyEvent<EventEnum> ENUM_PARAM_EVENT = new ParameterisedJourneyEvent<>("PEE", EventEnum.class);
  private final ParameterisedJourneyEvent<String> STRING_PARAM_EVENT = new ParameterisedJourneyEvent<>("PSE", String.class);
  private final ParameterisedJourneyEvent<Boolean> BOOLEAN_PARAM_EVENT = new ParameterisedJourneyEvent<>("PBE", Boolean.class);
  private final ParameterisedJourneyEvent<Integer> INT_PARAM_EVENT = new ParameterisedJourneyEvent<>("PIE", Integer.class);

  @Test
  public void testBasicDefinition() {

    JourneyDefinitionBuilder journeyDefinitionBuilder = new JourneyDefinitionBuilder();

    journeyDefinitionBuilder
        .atStage(STAGE_1)
        .onEvent(EVENT_1)
        .then(moveTo(STAGE_2));

    journeyDefinitionBuilder
        .atStage(STAGE_2)
        .onEvent(EVENT_2)
        .then(moveTo(STAGE_3));

    JourneyDefinition journeyDefinition = journeyDefinitionBuilder.build("default", STAGE_1);

    TransitionResult transitionResult = journeyDefinition.fireEvent("S1", EVENT_1);

    assertEquals(STAGE_2, transitionResult.getNewStage());

    //transitionResult = journeyDefinition.fireEvent("S2", EVENT_1); //assert not found

  }

  @Test
  public void testParamBranchDefinition() {

    JourneyDefinitionBuilder journeyDefinitionBuilder = new JourneyDefinitionBuilder();

    journeyDefinitionBuilder
        .atStage(STAGE_1)
        .onEvent(ENUM_PARAM_EVENT)
        .branch()
          .when(EV1, moveTo(STAGE_2))
          .when(EV2, moveTo(STAGE_3))
          .otherwise(moveTo(STAGE_4));

    JourneyDefinition journeyDefinition = journeyDefinitionBuilder.build("default", STAGE_1);

    TransitionResult transitionResult = journeyDefinition.fireEvent("S1", ENUM_PARAM_EVENT, EV1);
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent("S1", ENUM_PARAM_EVENT, EV2);
    assertEquals(STAGE_3, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent("S1", ENUM_PARAM_EVENT, EV3);
    assertEquals(STAGE_4, transitionResult.getNewStage());

  }

  @Test
  public void testParamBranchDefinition_withConverter() {

    JourneyDefinitionBuilder journeyDefinitionBuilder = new JourneyDefinitionBuilder();

    journeyDefinitionBuilder
        .atStage(STAGE_1)
        .onEvent(ENUM_PARAM_EVENT)
        .branchWith(e -> "_" + e.toString())
          .when("_EV1", moveTo(STAGE_2))
          .when("_EV2", moveTo(STAGE_3))
          .otherwise(moveTo(STAGE_4));

    JourneyDefinition journeyDefinition = journeyDefinitionBuilder.build("default", STAGE_1);

    TransitionResult transitionResult = journeyDefinition.fireEvent("S1", ENUM_PARAM_EVENT, EV1);
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent("S1", ENUM_PARAM_EVENT, EV2);
    assertEquals(STAGE_3, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent("S1", ENUM_PARAM_EVENT, EV3);
    assertEquals(STAGE_4, transitionResult.getNewStage());

  }

  //TEST TODOs
  //A stage should be registered even if there is no event attached to it (i.e. destination stage only)


}