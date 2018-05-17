package components.common.auth;

import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.play.LogoutController;
import org.pac4j.play.store.PlaySessionStore;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SamlUtil {

  public static LogoutController createLogoutController(Config config, String defaultUrl) {
    LogoutController logoutController = new LogoutController();
    logoutController.setLocalLogout(true);
    logoutController.setDestroySession(true);
    String logoutUrl = config.getString("saml.logoutUrl");
    if ("default".equals(logoutUrl)) {
      logoutController.setDefaultUrl(defaultUrl);
    } else {
      logoutController.setDefaultUrl(logoutUrl);
    }
    return logoutController;
  }

  public static org.pac4j.core.config.Config buildConfig(Config config,
                                                         PlaySessionStore playSessionStore,
                                                         HttpActionAdapter httpActionAdapter,
                                                         Map<String, Authorizer> authorizers) {
    SAML2ClientConfiguration samlConfig = buildSaml2ClientConfiguration(config);

    //Custom SAML client which will store response attributes on the Saml Profile
    SAML2Client saml2Client = new SpireSAML2Client(samlConfig);

    Clients clients = new Clients(config.getString("saml.callbackUrl"), saml2Client);

    clients.setDefaultClient(saml2Client);
    saml2Client.setCallbackUrl(config.getString("saml.callbackUrl"));
    saml2Client.setIncludeClientNameInCallbackUrl(false);

    org.pac4j.core.config.Config pacConfig = new org.pac4j.core.config.Config(clients);

    pacConfig.setHttpActionAdapter(httpActionAdapter);
    pacConfig.setSessionStore(playSessionStore);
    pacConfig.setAuthorizers(authorizers);

    return pacConfig;
  }

  private static SAML2ClientConfiguration buildSaml2ClientConfiguration(Config config) {
    SAML2ClientConfiguration samlConfig = new SAML2ClientConfiguration(new ClassPathResource("saml/keystore.jks"),
        "lite-ogel-registration",
        "jks",
        "keypass",
        "keypass",
        getIdentityProviderMetadataResource(config));

    //Maximum permitted age of IdP response in seconds
    samlConfig.setMaximumAuthenticationLifetime(3600);

    //AKA Saml2:Issuer
    samlConfig.setServiceProviderEntityId(config.getString("saml.issuer"));

    return samlConfig;
  }

  private static Resource getIdentityProviderMetadataResource(Config config) {
    String filePath = "saml/template-metadata.xml";
    String fileToString = null;
    try {
      fileToString = IOUtils.toString(new ClassPathResource(filePath).getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException ioe) {
      throw new RuntimeException("Unable to read " + filePath, ioe);
    }
    String replaced = fileToString.replace("${entityId}", config.getString("saml.entityId"))
        .replace("${x509Certificate}", config.getString("saml.x509Certificate"))
        .replace("${samlLocation}", config.getString("saml.location"));
    return new InputStreamResource(IOUtils.toInputStream(replaced, StandardCharsets.UTF_8));
  }

}
