package components.common.cache;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class UpdateCountryCacheActor extends UntypedAbstractActor {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCountryCacheActor.class);

  private final CountryProvider countryProvider;
  private final int maxAttempts;
  private int attempts;

  public UpdateCountryCacheActor(CountryProvider countryProvider, int maxAttempts) {
    this.countryProvider = countryProvider;
    this.maxAttempts = maxAttempts;
  }

    @Override
  public void onReceive(Object message) throws Exception {
    if ("load".equals(message)) {
      if (attempts < maxAttempts) {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.create(attempts + 1, TimeUnit.SECONDS), () -> {
          try {
            countryProvider.loadCountries().toCompletableFuture().get();
          } catch (Exception e) {
            LOGGER.error("Error during country provider load", e);
            self().tell("load", ActorRef.noSender());
          }
        }, system.dispatcher());
        attempts++;
      } else {
        LOGGER.error("Failed to load country cache after {} attempts", attempts);
        attempts = 0;
      }
    }
  }
}