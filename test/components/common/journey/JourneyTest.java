package components.common.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class JourneyTest {

  private Journey baseJourney;

  @Before
  public void setup() {
    baseJourney = Journey.fromString("name~stage1-stage2-stage3");
  }

  @Test
  public void testStringParse() {
    assertThat(baseJourney.getHistoryQueue()).containsExactly("stage1", "stage2", "stage3");
    assertEquals("name", baseJourney.getJourneyName());
  }

  @Test
  public void testStagePush() {
    baseJourney.pushStage("stage4");
    assertThat(baseJourney.getHistoryQueue()).containsExactly("stage1", "stage2", "stage3", "stage4");
  }

  @Test
  public void testStagePop() {
    baseJourney.popBackToStage("stage2");
    assertThat(baseJourney.getHistoryQueue()).containsExactly("stage1", "stage2");

    setup();

    //If the stage is not in the journey, all stages should be popped and the new stage pushed
    baseJourney.popBackToStage("stage4");
    assertThat(baseJourney.getHistoryQueue()).containsExactly("stage4");

    setup();

    //Should ignore the current stage in the case that it's the target stage for a pop
    baseJourney.pushStage("stage2");
    baseJourney.popBackToStage("stage2");
    assertThat(baseJourney.getHistoryQueue()).containsExactly("stage1", "stage2");
  }

}
