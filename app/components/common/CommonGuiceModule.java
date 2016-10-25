package components.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
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

    //Dynamically bind hash configs based on contents of config file
    for (Configuration daoHashConfig : configuration.getConfigList("redis.daoHashes")) {
      bind(RedisKeyConfig.class)
          .annotatedWith(Names.named(daoHashConfig.getString("name")))
          .toInstance(createRedisKeyConfig(daoHashConfig));
    }

  }

  @Provides
  public ContextParamManager provideContextParamManager() {
    return new ContextParamManager(new JourneyContextParamProvider(), new TransactionContextParamProvider());
  }


  private RedisKeyConfig createRedisKeyConfig(Configuration hashConfiguration) {
    return new RedisKeyConfig(configuration.getString("redis.keyPrefix"), hashConfiguration.getString("hashName"),
        hashConfiguration.getInt("ttlSeconds"));
  }
}
