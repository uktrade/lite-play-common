package components.common.state;

import static play.mvc.Results.redirect;

import io.mikael.urlbuilder.UrlBuilder;
import play.mvc.Call;
import play.mvc.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Object responsible for "ping-ponging" stateful parameters between requests. Context parameters should be available
 * on an incoming request, and are set on the current HTTP context. Values may be modified as the response is processed,
 * in which case they are updated on the HTTP context. This object should then be used to provide the final values for
 * appending to a URI, or adding to an HTML form as hidden fields.
 */
public class ContextParamManager {

  public static final String CTX_PARAM_NAME = "uri_param_decorator";

  private final List<ContextParamProvider> contextParamProviderList;

  public ContextParamManager(ContextParamProvider... contextParamProviderList) {
    this.contextParamProviderList = Arrays.asList(contextParamProviderList);
  }

  /**
   * Adds all context parameters known to this manager as query string parameters to the given Call.
   * @param call Call to append parameters to.
   * @return New URI for call, with parameters added.
   */
  public String addParamsToCall(Call call) {

    try {
      UrlBuilder urlBuilder = UrlBuilder.fromUri(new URI(call.url()));

      for (ContextParamProvider provider : contextParamProviderList) {
        urlBuilder = urlBuilder.addParameter(provider.getParamName(), provider.getParamValueFromContext());
      }

      return urlBuilder.toUri().toString();

    } catch (URISyntaxException e) {
      throw new RuntimeException("Problem parsing URI", e);
    }
  }

  /**
   * Convenience method which adds all known context params to the given call and returns an HTTP redirect response to
   * the resulting URI.
   * @param call Call to add parameters to.
   * @return Redirect to new URI.
   */
  public CompletionStage<Result> addParamsAndRedirect(Call call) {
    return CompletableFuture.completedFuture(redirect(addParamsToCall(call)));
  }

  /**
   * Updates the current Http Context so it has the latest context parameter values from the current request. This should
   * be called at the start of request processing.
   */
  public void setAllContextArgsFromRequest() {
    contextParamProviderList.forEach(e -> e.updateParamValueOnContext(e.getParamValueFromRequest()));
  }

  /**
   * @return All context param name/value tuples known to this manager, for adding to an HTML form or URL.
   */
  public Collection<ContextParam> getAllContextParams() {
    return contextParamProviderList
        .stream()
        .map(e -> new ContextParam(e.getParamName(), e.getParamValueFromContext()))
        .collect(Collectors.toList());
  }
}
