package components.common.journey;

import static components.common.journey.JourneyDefinitionBuilder.moveTo;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV1;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV2;
import static components.common.journey.JourneyDefinitionTest.EventEnum.EV3;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class JourneyDefinitionTest {

  //TODO Test to test build edge cases / errors
  private JourneyDefinitionBuilder BUILDER = new JourneyDefinitionBuilder();

  private JourneyStage STAGE_1;
  private JourneyStage STAGE_2;
  private JourneyStage STAGE_3;
  private JourneyStage STAGE_4;
  private JourneyStage STAGE_5;

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

  @Before
  public void setup() {
    BUILDER = new JourneyDefinitionBuilder();

    STAGE_1 = BUILDER.defineStage("S1", "S1", () -> null);
    STAGE_2 = BUILDER.defineStage("S2", "S2", () -> null);
    STAGE_3 = BUILDER.defineStage("S3", "S3", () -> null);
    STAGE_4 = BUILDER.defineStage("S4", "S4", () -> null);
    STAGE_5 = BUILDER.defineStage("S5", "S5", () -> null);
  }

  @Test
  public void testBasicDefinition() {

    BUILDER
        .atStage(STAGE_1)
        .onEvent(EVENT_1)
        .then(moveTo(STAGE_2));

    BUILDER
        .atStage(STAGE_2)
        .onEvent(EVENT_2)
        .then(moveTo(STAGE_3));

    JourneyDefinition journeyDefinition = BUILDER.build("default", STAGE_1);

    TransitionResult transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), EVENT_1);

    assertEquals(STAGE_2, transitionResult.getNewStage());
  }

  @Test
  public void testParamBranchDefinition() {

    BUILDER
        .atStage(STAGE_1)
        .onEvent(ENUM_PARAM_EVENT)
        .branch()
          .when(EV1, moveTo(STAGE_2))
          .when(EV2, moveTo(STAGE_3))
          .otherwise(moveTo(STAGE_4));

    JourneyDefinition journeyDefinition = BUILDER.build("default", STAGE_1);

    TransitionResult transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1);
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), ENUM_PARAM_EVENT, EV2);
    assertEquals(STAGE_3, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), ENUM_PARAM_EVENT, EV3);
    assertEquals(STAGE_4, transitionResult.getNewStage());

  }

  @Test
  public void testParamBranchDefinition_withConverter() {

    BUILDER
        .atStage(STAGE_1)
        .onEvent(ENUM_PARAM_EVENT)
        .branchWith(e -> "_" + e.toString())
          .when("_EV1", moveTo(STAGE_2))
          .when("_EV2", moveTo(STAGE_3))
          .otherwise(moveTo(STAGE_4));

    JourneyDefinition journeyDefinition = BUILDER.build("default", STAGE_1);

    TransitionResult transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), ENUM_PARAM_EVENT, EV1);
    assertEquals(STAGE_2, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), ENUM_PARAM_EVENT, EV2);
    assertEquals(STAGE_3, transitionResult.getNewStage());

    transitionResult = journeyDefinition.fireEvent(STAGE_1.getHash(), ENUM_PARAM_EVENT, EV3);
    assertEquals(STAGE_4, transitionResult.getNewStage());

  }

  //TEST TODOs
  //A stage should be registered even if there is no event attached to it (i.e. destination stage only)


}