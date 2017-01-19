package components.common.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import components.common.state.ContextParamManager;
import org.junit.Before;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class JourneyManagerTest {

  private JourneyManager manager;

  //Note this member is updated by tests - these tests should not be run concurrently
  private String currentJourneyString;

  private static class DummySerialiser implements JourneySerialiser {
    @Override
    public String readJourneyString() {
      return "";
    }

    @Override
    public void writeJourneyString(String journeyString) {
    }
  }

  private static class DummyCall extends Call {

    private final String url;

    public DummyCall(String url) {
      this.url = url;
    }

    @Override
    public String url() {
      return url;
    }

    @Override
    public String method() {
      return "GET";
    }

    @Override
    public String fragment() {
      return "";
    }
  }

  JourneyStage stage1;
  JourneyStage stage2;
  JourneyStage stage3;

  private class Builder extends JourneyDefinitionBuilder {
    @Override
    protected void journeys() {

      stage1 = defineStage("S1", "S1", new DummyCall("url1"));
      stage2 = defineStage("S2", "S2", new DummyCall("url2"));
      stage3 = defineStage("S3", "S3", () -> CompletableFuture.completedFuture(new Result(200)));

      defineJourney("journey1", stage1);
      defineJourney("journey2", stage2);

      atStage(stage1)
          .onEvent(StandardEvents.NEXT)
          .then(moveTo(stage2));

      atStage(stage1)
          .onEvent(StandardEvents.CONFIRM)
          .branch()
          .when(true, moveTo(stage2))
          .when(false, moveTo(stage3));

      atStage(stage2)
          .onEvent(StandardEvents.CANCEL)
          .then(backTo(stage1));
    }
  }

  private class DummyContextParamProvider extends JourneyContextParamProvider {
    @Override
    public String getParamValueFromRequest() {
      return currentJourneyString;
    }

    @Override
    public String getParamValueFromContext() {
      return currentJourneyString;
    }

    @Override
    public void updateParamValueOnContext(String paramValue) {
      currentJourneyString = paramValue;
    }
  }

  @Before
  public void setup() {
    Http.Context context = new Http.Context(1L, null, null, new HashMap<>(), new HashMap<>(), new HashMap<>());
    Http.Context.current.set(context);

    manager = new JourneyManager(new DummySerialiser(), new ContextParamManager(), new DummyContextParamProvider(),
        Collections.singleton(new Builder()), new HttpExecutionContext(Runnable::run));

  }

  @Test
  public void testStartJourney() throws ExecutionException, InterruptedException {
    assertThat(manager.startJourney("journey1").toCompletableFuture().get().redirectLocation()).contains("url1");
    //Transition result should be written to context
    assertEquals("journey1~" + stage1.getHash(), currentJourneyString);

    assertThat(manager.startJourney("journey2").toCompletableFuture().get().redirectLocation()).contains("url2");
    assertEquals("journey2~" + stage2.getHash(), currentJourneyString);

  }

  @Test
  public void testTransition() throws ExecutionException, InterruptedException {
    manager.startJourney("journey1");
    CompletionStage<Result> result = manager.performTransition(StandardEvents.NEXT);
    assertThat(result.toCompletableFuture().get().redirectLocation()).contains("url2");
    assertEquals("journey1~" + stage1.getHash() + "-" + stage2.getHash(), currentJourneyString);

    //Test the backTo pops back
    result = manager.performTransition(StandardEvents.CANCEL);
    assertThat(result.toCompletableFuture().get().redirectLocation()).contains("url1");
    assertEquals("journey1~" + stage1.getHash(), currentJourneyString);

  }

  @Test
  public void testParamTransition() throws ExecutionException, InterruptedException {
    manager.startJourney("journey1");
    CompletionStage<Result> result = manager.performTransition(StandardEvents.CONFIRM, true);
    assertThat(result.toCompletableFuture().get().redirectLocation()).contains("url2");
    assertEquals("journey1~" + stage1.getHash() + "-" + stage2.getHash(), currentJourneyString);
  }

  @Test
  public void testUriForTransition() throws ExecutionException, InterruptedException {
    manager.startJourney("journey1");
    String uriForTransition = manager.uriForTransition(StandardEvents.NEXT);
    assertTrue(uriForTransition.startsWith("url2"));
  }

  @Test
  public void testUriForParamTransition() throws ExecutionException, InterruptedException {
    manager.startJourney("journey1");
    String uriForTransition = manager.uriForTransition(StandardEvents.CONFIRM, true);
    assertTrue(uriForTransition.startsWith("url2"));

    assertThatThrownBy(() -> manager.uriForTransition(StandardEvents.CONFIRM, false))
        .isInstanceOf(JourneyException.class)
        .hasMessageContaining("S3 is not callable");
  }
}
