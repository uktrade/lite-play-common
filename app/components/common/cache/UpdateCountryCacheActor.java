package components.common.cache;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class UpdateCountryCacheActor extends UntypedAbstractActor {
  public static final String LOAD_MESSAGE = "load";
  public static final String STOP_MESSAGE = "stop";

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCountryCacheActor.class);

  private final CountryProvider countryProvider;
  private final int maxDelaySeconds;
  private final ExecutionContext executionContext;

  private int attempts;

  public UpdateCountryCacheActor(CountryProvider countryProvider, int maxDelaySeconds, ExecutionContext executionContext) {
    this.countryProvider = countryProvider;
    this.maxDelaySeconds = maxDelaySeconds;
    this.executionContext = executionContext;
  }

  @Override
  public void onReceive(Object message) {
    if (LOAD_MESSAGE.equals(message)) {
      ActorSystem system = getContext().getSystem();
      FiniteDuration delay = delayDuration(attempts);
      attempts++;
      LOGGER.info("Scheduling cache load for delay of {} second(s)", delay.toSeconds());
      system.scheduler().scheduleOnce(delay, () -> {
        try {
          countryProvider.loadCountries().toCompletableFuture().get();
          self().tell(STOP_MESSAGE, ActorRef.noSender());
          LOGGER.info("Successful cache load after {} attempts", attempts);
        } catch (Exception e) {
          LOGGER.error("Error during country provider load", e);
          self().tell(LOAD_MESSAGE, ActorRef.noSender());
        }
      }, executionContext);
    } else if (STOP_MESSAGE.equals(message)) {
      LOGGER.info("Stopped scheduling cache loads after {} attempts", attempts);
      attempts = 0;
    } else {
      LOGGER.error("Unknown message value {}", message);
    }
  }

  /**
   *  Each attempt will increase the duration by one second up to the maximum delay
   */
  private FiniteDuration delayDuration(int attempt) {
    return Duration.create(Math.min(attempt, maxDelaySeconds), TimeUnit.SECONDS);
  }
}