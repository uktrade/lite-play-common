package components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.unauthorized;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import play.test.WSTestClient;
import uk.gov.bis.lite.user.api.view.UserAccountTypeView;
import uk.gov.bis.lite.user.api.view.enums.AccountType;

import java.io.InputStream;

public class UserServiceClientBasicAuthTest {
  private static final String USER_ID = "USER_ID";
  private static final String UNAUTHORIZED_USER_ID = "UNAUTHORIZED";
  private static final String ERROR_USER_ID = "ERROR";

  private UserServiceClientBasicAuth client;
  private WSClient ws;
  private Server server;

  @Before
  public void setUp() {
    server = Server.forRouter(builtInComponents -> RoutingDsl.fromComponents(builtInComponents)
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

    int port = server.httpPort();
    ws = WSTestClient.newClient(port);
    String serviceUrl = "http://localhost:" + port;
    client = new UserServiceClientBasicAuth(serviceUrl, 1000, "service:password", ws, new HttpExecutionContext(Runnable::run));
  }

  @Test
  public void getUserAccountTypeViewTest() throws Exception {
    UserAccountTypeView userAccountTypeView = client.getUserAccountTypeView(USER_ID).toCompletableFuture().get();
    assertThat(userAccountTypeView.getAccountType()).isEqualTo(AccountType.EXPORTER);
  }

  @Test
  public void unauthorizedErrorTest() {
    assertThatThrownBy(() -> client.getUserAccountTypeView(UNAUTHORIZED_USER_ID).toCompletableFuture().get())
        .hasMessageContaining("Unexpected status code 401. Body is empty. user-service getUserAccountTypeView failure.");
  }

  @Test
  public void userServiceError() throws Exception {
    assertThatThrownBy(() -> client.getUserAccountTypeView("NOT_FOUND").toCompletableFuture().get())
        .hasMessageEndingWith("Unexpected status code 404. Body is empty. user-service getUserAccountTypeView failure.");
  }

  @Test
  public void internalServerErrorTest() {
    assertThatThrownBy(() -> client.getUserAccountTypeView(ERROR_USER_ID).toCompletableFuture().get())
        .hasMessageContaining("Unexpected status code 500. Body is empty. user-service getUserAccountTypeView failure.");
  }

  @After
  public void cleanUp() throws Exception {
    try {
      ws.close();
    } finally {
      server.stop();
    }
  }

}