package components.common;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import components.common.persistence.RedisKeyConfig;
import components.common.transaction.TransactionIdProvider;
import components.common.transaction.TransactionManager;

public class CommonGuiceModule extends AbstractModule {

  private final Config config;

  public CommonGuiceModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {

    //Dynamically bind hash configs based on contents of config file
    for (Config daoHashConfig : config.getConfigList("redis.daoHashes")) {
      bind(RedisKeyConfig.class)
          .annotatedWith(Names.named(daoHashConfig.getString("name")))
          .toInstance(createRedisKeyConfig(daoHashConfig));
    }

    //Provide a useful default binding for TransactionIdProvider
    bind(TransactionIdProvider.class).to(TransactionManager.class);
  }

  private RedisKeyConfig createRedisKeyConfig(Config hashConfig) {
    return new RedisKeyConfig(config.getString("redis.keyPrefix"),
        hashConfig.getString("hashName"),
        hashConfig.getInt("ttlSeconds"));
  }
}
