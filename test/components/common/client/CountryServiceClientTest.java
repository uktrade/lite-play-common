package components.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.common.Country;
import org.junit.After;
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
import java.util.List;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Results.ok;

public class CountryServiceClientTest {

  private static final String PATH = "/countries/set/export-control";

  private CountryServiceClient client;
  private WSClient ws;
  private Server server;

  @Before
  public void setUp() {
    Router router = new RoutingDsl()
      .GET(PATH).routeTo(() -> {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("common/client/countries.json");
        JsonNode jsonNode = Json.parse(inputStream);
        return ok(jsonNode);
      })
      .build();

    server = Server.forRouter(router);
    int port = server.httpPort();
    ws = WS.newClient(port);
    String serviceUrl = "http://localhost:" + port + PATH;
    client = new CountryServiceClient(new HttpExecutionContext(Runnable::run), ws, serviceUrl, 10000, new ObjectMapper());
  }

  @Test
  public void shouldGetCountries() throws Exception {

    CompletionStage<List<Country>> completionStage = client.getCountries();

    List<Country> countries = completionStage.toCompletableFuture().get();

    assertThat(countries).isNotEmpty();
    assertThat(countries.size()).isEqualTo(18);
  }

  @After
  public void cleanUp() throws Exception {
    try {
      ws.close();
    }
    finally {
      server.stop();
    }
  }



}