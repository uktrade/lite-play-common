package actions;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BasicAuthAction extends Action.Simple {

  private static final String AUTHORIZATION = "authorization";
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
  private static final String REALM = "Basic realm=\"Your Realm Here\"";

  private final String basicAuthUser;
  private final String basicAuthPassword;

  @Inject
  public BasicAuthAction(@Named("basicAuthUser") String basicAuthUser,
                         @Named("basicAuthPassword") String basicAuthPassword) {
    this.basicAuthUser = basicAuthUser;
    this.basicAuthPassword = basicAuthPassword;
  }
  @Override
  public CompletionStage<Result> call(Http.Context context) {

    String authHeader = context.request().getHeader(AUTHORIZATION);
    if (authHeader == null) {
      context.response().setHeader(WWW_AUTHENTICATE, REALM);
      return unauthorizedResult();
    }

    try {
      String auth = authHeader.substring(6);
      byte[] decodedAuth = Base64.getDecoder().decode(auth);

      String[] credString = new String(decodedAuth, "UTF-8").split(":");
      if (credString.length != 2) {
        return unauthorizedResult();
      }

      boolean isAuthenticated = authenticate(credString[0], credString[1]);

      return isAuthenticated ? delegate.call(context) : unauthorizedResult();

    } catch (IOException e) {
      Logger.error("Failed to authenticate user.", e);
      throw new RuntimeException(e);
    }

  }

  private CompletionStage<Result> unauthorizedResult() {
    return CompletableFuture.completedFuture(unauthorized());
  }

  private boolean authenticate(String user, String password) {
    return basicAuthUser.equals(user) && basicAuthPassword.equals(password);
  }

}