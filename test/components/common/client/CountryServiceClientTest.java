package components.common.client;

import static components.common.client.CountryServiceClient.Status.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.routing.Router;
import play.routing.RoutingDsl;
import play.server.Server;

import java.io.InputStream;

public class CountryServiceClientTest {

  private CountryServiceClient client;

  @Before
  public void setup() {
    Router router = new RoutingDsl()
      .GET("/countries/set/export-control").routeTo(() -> {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("common/client/countries.json");
        JsonNode jsonNode = Json.parse(inputStream);
        return ok(jsonNode);
      })
      .build();

    Server server = Server.forRouter(router);
    int port = server.httpPort();
    WSClient ws = WS.newClient(port);
    client = new CountryServiceClient(ws, "localhost", port, 10000, new ObjectMapper());
  }

  @Test
  public void shouldGetCompanyDetails() throws Exception {
    ExecutorThreadPool delegate = new ExecutorThreadPool();
    CountryServiceClient.CountryServiceResponse response = client.getCountries(new HttpExecutionContext(delegate)).toCompletableFuture().get();
    delegate.stop();

    assertThat(response.getStatus()).isEqualTo(SUCCESS);
    assertThat(response.getCountries().size()).isEqualTo(18);
  }

}