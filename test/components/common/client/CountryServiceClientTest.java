package components.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import components.common.client.CountryServiceClient.CountryServiceEndpoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import play.test.WSTestClient;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class CountryServiceClientTest {

  private static final String BASE_PATH = "/countries/set/";
  private static final String COUNTRY_SET_NAME = "export-control";

  private CountryServiceClient client;
  private WSClient ws;
  private Server server;

  @Before
  public void setUp() {
    server = Server.forRouter(builtInComponents -> RoutingDsl.fromComponents(builtInComponents)
        .GET(BASE_PATH + COUNTRY_SET_NAME).routeTo(() -> {
          InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("common/client/countries.json");
          JsonNode jsonNode = Json.parse(inputStream);
          return ok(jsonNode);
        })
        .build());

    int port = server.httpPort();
    ws = WSTestClient.newClient(port);
    String serviceUrl = "http://localhost:" + port;
    client = new CountryServiceClient(new HttpExecutionContext(Runnable::run), ws, 1000, serviceUrl,
        "service:password", CountryServiceEndpoint.SET, COUNTRY_SET_NAME, new ObjectMapper());
  }

  @Test
  public void shouldGetCountries() throws Exception {

    CompletionStage<List<CountryView>> completionStage = client.getCountries();

    List<CountryView> countries = completionStage.toCompletableFuture().get();

    assertThat(countries).isNotEmpty();
    assertThat(countries.size()).isEqualTo(18);
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
