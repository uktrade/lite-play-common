package components.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import components.common.journey.JourneyContextParamProvider;
import components.common.persistence.RedisKeyConfig;
import components.common.state.ContextParamManager;
import components.common.transaction.TransactionContextParamProvider;
import play.Configuration;

public class CommonGuiceModule extends AbstractModule {

  private final Configuration configuration;

  public CommonGuiceModule(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(RedisKeyConfig.class).toInstance(createRedisKeyConfig());
  }

  @Provides
  public ContextParamManager provideContextParamManager() {
    return new ContextParamManager(new JourneyContextParamProvider(), new TransactionContextParamProvider());
  }


  private RedisKeyConfig createRedisKeyConfig() {
    return new RedisKeyConfig(configuration.getString("redis.keyPrefix"), configuration.getString("redis.hash.name"),
        configuration.getInt("redis.hash.ttlSeconds"));
  }
}
