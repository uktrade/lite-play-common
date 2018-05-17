package filters.common;

import com.google.inject.Inject;
import components.common.auth.AuthInfo;
import components.common.auth.SpireAuthManager;
import components.common.client.userservice.UserServiceClientBasicAuth;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import play.libs.ws.WSRequestExecutor;
import play.libs.ws.WSRequestFilter;
import uk.gov.bis.lite.user.api.view.UserAccountTypeView;

import java.util.concurrent.ExecutionException;

public class JwtRequestFilter implements WSRequestFilter {

  private final SpireAuthManager authManager;
  private final JwtRequestFilterConfig config;
  private final UserServiceClientBasicAuth userServiceClientBasicAuth;

  @Inject
  public JwtRequestFilter(SpireAuthManager authManager, JwtRequestFilterConfig config, UserServiceClientBasicAuth userServiceClientBasicAuth) {
    this.authManager = authManager;
    this.config = config;
    this.userServiceClientBasicAuth = userServiceClientBasicAuth;
  }

  @Override
  public WSRequestExecutor apply(WSRequestExecutor executor) {
    return request -> {

      AuthInfo authInfo = authManager.getAuthInfoFromContext();

      if (StringUtils.isBlank(authInfo.getId())) {
        throw new JwtRequestFilterException(String.format("id provided by auth info is invalid '%s'", authInfo.getId()));
      }

      UserAccountTypeView userAccountTypeView;
      try {
        userAccountTypeView = userServiceClientBasicAuth.getUserAccountTypeView(authInfo.getId()).toCompletableFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        throw new JwtRequestFilterException(String.format("Error requesting user account type for id '%s'", authInfo.getId()), e);
      }

      JwtClaims claims = new JwtClaims();
      claims.setIssuer(config.getIssuer());
      claims.setExpirationTimeMinutesInTheFuture(10);
      claims.setGeneratedJwtId();
      claims.setIssuedAtToNow();
      claims.setNotBeforeMinutesInThePast(2);
      claims.setSubject(authInfo.getId());
      claims.setClaim("email", authInfo.getEmail());
      claims.setClaim("fullName", authInfo.getFullName());
      claims.setClaim("accountType", userAccountTypeView.getAccountType().toString());

      JsonWebSignature jws = new JsonWebSignature();
      jws.setHeader(HeaderParameterNames.TYPE, "JWT");
      jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
      jws.setKey(new HmacKey(config.getKey().getBytes()));
      jws.setPayload(claims.toJson());

      String token;
      try {
        token = jws.getCompactSerialization();
      } catch (JoseException e) {
        throw new JwtRequestFilterException("Error during jwt serialization", e);
      }

      request.addHeader("Authorization", "Bearer " + token);

      return executor.apply(request);
    };
  }
}
