package components.common.auth;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Attribute;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.profile.SAML2Profile;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import play.Logger;
import play.mvc.Http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages SAML authentication information from SPIRE on a request's HTTP context.
 */
public class SpireAuthManager {

  private static String CONTEXT_ARG_NAME = "auth_manager";

  private static final String ID_ATTRIBUTE = "ID";
  private static final String EMAIL_ADDRESS_ATTRIBUTE = "PRIMARY_EMAIL_ADDRESS";
  private static final String FORENAME_ATTRIBUTE = "FORENAME";
  private static final String SURNAME_ATTRIBUTE = "SURNAME";

  @Inject
  public SpireAuthManager() {
  }

  /**
   * @return AuthInfo constructed from SAML attributes which have been cached by Pac4j.
   */
  public AuthInfo getAuthInfoFromContext() {

    Stopwatch sw = Stopwatch.createStarted();
    try {
      PlayWebContext context = new PlayWebContext(Http.Context.current());
      ProfileManager<SAML2Profile> profileManager = new ProfileManager<>(context);
      Optional<SAML2Profile> optionalProfile = profileManager.get(true);

      if (optionalProfile.isPresent()) {
        return getAuthInfoFromSamlProfile(optionalProfile.get());
      }
      else {
        return AuthInfo.unauthenticatedInstance();
      }
    } finally {
      Logger.trace(String.format("User info read in %d ms", sw.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Reads a set of known <tt>saml2:Attribute</tt>s from a SAML authentication response and copies them onto the
   * given profile for persistence. Attributes are expected in the following format:
   *
   * <pre>{@literal
   * <saml2:Attribute Name="ATTR_NAME">
      <xs:string xmlns:xs="http://www.w3.org/2001/XMLSchema">attr value</xs:string>
    </saml2:Attribute>}
   * </pre>
   *
   * @param credentials Credentials from a SAML2 Authentication response.
   * @param profile Profile object to copy attributes to.
   */
  void setAttributesOnSamlProfile(SAML2Credentials credentials, SAML2Profile profile) {

    List<String> expectedAttrs = Arrays.asList(ID_ATTRIBUTE, EMAIL_ADDRESS_ATTRIBUTE, FORENAME_ATTRIBUTE,
        SURNAME_ATTRIBUTE);

    //All the attribute names we have
    Set<String> attrNames = credentials.getAttributes().stream().map(Attribute::getName).collect(Collectors.toSet());

    //Any required attributes which are missing
    Set<String> missingAttrs = expectedAttrs.stream().filter(e -> !attrNames.contains(e)).collect(Collectors.toSet());

    if (missingAttrs.size() > 0) {
      throw new AuthException("SAML credentials missing the following attributes: " + String.join(", ", missingAttrs));
    }

    //Write credential attributes to the SAML profile

    for (Attribute attribute : credentials.getAttributes()) {
      //Exclude unexpected attributes from the SAML profile
      if (expectedAttrs.contains(attribute.getName())) {
        Element attributeDOM = attribute.getDOM();

        if (attributeDOM == null) {
          throw new AuthException("Malformed SAML response: DOM is missing for attribute " + attribute.getName());
        }

        Node firstChild = attributeDOM.getFirstChild();
        if(firstChild == null) {
          throw new AuthException("Malformed SAML response: attribute value element is missing for attribute "
              + attribute.getName());
        }
        else if (StringUtils.isBlank(firstChild.getTextContent())) {
          throw new AuthException("Malformed SAML response: attribute value element is missing text content for attribute "
              + attribute.getName());
        }

        profile.addAttribute(attribute.getName(), firstChild.getTextContent());
      }
    }
  }

  /**
   * Reads a set of known attributes from the given SAML profile and creates an AuthInfo instance representing and
   * authenticated user.
   * @param profile Profile to read attributes from.
   * @return AuthInfo details for the current user.
   */
  public AuthInfo getAuthInfoFromSamlProfile(SAML2Profile profile) {

    Map<String, Object> attributes = profile.getAttributes();

    String id = (String) attributes.get(ID_ATTRIBUTE);
    String forename = (String) attributes.get(FORENAME_ATTRIBUTE);
    String surname = (String) attributes.get(SURNAME_ATTRIBUTE);
    String email = (String) attributes.get(EMAIL_ADDRESS_ATTRIBUTE);

    return new AuthInfo(id, forename, surname, email);
  }

  /**
   * Sets this instance as an argument on the given context, for later retrieval in views. This can be removed when
   * DI is supported in views.
   * @param context Context to set arg on.
   */
  public void setAsContextArgument(Http.Context context) {
    context.args.put(CONTEXT_ARG_NAME, this);
  }

  /**
   * Gets the current AuthInfo from the AuthManager defined on the given context.
   * @param context Context.
   * @return Current AuthInfo.
   */
  public static AuthInfo currentAuthInfo(Http.Context context) {
    return ((SpireAuthManager) context.args.get(CONTEXT_ARG_NAME)).getAuthInfoFromContext();
  }

}
