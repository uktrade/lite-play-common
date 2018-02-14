package components.common.auth;

import org.apache.commons.io.IOUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.core.io.Resource;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.play.PlayWebContext;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import play.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SamlUtil {

  public static Config buildConfig(Configuration configuration,
                                   SpireAuthManager authManager,
                                   SessionStore<PlayWebContext> playCacheStore,
                                   HttpActionAdapter httpActionAdapter,
                                   Map<String, Authorizer> authorizers) {
    SAML2ClientConfiguration samlConfig = buildSaml2ClientConfiguration(configuration);

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

  private static SAML2ClientConfiguration buildSaml2ClientConfiguration(Configuration configuration) {
    SAML2ClientConfiguration samlConfig = new SAML2ClientConfiguration(CommonHelper.getResource("resource:saml/keystore.jks"),
        "lite-ogel-registration",
        "jks",
        "keypass",
        "keypass",
        getIdentityProviderMetadataResource(configuration));

    //Maximum permitted age of IdP response in seconds
    samlConfig.setMaximumAuthenticationLifetime(3600);

    //AKA Saml2:Issuer
    samlConfig.setServiceProviderEntityId(configuration.getString("saml.issuer"));
    samlConfig.setServiceProviderMetadataPath("");

    return samlConfig;
  }

  private static Resource getIdentityProviderMetadataResource(Configuration configuration) {
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
        String filePath = "resource:saml/template-metadata.xml";
        String fileToString = IOUtils.toString(CommonHelper.getResource(filePath).getInputStream(), StandardCharsets.UTF_8);
        String replaced = fileToString.replace("${samlLocation}", configuration.getString("saml.location"));
        return IOUtils.toInputStream(replaced, StandardCharsets.UTF_8);
      }
    };
  }

}
