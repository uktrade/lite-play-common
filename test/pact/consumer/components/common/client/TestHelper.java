package pact.consumer.components.common.client;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

class TestHelper {

  /*
    {
      "typ": "JWT",
      "alg": "HS256"
    }
    {
      "iss": "Some lite application",
      "exp": 1825508319,
      "jti": "Jv1XdmhlFhrbQhq5QaPgyg",
      "iat": 1510148319,
      "nbf": 1510148199,
      "sub": "123456",
      "email": "example@example.com",
      "fullName": "Mr Test",
      "accountType": "UNKNOWN"
    }
    Secret: demo-secret-which-is-very-long-so-as-to-hit-the-byte-requirement
  */
  static final String JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJTb21lIGxpdGUgYXBwbGljYXRpb24iLCJleHAiOjE4MjU1MDgzMTksImp0aSI6Ikp2MVhkbWhsRmhyYlFocTVRYVBneWciLCJpYXQiOjE1MTAxNDgzMTksIm5iZiI6MTUxMDE0ODE5OSwic3ViIjoiMTIzNDU2IiwiZW1haWwiOiJleGFtcGxlQGV4YW1wbGUuY29tIiwiZnVsbE5hbWUiOiJNciBUZXN0IiwiYWNjb3VudFR5cGUiOiJVTktOT1dOIn0.dSmBA7gcZwcDiRFH4a8IjHjFQK54bXVRckl5i9tK6mk";
  static final Map<String, String> JWT_AUTH_HEADERS = ImmutableMap.of("Authorization", "Bearer " + JWT_TOKEN);
  // service:password
  static final Map<String, String> BASIC_AUTH_HEADERS = ImmutableMap.of("Authorization", "Basic c2VydmljZTpwYXNzd29yZA==");
  static final Map<String, String> CONTENT_TYPE_HEADERS = ImmutableMap.of("Content-Type", "application/json");
  static final String PROVIDER = "lite-play-common";
  static final String CONSUMER = "lite-play-common";

  private TestHelper() {
  }

}
