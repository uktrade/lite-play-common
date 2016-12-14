package components.common.cache;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class CountryCacheScheduler {

  @Inject
  public CountryCacheScheduler(ActorSystem system, @Named("updateCountryCacheActor") ActorRef updateCountryCache) {

    system.scheduler().schedule(
      Duration.create(0, TimeUnit.MILLISECONDS), // Initial delay
      Duration.create(1, TimeUnit.DAYS), // Frequency
      updateCountryCache,
      "update country cache",
      system.dispatcher(),
      null);
  }

}
