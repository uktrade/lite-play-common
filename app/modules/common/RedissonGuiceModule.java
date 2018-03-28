package modules.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.SingleServerConfig;
import play.Logger;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.CompletableFuture;

public class RedissonGuiceModule extends AbstractModule {

  private final Config config;

  public RedissonGuiceModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public RedissonClient provideRedissonClient(ApplicationLifecycle applicationLifecycle) {

    boolean useSsl = config.getBoolean("redis.ssl");
    String protocol = useSsl ? "rediss://" : "redis://"; //add additional "s" to protocol for SSL

    org.redisson.config.Config redissonConfig = new org.redisson.config.Config();
    SingleServerConfig singleServerConfig = redissonConfig.useSingleServer()
        .setAddress(protocol + config.getString("redis.host") + ":" + config.getString("redis.port"))
        .setPassword(StringUtils.defaultIfBlank(config.getString("redis.password"), null))
        .setDatabase(config.getInt("redis.database"))
        .setTimeout(config.getInt("redis.timeout"))
        .setDnsMonitoring(false)
        .setDnsMonitoringInterval(-1)
        .setConnectionMinimumIdleSize(config.getInt("redis.pool.minIdle"))
        .setConnectionPoolSize(config.getInt("redis.pool.maxTotal"));

    if (useSsl) {
      //Don't attempt to verify the SSL certificates
      singleServerConfig.setSslEnableEndpointIdentification(false);
    }

    RedissonClient redissonClient = Redisson.create(redissonConfig);

    //Needs to happen in dev mode to stop connections persisting across restarts
    applicationLifecycle.addStopHook(() -> {
      Logger.info("Shutdown Redisson client");
      redissonClient.shutdown();
      return CompletableFuture.completedFuture(null);
    });

    return redissonClient;
  }
}
