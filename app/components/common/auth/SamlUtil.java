package components.common.auth;

import com.google.inject.Provider;
import org.apache.commons.io.FileUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.core.io.Resource;
import org.pac4j.play.PlayWebContext;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import play.Application;
import play.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SamlUtil {

  public static Config buildConfig(Provider<Application> application,
                                   Configuration configuration,
                                   SpireAuthManager authManager,
                                   SessionStore<PlayWebContext> playCacheStore,
                                   HttpActionAdapter httpActionAdapter,
                                   Map<String, Authorizer> authorizers) {
    SAML2ClientConfiguration samlConfig = buildSaml2ClientConfiguration(application, configuration);

    //Custom SAML client which will store response attributes on the Saml Profile
    SAML2Client saml2Client = new SpireSAML2Client(samlConfig, authManager);

    Clients clients = new Clients(configuration.getString("saml.callbackUrl"), saml2Client);

    clients.setDefaultClient(saml2Client);
    saml2Client.setCallbackUrl(configuration.getString("saml.callbackUrl"));
    saml2Client.setIncludeClientNameInCallbackUrl(false);

    Config config = new Config(clients);

    config.setHttpActionAdapter(httpActionAdapter);

    config.setSessionStore(playCacheStore);

    config.setAuthorizers(authorizers);

    return config;
  }

  private static SAML2ClientConfiguration buildSaml2ClientConfiguration(Provider<Application> application, Configuration configuration) {
    SAML2ClientConfiguration samlConfig = new SAML2ClientConfiguration(getKeystoreResource(application),
        "lite-ogel-registration",
        "jks",
        "keypass",
        "keypass",
        getIdentityProviderMetadataResource(application, configuration));

    //Maximum permitted age of IdP response in seconds
    samlConfig.setMaximumAuthenticationLifetime(3600);

    //AKA Saml2:Issuer
    samlConfig.setServiceProviderEntityId(configuration.getString("saml.issuer"));
    samlConfig.setServiceProviderMetadataPath("");

    return samlConfig;
  }

  private static Resource getKeystoreResource(Provider<Application> application) {
    return new Resource() {
      @Override
      public boolean exists() {
        return true;
      }

      @Override
      public String getFilename() {
        return "keystore.jks";
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new FileInputStream(application.get().getFile("conf/saml/keystore.jks"));
      }
    };
  }

  private static Resource getIdentityProviderMetadataResource(Provider<Application> application, Configuration configuration) {
    return new Resource() {
      @Override
      public boolean exists() {
        return true;
      }

      @Override
      public String getFilename() {
        return "metadata.xml";
      }

      @Override
      public InputStream getInputStream() throws IOException {
        File file = application.get().getFile("conf/saml/template-metadata.xml");
        String fileToString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        String replaced = fileToString.replace("${samlLocation}", configuration.getString("saml.location"));
        return org.apache.commons.io.IOUtils.toInputStream(replaced, StandardCharsets.UTF_8);
      }
    };
  }

}
