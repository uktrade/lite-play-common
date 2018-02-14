package components.common;

import com.google.inject.AbstractModule;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RedisGuiceModule extends AbstractModule {

  private Configuration configuration;

  public RedisGuiceModule(Environment environment, Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {

    bind(JedisPool.class).toInstance(createJedisPool(configuration));
  }

  private JedisPool createJedisPool(Configuration configuration) {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(configuration.getInt("redis.pool.maxTotal"));
    poolConfig.setMaxIdle(configuration.getInt("redis.pool.maxIdle"));
    poolConfig.setMinIdle(configuration.getInt("redis.pool.minIdle"));
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);

    if (configuration.getBoolean("redis.ssl")) {
      //Disable SSL certificate verification on the Redis connection (but still connect using SSL)
      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }};

      SSLContext sslContext;
      try {
        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        throw new RuntimeException("Unable to construct SSL context for Redis connection", e);
      }

      SSLParameters sslParameters = new SSLParameters();
      HostnameVerifier hostnameVerifier = (s, sslSession) -> true;

      return new JedisPool(poolConfig, configuration.getString("redis.host"), configuration.getInt("redis.port"),
          configuration.getInt("redis.timeout"), StringUtils.defaultIfBlank(configuration.getString("redis.password"), null),
          configuration.getInt("redis.database", Protocol.DEFAULT_DATABASE), true, sslContext.getSocketFactory(),
          sslParameters, hostnameVerifier);
    } else {
      return new JedisPool(poolConfig, configuration.getString("redis.host"), configuration.getInt("redis.port"),
          configuration.getInt("redis.timeout"), StringUtils.defaultIfBlank(configuration.getString("redis.password"), null),
          configuration.getInt("redis.database", Protocol.DEFAULT_DATABASE));
    }

  }

}
