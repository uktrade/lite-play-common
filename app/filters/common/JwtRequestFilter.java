package filters.common;

import com.google.inject.Inject;
import components.common.auth.AuthInfo;
import components.common.auth.SpireAuthManager;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import play.libs.ws.WSRequestExecutor;
import play.libs.ws.WSRequestFilter;

public class JwtRequestFilter implements WSRequestFilter {

  private final SpireAuthManager authManager;
  private final JwtRequestFilterConfig config;

  @Inject
  public JwtRequestFilter(SpireAuthManager authManager, JwtRequestFilterConfig config) {
    this.authManager = authManager;
    this.config = config;
  }

  @Override
  public WSRequestExecutor apply(WSRequestExecutor executor) {
    return request -> {

      AuthInfo authInfo = authManager.getAuthInfoFromContext();

      JwtClaims claims = new JwtClaims();
      claims.setIssuer(config.getIssuer());
      claims.setExpirationTimeMinutesInTheFuture(10);
      claims.setGeneratedJwtId();
      claims.setIssuedAtToNow();
      claims.setNotBeforeMinutesInThePast(2);
      claims.setSubject(authInfo.getId());
      claims.setClaim("email", authInfo.getEmail());
      claims.setClaim("fullName", authInfo.getFullName());

      JsonWebSignature jws = new JsonWebSignature();
      jws.setHeader(HeaderParameterNames.TYPE, "JWT");
      jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
      jws.setKey(new HmacKey(config.getKey().getBytes()));
      jws.setPayload(claims.toJson());

      String token;
      try {
        token = jws.getCompactSerialization();
      } catch (JoseException e) {
        throw new RuntimeException(e);
      }

      request.setHeader("Authorization", "Bearer " + token);

      return executor.apply(request);
    };
  }
}
