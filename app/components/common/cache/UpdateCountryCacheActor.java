package components.common.cache;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class UpdateCountryCacheActor extends UntypedAbstractActor {
  public static final String LOAD_MESSAGE = "load";
  public static final String STOP_MESSAGE = "stop";

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCountryCacheActor.class);

  private final CountryProvider countryProvider;
  private final int maxAttempts;
  private final int maxDelaySeconds;
  private final int[] lookupDelaySeconds;

  private int attempts;

  public UpdateCountryCacheActor(CountryProvider countryProvider, int maxAttempts, int maxDelaySeconds) {
    this.countryProvider = countryProvider;
    this.maxAttempts = maxAttempts;
    this.maxDelaySeconds = maxDelaySeconds;
    this.lookupDelaySeconds = initializeDelayLookup(maxAttempts);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    if (LOAD_MESSAGE.equals(message)) {
      attempts++;
      ActorSystem system = getContext().getSystem();
      FiniteDuration delay = delayDuration(attempts);
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
      }, system.dispatcher());
    } else if (STOP_MESSAGE.equals(message)) {
      LOGGER.info("Stopped scheduling cache loads after {} attempts", attempts);
      attempts = 0;
    } else {
      LOGGER.error("Unknown message value {}", message);
    }
  }

  private FiniteDuration delayDuration(int attempt) {
    if (attempt <= maxAttempts && attempt <= lookupDelaySeconds.length && attempt > 0 && lookupDelaySeconds[attempt -1] <= maxDelaySeconds) {
      return Duration.create(lookupDelaySeconds[attempt - 1], TimeUnit.SECONDS);
    } else {
      return Duration.create(maxDelaySeconds, TimeUnit.SECONDS);
    }
  }

  private int[] initializeDelayLookup(int total) {
    // Initialize array with fibonacci sequence of length total
    int[] delayLookup = new int[total];
    delayLookup[0] = 0;
    delayLookup[1] = 1;
    for(int i = 2; i < total; i++){
      delayLookup[i] = delayLookup[i - 1] + delayLookup[i - 2];
    }
    return delayLookup;
  }
}