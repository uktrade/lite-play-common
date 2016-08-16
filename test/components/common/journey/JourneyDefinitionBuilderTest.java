package components.common.journey;

import static components.common.journey.JourneyDefinitionBuilder.moveTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;

public class JourneyDefinitionBuilderTest {

  //TODO Test to test build edge cases / errors
  private JourneyDefinitionBuilder BUILDER = new JourneyDefinitionBuilder();

  private JourneyStage STAGE_1;

  private JourneyStage UNKNOWN_STAGE = new JourneyStage("S5", "S5", "S5", () -> null);

  private final JourneyEvent EVENT_1 = new JourneyEvent("E1");

  @Before
  public void setup() {
    BUILDER = new JourneyDefinitionBuilder();
    STAGE_1 = BUILDER.defineStage("S1", "S1", () -> null);
  }


  @Test
  public void testUnregisteredStagesFail() {

    BUILDER
        .atStage(UNKNOWN_STAGE)
        .onEvent(EVENT_1)
        .then(moveTo(STAGE_1));

    assertThatThrownBy(() -> BUILDER.build("name", STAGE_1))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("not registered");

    //TODO test stages in a then() also fail
  }

}
