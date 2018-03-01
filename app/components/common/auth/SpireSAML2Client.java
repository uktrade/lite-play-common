package components.common.auth;

import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;

public class SpireSAML2Client extends SAML2Client {

  public static final String CLIENT_NAME = "spireClient";

  public SpireSAML2Client(SAML2ClientConfiguration configuration) {
    super(configuration);
  }

  @Override
  public String getName() {
    return CLIENT_NAME;
  }

}
