package components.common.auth;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * Properties of an authenticated user. All fields will be null if the user is unauthenticated.
 */
public class AuthInfo {

  private final String id;
  private final String forename;
  private final String surname;
  private final String email;
  private final Set<String> roles;

  public static AuthInfo unauthenticatedInstance() {
    return new AuthInfo(null, null, null, null, null);
  }

  AuthInfo(String id, String forename, String surname, String email, Set<String> roles) {
    this.id = id;
    this.forename = forename;
    this.surname = surname;
    this.email = email;
    this.roles = roles;
  }

  public boolean isAuthenticated() {
    return StringUtils.isNoneBlank(id);
  }

  public String getId() {
    return id;
  }

  public String getForename() {
    return forename;
  }

  public String getSurname() {
    return surname;
  }

  public String getFullName() {
    if (isAuthenticated()) {
      return forename + " " + surname;
    }
    else {
      return "Unauthenticated user";
    }
  }

  public String getEmail() {
    return email;
  }

  public Set<String> getRoles() {
    return roles;
  }
}
