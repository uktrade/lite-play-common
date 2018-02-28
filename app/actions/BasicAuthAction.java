package actions;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BasicAuthAction extends Action.Simple {

  private static final String AUTHORIZATION = "authorization";
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  private final String basicAuthUser;
  private final String basicAuthPassword;
  private final String basicAuthRealm;

  @Inject
  public BasicAuthAction(@Named("basicAuthUser") String basicAuthUser,
                         @Named("basicAuthPassword") String basicAuthPassword,
                         @Named("basicAuthRealm") String basicAuthRealm) {
    this.basicAuthUser = basicAuthUser;
    this.basicAuthPassword = basicAuthPassword;
    this.basicAuthRealm = basicAuthRealm;
  }

  @Override
  public CompletionStage<Result> call(Http.Context context) {
    Optional<String> authHeader = context.request().header(AUTHORIZATION);
    if (authHeader.isPresent() && StringUtils.startsWith(authHeader.get(), "Basic ")) {
      String auth = authHeader.get().substring(6);
      byte[] decodedAuth = Base64.getDecoder().decode(auth);
      String[] credString = new String(decodedAuth, StandardCharsets.UTF_8).split(":");
      if (credString.length == 2 && authenticate(credString[0], credString[1])) {
        return delegate.call(context);
      } else {
        return unauthorizedResult(context);
      }
    } else {
      return unauthorizedResult(context);
    }
  }

  private CompletionStage<Result> unauthorizedResult(Http.Context context) {
    context.response().setHeader(WWW_AUTHENTICATE, "Basic realm=\"" + basicAuthRealm + "\"");
    return CompletableFuture.completedFuture(unauthorized());
  }

  private boolean authenticate(String user, String password) {
    return basicAuthUser.equals(user) && basicAuthPassword.equals(password);
  }

}