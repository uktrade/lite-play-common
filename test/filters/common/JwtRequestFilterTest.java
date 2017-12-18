package filters.common;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import components.common.auth.AuthInfo;
import components.common.auth.SpireAuthManager;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSRequestExecutor;
import play.libs.ws.WSResponse;

import java.util.Map;
import java.util.concurrent.CompletionStage;

public class JwtRequestFilterTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @Before
  public void setUp() throws Exception {
    configureFor(wireMockRule.port());
  }

  /**
   * Test method, testing {@link JwtRequestFilter}'s JWT generation and inclusion in the "Authorization" header for a {@link WSRequest}
   * @param authInfo The non null auth info
   * @param issuer The issuer to include in the "iss" JWT claim
   * @return A loosely validated set of JWT Claims for testing
   * @throws Exception
   */
  private JwtClaims doRequestFilterTestWithGivenAuthInfo(AuthInfo authInfo, String issuer) throws Exception {
    final String key = "demo-secret-which-is-very-long-so-as-to-hit-the-byte-requirement";

    // Setup client and dummy wiremock endpoint
    stubFor(get(urlEqualTo("/test"))
        .willReturn(aResponse().withStatus(200)));
    WSClient wsClient = WS.newClient(wireMockRule.port());

    SpireAuthManager spireAuthManager = mock(SpireAuthManager.class);
    when(spireAuthManager.getAuthInfoFromContext()).thenReturn(authInfo);

    // Custom executor, for retrieving the WSRequest object during execution
    WSRequestExecutorTestHarness executor = new WSRequestExecutorTestHarness();

    JwtRequestFilter filter = new JwtRequestFilter(spireAuthManager, new JwtRequestFilterConfig(key, issuer));
    filter.apply(executor)
        .apply(wsClient.url("/test"))
        .toCompletableFuture().get();

    // Extract token from Authorization header
    String authorizationHeader = (String) executor.getRequest().getHeaders().get("Authorization").toArray()[0];
    System.out.println("Authorization Header: " + authorizationHeader);

    String token = authorizationHeader.replace("Bearer ", "");
    System.out.println("JWT: " + token);

    // Validate signature, consumer claims without restrictions
    JwtConsumer consumer = new JwtConsumerBuilder()
        .setVerificationKey(new HmacKey(key.getBytes()))
        .setJwsAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, AlgorithmIdentifiers.HMAC_SHA256))
        .build();

    JwtClaims claims = consumer.processToClaims(token);
    System.out.println("Claims: " + claims);

    return claims;
  }

  /**
   * Executor for intercepting WSRequest object during completion stage chain execution
   */
  class WSRequestExecutorTestHarness implements WSRequestExecutor {

    private WSRequest request;

    /**
     * @return The last WSRequest object used by {@link WSRequestExecutorTestHarness#apply(WSRequest)}
     */
    public WSRequest getRequest() {
      return request;
    }

    @Override
    public CompletionStage<WSResponse> apply(WSRequest request) {
      this.request = request;
      return request.execute();
    }
  }

  @Test
  public void normalClaimsTest() throws Exception {
    // Mock auth manager with test data
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn("123456");
    when(authInfo.getEmail()).thenReturn("example@example.org");
    when(authInfo.getFullName()).thenReturn("Mr test");

    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, "some-service");

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo("some-service");
    assertThat(claims.getSubject()).isEqualTo("123456");
    assertThat(claims.getStringClaimValue("email")).isEqualTo("example@example.org");
    assertThat(claims.getStringClaimValue("fullName")).isEqualTo("Mr test");
  }

  @Test
  public void emptyClaimsTest() throws Exception {
    // Mock auth manager with test data
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn("");
    when(authInfo.getEmail()).thenReturn("");
    when(authInfo.getFullName()).thenReturn("");

    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, "some-service");

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo("some-service");
    assertThat(claims.getSubject()).isEqualTo("");
    assertThat(claims.getStringClaimValue("email")).isEqualTo("");
    assertThat(claims.getStringClaimValue("fullName")).isEqualTo("");
  }

  @Test
  public void nullClaimsTest() throws Exception {
    // Mock auth manager with test data
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(null);
    when(authInfo.getEmail()).thenReturn(null);
    when(authInfo.getFullName()).thenReturn(null);

    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, "some-service");

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo("some-service");

    Map<String, Object> claimsMap = claims.getClaimsMap();

    assertThat(claimsMap.containsKey(ReservedClaimNames.SUBJECT)).isTrue();
    assertThat(claimsMap.get(ReservedClaimNames.SUBJECT)).isNull();

    assertThat(claimsMap.containsKey("email")).isTrue();
    assertThat(claimsMap.get("email")).isNull();

    assertThat(claimsMap.containsKey("fullName")).isTrue();
    assertThat(claimsMap.get("fullName")).isNull();
  }
}

