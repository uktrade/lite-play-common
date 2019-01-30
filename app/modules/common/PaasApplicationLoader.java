package modules.common;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryEnvironmentException;
import io.pivotal.labs.cfenv.CloudFoundryService;
import org.apache.commons.lang3.StringUtils;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

import java.util.Optional;

/**
 * ApplicationLoader which overloads the <tt>db.default.url</tt> config property to a value read from parsed VCAP_SERVICES
 * enviornment variable.
 */
public class PaasApplicationLoader extends GuiceApplicationLoader {

  private String getVcapJdbcUri() {
    CloudFoundryEnvironment cfEnvironment;
    try {
      //TODO system property lookup is an IntelliJ hack (sets system properties instead of env vars)
      cfEnvironment = new CloudFoundryEnvironment(name -> StringUtils.defaultIfBlank(System.getenv(name), System.getProperty(name)));
    } catch (CloudFoundryEnvironmentException e) {
      throw new RuntimeException("Failed to initialise CloudFoundryEnvironment", e);
    }

    Optional<CloudFoundryService> pgService = cfEnvironment.getServiceNames().stream()
        .map(cfEnvironment::getService)
        .filter(service -> service.getTags().contains("postgres")) //TODO make configurable
        .findFirst();

    if (pgService.isPresent()) {
      String credential = pgService.get().getCredentials().get("jdbcuri").toString();
      if (StringUtils.isNoneBlank(credential)) {
        return credential;
      } else {
        String message = String.format("jdbcuri not found in credentials (credentials available: %s)",
            pgService.get().getCredentials().keySet());
        throw new RuntimeException(message);
      }
    } else {
      throw new RuntimeException("No service with tag 'postgres' found in CloudFoundryEnvironment");
    }
  }

  @Override
  public GuiceApplicationBuilder builder(Context context) {

    String schema = context.initialConfig().getString("db.default.schema");

    return initialBuilder
        .in(context.environment())
        .configure("db.default.url", getVcapJdbcUri() + "&currentSchema=" + schema)
        .overrides(overrides(context));
  }
}
