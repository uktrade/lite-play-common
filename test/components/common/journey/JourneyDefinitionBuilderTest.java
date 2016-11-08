package components.common.journey;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class JourneyDefinitionBuilderTest {

  //TODO Test to test build edge cases / errors

  private JourneyStage STAGE_1;

  private JourneyStage UNKNOWN_STAGE = new RenderedJourneyStage("S5", "S5", "S5", () -> null);

  private final JourneyEvent EVENT_1 = new JourneyEvent("E1");

  private class BaseStageBuilder extends JourneyDefinitionBuilder {
    public BaseStageBuilder() {
      STAGE_1 = defineStage("S1", "S1", () -> null);
    }
  }


  @Test
  public void testUnregisteredStagesFail() {

    class TestBuilder extends BaseStageBuilder {
      public TestBuilder() {

        atStage(UNKNOWN_STAGE)
            .onEvent(EVENT_1)
            .then(moveTo(STAGE_1));

        assertThatThrownBy(() -> defineJourney("name", STAGE_1))
            .isInstanceOf(JourneyException.class)
            .hasMessageContaining("not registered");
      }
    }

    //TODO test stages in a then() also fail
  }

}
