package components.common;

import com.google.inject.AbstractModule;
import components.common.persistence.RedisKeyConfig;
import components.common.transaction.ContextAccessorStrategy;
import components.common.transaction.StaticContextAccessorStrategy;
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
    bind(ContextAccessorStrategy.class).toInstance(new StaticContextAccessorStrategy());
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
}
