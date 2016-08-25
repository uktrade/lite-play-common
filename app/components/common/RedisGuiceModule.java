package components.common;

import com.google.inject.AbstractModule;
import components.common.persistence.RedisKeyConfig;
import play.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisGuiceModule extends AbstractModule {

  private Configuration configuration;

  public RedisGuiceModule(Configuration configuration) {
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

}
