package filters.common;

import org.apache.commons.lang3.StringUtils;

public class JwtRequestFilterConfig {
  private final String key;
  private final String issuer;

  public JwtRequestFilterConfig(String key, String issuer) {
    validate(key, issuer);
    this.key = key;
    this.issuer = issuer;
  }

  private void validate(String key, String issuer) {
    if (key == null || key.getBytes().length < 64) {
      throw new RuntimeException("key must be >= 64 bytes in length");
    }
    if (StringUtils.isBlank(issuer)) {
      throw new RuntimeException("issuer must not be empty, blank, or null");
    }
  }

  public String getKey() {
    return key;
  }

  public String getIssuer() {
    return issuer;
  }
}
