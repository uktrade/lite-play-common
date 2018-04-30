package components.common.auth;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.store.PlaySessionStore;
import org.pac4j.saml.profile.SAML2Profile;
import play.Logger;
import play.mvc.Http;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages SAML authentication information from SPIRE on a request's HTTP context.
 */
public class SpireAuthManager {

  private static final String CONTEXT_ARG_NAME = "auth_manager";

  private static final String ID_ATTRIBUTE = "ID";
  private static final String EMAIL_ADDRESS_ATTRIBUTE = "PRIMARY_EMAIL_ADDRESS";
  private static final String FORENAME_ATTRIBUTE = "FORENAME";
  private static final String SURNAME_ATTRIBUTE = "SURNAME";
  private static final String ROLES_ATTRIBUTE = "ROLES";

  private final PlaySessionStore playSessionStore;

  @Inject
  public SpireAuthManager(PlaySessionStore playSessionStore) {
    this.playSessionStore = playSessionStore;
  }

  /**
   * @return AuthInfo constructed from SAML attributes which have been cached by Pac4j.
   */
  public AuthInfo getAuthInfoFromContext() {

    Stopwatch sw = Stopwatch.createStarted();
    try {
      PlayWebContext context = new PlayWebContext(Http.Context.current(), playSessionStore);
      ProfileManager<SAML2Profile> profileManager = new ProfileManager<>(context);
      Optional<SAML2Profile> optionalProfile = profileManager.get(true);

      if (optionalProfile.isPresent()) {
        return getAuthInfoFromSamlProfile(optionalProfile.get());
      } else {
        return AuthInfo.unauthenticatedInstance();
      }
    } finally {
      Logger.trace(String.format("User info read in %d ms", sw.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Reads a set of known attributes from the given SAML profile and creates an AuthInfo instance representing and
   * authenticated user.
   *
   * @param profile Profile to read attributes from.
   * @return AuthInfo details for the current user.
   */
  public AuthInfo getAuthInfoFromSamlProfile(SAML2Profile profile) {

    Map<String, Object> attributes = profile.getAttributes();

    Optional<String> id = getAttribute(attributes, ID_ATTRIBUTE);
    Optional<String> forename = getAttribute(attributes, FORENAME_ATTRIBUTE);
    Optional<String> surname = getAttribute(attributes, SURNAME_ATTRIBUTE);
    Optional<String> email = getAttribute(attributes, EMAIL_ADDRESS_ATTRIBUTE);
    Set<String> roles = getAttributeAsSet(attributes, ROLES_ATTRIBUTE);

    if (id.isPresent() && forename.isPresent() && surname.isPresent() && email.isPresent()) {
      return new AuthInfo(id.get(), forename.get(), surname.get(), email.get(), roles);
    } else {
      return AuthInfo.unauthenticatedInstance();
    }
  }

  @SuppressWarnings("unchecked")
  private Optional<String> getAttribute(Map<String, Object> attributes, String name) {
    List<String> list = (List<String>) attributes.get(name);
    if (CollectionUtils.size(list) == 1) {
      return Optional.of(list.get(0));
    } else {
      Logger.error("Size of saml attribute {} is not 1", name);
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  private Set<String> getAttributeAsSet(Map<String, Object> attributes, String name) {
    List<String> list = (List<String>) attributes.get(name);
    if (list == null) {
      Logger.warn("Attribute " + name + " not present in profile.");
      return Collections.emptySet();
    } else {
      return new HashSet<>(list);
    }
  }

  /**
   * Sets this instance as an argument on the given context, for later retrieval from entities where DI cannot be accessed.
   *
   * @param context Context to set arg on.
   */
  public void setAsContextArgument(Http.Context context) {
    context.args.put(CONTEXT_ARG_NAME, this);
  }

  private static Optional<AuthInfo> currentAuthInfo(Http.Context context) {
    Object authManagerContext = context.args.get(CONTEXT_ARG_NAME);
    if(authManagerContext == null) {
      return Optional.empty();
    }
    else {
      return Optional.of( ((SpireAuthManager) authManagerContext).getAuthInfoFromContext() );
    }
  }

  /**
   * Gets the current userId from the AuthManager defined on the given HTTP context.
   *
   * Note that DI should be used to retrieve the {@link SpireAuthManager} instance where possible.
   *
   * @param context Context.
   * @return The current userId if present.
   */
  public static Optional<String> getCurrentUserId(Http.Context context) {
    return currentAuthInfo(context).map(AuthInfo::getId);
  }

}
