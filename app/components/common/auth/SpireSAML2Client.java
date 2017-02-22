package components.common.auth;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.profile.SAML2Profile;

public class SpireSAML2Client extends SAML2Client {

  public static final String CLIENT_NAME = "spireClient";
  private final SpireAuthManager authManager;

  public SpireSAML2Client(SAML2ClientConfiguration cfg, SpireAuthManager authManager) {
    super(cfg);
    this.authManager = authManager;
  }

  @Override
  public String getName() {
    return CLIENT_NAME;
  }

  @Override
  protected SAML2Profile retrieveUserProfile(SAML2Credentials credentials, WebContext context) {

    SAML2Profile saml2Profile;
    try {
      saml2Profile = super.retrieveUserProfile(credentials, context);
    } catch (HttpAction httpAction) {
      throw new RuntimeException("Unexpected exception retrieving profile from SAML response", httpAction);
    }

    //Store each saml2:attribute in the Pac4j saml2Profile for later retrieval
    authManager.setAttributesOnSamlProfile(credentials, saml2Profile);

    return saml2Profile;
  }
}
