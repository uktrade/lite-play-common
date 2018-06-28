package components.common.auth;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.store.PlaySessionStore;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages SAML authentication information from SPIRE on a request's HTTP context.
 */
public class SpireAuthManager {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SpireAuthManager.class);

  private static final String ID_ATTRIBUTE = "ID";
  private static final String EMAIL_ADDRESS_ATTRIBUTE = "PRIMARY_EMAIL_ADDRESS";
  private static final String FORENAME_ATTRIBUTE = "FORENAME";
  private static final String SURNAME_ATTRIBUTE = "SURNAME";

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
      LOGGER.trace(String.format("User info read in %d ms", sw.elapsed(TimeUnit.MILLISECONDS)));
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

    if (id.isPresent() && forename.isPresent() && surname.isPresent() && email.isPresent()) {
      return new AuthInfo(id.get(), forename.get(), surname.get(), email.get());
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
      LOGGER.error("Size of saml attribute {} is not 1", name);
      return Optional.empty();
    }
  }

}
