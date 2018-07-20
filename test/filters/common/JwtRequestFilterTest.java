package filters.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.unauthorized;

import com.fasterxml.jackson.databind.JsonNode;
import components.common.auth.AuthInfo;
import components.common.auth.SpireAuthManager;
import components.common.client.UserServiceClientBasicAuth;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.StandaloneWSResponse;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSRequestExecutor;
import play.mvc.Result;
import play.mvc.Results;
import play.routing.RoutingDsl;
import play.server.Server;
import play.test.WSTestClient;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class JwtRequestFilterTest {
  private static final String USER_ID = "USER_ID";
  private static final String UNAUTHORIZED_USER_ID = "UNAUTHORIZED";
  private static final String ERROR_USER_ID = "ERROR";
  private static final String ISSUER = "some-service";
  private Server server;

  @Before
  public void setUp() throws Exception {
    server = Server.forRouter(builtInComponents -> RoutingDsl.fromComponents(builtInComponents)
        .GET("/test").routeTo((Supplier<Result>) Results::ok)
        .GET("/user-account-type/:id").routeTo(id -> {
          if (USER_ID.equals(id)) {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("common/client/user-account-type.json");
            JsonNode jsonNode = Json.parse(inputStream);
            return ok(jsonNode);
          } else if (StringUtils.equals((String) id, UNAUTHORIZED_USER_ID)) {
            return unauthorized();
          } else if (StringUtils.equals((String) id, ERROR_USER_ID)) {
            return internalServerError();
          } else {
            return notFound();
          }
        })
        .build());
  }

  /**
   * Test method, testing {@link JwtRequestFilter}'s JWT generation and inclusion in the "Authorization" header for a {@link WSRequest}
   *
   * @param authInfo The non null auth info
   * @param issuer   The issuer to include in the "iss" JWT claim
   * @return A loosely validated set of JWT Claims for testing
   * @throws Exception
   */
  private JwtClaims doRequestFilterTestWithGivenAuthInfo(AuthInfo authInfo, String issuer) throws Exception {
    String key = "demo-secret-which-is-very-long-so-as-to-hit-the-byte-requirement";

    WSClient wsClient = WSTestClient.newClient(server.httpPort());

    SpireAuthManager spireAuthManager = mock(SpireAuthManager.class);
    when(spireAuthManager.getAuthInfoFromContext()).thenReturn(authInfo);

    // Custom executor, for retrieving the WSRequest object during execution
    WSRequestExecutorTestHarness executor = new WSRequestExecutorTestHarness();

    // Construct user service client
    String serviceUrl = "http://localhost:" + server.httpPort();
    UserServiceClientBasicAuth userServiceClientBasicAuth = new UserServiceClientBasicAuth(serviceUrl, 1000, "service:password", wsClient, new HttpExecutionContext(Runnable::run));

    JwtRequestFilter filter = new JwtRequestFilter(spireAuthManager, new JwtRequestFilterConfig(key, issuer), userServiceClientBasicAuth);
    filter.apply(executor)
        .apply(wsClient.url("/test"))
        .toCompletableFuture().get();

    // Extract token from Authorization header
    String authorizationHeader = (String) executor.getStandaloneWSRequest().getHeaders().get("Authorization").toArray()[0];
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

    private StandaloneWSRequest standaloneWSRequest;

    public StandaloneWSRequest getStandaloneWSRequest() {
      return standaloneWSRequest;
    }

    @Override
    public CompletionStage<StandaloneWSResponse> apply(StandaloneWSRequest standaloneWSRequest) {
      this.standaloneWSRequest = standaloneWSRequest;
      return (CompletionStage<StandaloneWSResponse>) standaloneWSRequest.execute();
    }

  }

  @Test
  public void normalClaimsTest() throws Exception {
    // Mock auth manager with test data
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(USER_ID);
    when(authInfo.getEmail()).thenReturn("example@example.org");
    when(authInfo.getFullName()).thenReturn("Mr test");

    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER);

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.getSubject()).isEqualTo(USER_ID);
    assertThat(claims.getStringClaimValue("email")).isEqualTo("example@example.org");
    assertThat(claims.getStringClaimValue("fullName")).isEqualTo("Mr test");
  }

  @Test
  public void emptyClaimsTest() throws Exception {
    // Mock auth manager with test data, empty id first
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn("");
    when(authInfo.getEmail()).thenReturn("");
    when(authInfo.getFullName()).thenReturn("");

    assertThatThrownBy(() -> doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER))
        .isExactlyInstanceOf(JwtRequestFilterException.class).hasMessage("id provided by auth info is invalid '%s'", "");
  }

  @Test
  public void emptyClaimsWithIdTest() throws Exception {
    // Mock auth manager with test data, empty id first
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(USER_ID);
    when(authInfo.getEmail()).thenReturn("");
    when(authInfo.getFullName()).thenReturn("");
    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER);

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.getSubject()).isEqualTo(USER_ID);
    assertThat(claims.getStringClaimValue("email")).isEqualTo("");
    assertThat(claims.getStringClaimValue("fullName")).isEqualTo("");
  }

  @Test
  public void blankClaimsTest() throws Exception {
    // Mock auth manager with test data, empty id first
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(" ");
    when(authInfo.getEmail()).thenReturn(" ");
    when(authInfo.getFullName()).thenReturn(" ");

    assertThatThrownBy(() -> doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER))
        .isExactlyInstanceOf(JwtRequestFilterException.class).hasMessage("id provided by auth info is invalid '%s'", " ");
  }

  @Test
  public void blankClaimsWithIdTest() throws Exception {
    // Mock auth manager with test data, empty id first
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(USER_ID);
    when(authInfo.getEmail()).thenReturn(" ");
    when(authInfo.getFullName()).thenReturn(" ");

    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER);

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.getSubject()).isEqualTo(USER_ID);
    assertThat(claims.getStringClaimValue("email")).isEqualTo(" ");
    assertThat(claims.getStringClaimValue("fullName")).isEqualTo(" ");
  }

  @Test
  public void nullClaimsTest() throws Exception {
    // Mock auth manager with test data
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(null);
    when(authInfo.getEmail()).thenReturn(null);
    when(authInfo.getFullName()).thenReturn(null);

    assertThatThrownBy(() -> doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER))
        .isExactlyInstanceOf(JwtRequestFilterException.class).hasMessage("id provided by auth info is invalid 'null'");
  }

  @Test
  public void nullClaimsWithIdTest() throws Exception {
    // Mock auth manager with test data
    AuthInfo authInfo = mock(AuthInfo.class);
    when(authInfo.getId()).thenReturn(USER_ID);
    when(authInfo.getEmail()).thenReturn(null);
    when(authInfo.getFullName()).thenReturn(null);

    JwtClaims claims = doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER);

    // Generated claims
    assertThat(claims.getJwtId()).isNotEmpty();
    assertThat(claims.getIssuedAt()).isNotNull();
    assertThat(claims.getNotBefore()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();

    // Configurable claims
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);

    assertThat(claims.getSubject()).isEqualTo(USER_ID);

    Map<String, Object> claimsMap = claims.getClaimsMap();

    assertThat(claimsMap.containsKey("email")).isTrue();
    assertThat(claimsMap.get("email")).isNull();

    assertThat(claimsMap.containsKey("fullName")).isTrue();
    assertThat(claimsMap.get("fullName")).isNull();
  }

  @Test
  public void userServiceErrorNotFound() throws Exception {
    AuthInfo authInfo = mock(AuthInfo.class);

    when(authInfo.getId()).thenReturn("NOT_FOUND");
    assertThatThrownBy(() -> doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER))
        .isExactlyInstanceOf(JwtRequestFilterException.class)
        .hasMessage("Error requesting user account type for id '%s'", "NOT_FOUND");
  }

  @Test
  public void userServiceErrorServiceError() throws Exception {
    AuthInfo authInfo = mock(AuthInfo.class);

    when(authInfo.getId()).thenReturn(ERROR_USER_ID);
    assertThatThrownBy(() -> doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER))
        .isExactlyInstanceOf(JwtRequestFilterException.class)
        .hasMessage("Error requesting user account type for id '%s'", ERROR_USER_ID);
  }

  @Test
  public void userServiceErrorUnauthorized() throws Exception {
    AuthInfo authInfo = mock(AuthInfo.class);

    when(authInfo.getId()).thenReturn(UNAUTHORIZED_USER_ID);
    assertThatThrownBy(() -> doRequestFilterTestWithGivenAuthInfo(authInfo, ISSUER))
        .isExactlyInstanceOf(JwtRequestFilterException.class)
        .hasMessage("Error requesting user account type for id '%s'", UNAUTHORIZED_USER_ID);
  }

}

