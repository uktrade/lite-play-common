package components.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import components.common.journey.JourneyContextParamProvider;
import components.common.persistence.RedisKeyConfig;
import components.common.state.ContextParamManager;
import components.common.transaction.TransactionContextParamProvider;
import play.Configuration;
import play.Environment;

public class CommonGuiceModule extends AbstractModule {

  public CommonGuiceModule() {
  }

  @Override
  protected void configure() {
  }

  @Provides
  public ContextParamManager provideContextParamManager() {
    return new ContextParamManager(new JourneyContextParamProvider(), new TransactionContextParamProvider());
  }
}
