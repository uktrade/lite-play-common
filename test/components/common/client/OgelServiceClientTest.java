package components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import play.test.WSTestClient;
import uk.gov.bis.lite.ogel.api.view.ApplicableOgelView;
import uk.gov.bis.lite.ogel.api.view.OgelFullView;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OgelServiceClientTest {

  @Test
  public void shouldGetOgel() throws Exception {
    Server server = createServer("common/client/ogel.json", "/ogels/OGL61");
    int port = server.httpPort();
    WSClient wsClient = WSTestClient.newClient(port);
    OgelServiceClient client = buildClient(port, wsClient);

    OgelFullView ogelFullView = client.getById("OGL61").toCompletableFuture().get();

    assertThat(ogelFullView.getId()).isEqualTo("OGL991");

    wsClient.close();
    server.stop();
  }

  @Test
  public void shouldGetApplicableOgel() throws Exception {
    Server server = createServer("common/client/applicable-ogels.json", "/applicable-ogels");
    int port = server.httpPort();
    WSClient wsClient = WSTestClient.newClient(port);
    OgelServiceClient client = buildClient(port, wsClient);

    List<ApplicableOgelView> ogelViews = client.get("ML1a",
        "UK",
        Arrays.asList("France", "Spain"),
        Collections.singletonList("test"),
        true)
        .toCompletableFuture()
        .get();

    assertThat(ogelViews.size()).isEqualTo(1);
    assertThat(ogelViews.get(0).getId()).isEqualTo("OGL991");
    assertThat(ogelViews.get(0).getName()).isEqualTo("OGEL 991");
    assertThat(ogelViews.get(0).getUsageSummary().size()).isEqualTo(3);

    wsClient.close();
    server.stop();
  }

  private Server createServer(String resource, String path) {
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resource);
    JsonNode jsonNode = Json.parse(inputStream);
    return Server.forRouter(builtInComponents -> RoutingDsl.fromComponents(builtInComponents)
        .GET(path)
        .routeTo(() -> ok(jsonNode))
        .build());
  }

  private OgelServiceClient buildClient(int port, WSClient wsClient) {
    return new OgelServiceClient("http://localhost:" + port,
        10000,
        "service:password",
        wsClient,
        new HttpExecutionContext(Runnable::run));
  }

}
