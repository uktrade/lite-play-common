package components.common.cache;

import akka.actor.UntypedActor;
import com.google.inject.Inject;

public class UpdateCountryCacheActor extends UntypedActor {

  private final CountryProvider countryProvider;

  @Inject
  public UpdateCountryCacheActor(CountryProvider countryProvider) {
    this.countryProvider = countryProvider;
  }

  @Override
  public void onReceive(Object message) throws Exception {
      sender().tell(updateCache(message), self());
  }

  private Object updateCache(Object message) {
    countryProvider.loadCountries();
    return message;
  }
}