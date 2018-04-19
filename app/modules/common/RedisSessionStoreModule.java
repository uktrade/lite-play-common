package modules.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import components.common.redis.RedissonSyncCacheApi;
import org.redisson.api.RedissonClient;
import play.Environment;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;

/**
 * Enable this module to use a Redisson-backed cache to store Pac4j sessions.
 */
public class RedisSessionStoreModule extends AbstractModule {

  private final Config config;

  public RedisSessionStoreModule(Environment environment, Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(new RedissonGuiceModule(config));
  }

  @Singleton
  @Provides
  @NamedCache("pac4j-session-store")
  public SyncCacheApi provideRedisCache(RedissonClient redissonClient) {
    return new RedissonSyncCacheApi(redissonClient, config.getString("pac4j.sessionStoreKeyPrefix"));
  }
}
