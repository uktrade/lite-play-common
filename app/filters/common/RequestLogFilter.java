package filters.common;

import akka.stream.Materializer;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import play.Logger;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class RequestLogFilter extends Filter {

  @Inject
  public RequestLogFilter(Materializer materializer) {
    super(materializer);
  }

  @Override
  public CompletionStage<Result> apply(Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
                                       Http.RequestHeader requestHeader) {

    Stopwatch stopwatch = Stopwatch.createStarted();

    return nextFilter.apply(requestHeader).thenApply(result -> {

      long requestTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

      Logger.info("{} {} - {} {}ms",  requestHeader.method(), requestHeader.uri(), result.status(), requestTime);

      return result;
    });
  }
}
