package components.common;

import com.google.inject.AbstractModule;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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

    bind(JedisPool.class).toInstance(createJedisPool());
  }

  private JedisPool createJedisPool() {
    //TODO - pool configuration
    System.out.println("RPWD '" + configuration.getString("redis.password") + "'");

    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(50);
    poolConfig.setMaxIdle(5);
    poolConfig.setMinIdle(1);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);

    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    } };

    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException("Unable ton construct SSL context for Redis connection", e);
    }

    SSLParameters sslParameters = new SSLParameters();
    HostnameVerifier hostnameVerifier = (s, sslSession) -> true;

    return new JedisPool(poolConfig, configuration.getString("redis.host"), configuration.getInt("redis.port"),
        configuration.getInt("redis.timeout"), StringUtils.defaultIfBlank(configuration.getString("redis.password"), null),
        configuration.getInt("redis.database"), true, sslContext.getSocketFactory(), sslParameters, hostnameVerifier);
  }

}
