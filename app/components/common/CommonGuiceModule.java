package components.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import components.common.journey.JourneyContextParamProvider;
import components.common.persistence.RedisKeyConfig;
import components.common.state.ContextParamManager;
import components.common.transaction.TransactionContextParamProvider;
import play.Configuration;
import play.Environment;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class CommonGuiceModule extends AbstractModule {

  private Environment environment;
  private Configuration configuration;

  public CommonGuiceModule(Environment environment, Configuration configuration) {
    this.environment = environment;
    this.configuration = configuration;
  }

  @Override
  protected void configure() {

    bind(JedisPool.class).toInstance(createJedisPool());
    bind(RedisKeyConfig.class).toInstance(createRedisKeyConfig());
  }

  private JedisPool createJedisPool() {
    //TODO - pool configuration
    return new JedisPool(new JedisPoolConfig(), configuration.getString("redis.host"), configuration.getInt("redis.port"),
        configuration.getInt("redis.timeout"));
  }

  private RedisKeyConfig createRedisKeyConfig() {
    return new RedisKeyConfig(configuration.getString("redis.keyPrefix"), configuration.getString("redis.hash.name"),
        configuration.getInt("redis.hash.ttlSeconds"));
  }

  @Provides
  public ContextParamManager provideContextParamManager() {
    return new ContextParamManager(new JourneyContextParamProvider(), new TransactionContextParamProvider());
  }
}
