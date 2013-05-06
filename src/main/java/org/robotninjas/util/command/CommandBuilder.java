package org.robotninjas.util.command;

import com.github.rholder.retry.Retryer;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.robotninjas.util.callable.DecoratedCallableBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Futures.withFallback;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

public class CommandBuilder<V> {

  private final DecoratedCallableBuilder<V> callableBuilder = DecoratedCallableBuilder.newBuilder();
  private Executor executor = sameThreadExecutor();

  public static <V> CommandBuilder<V> builder() {
    return new CommandBuilder();
  }

  public CommandBuilder<V> withTimelimit(long duration, TimeUnit unit) {
    callableBuilder.within(duration, unit);
    return this;
  }

  public CommandBuilder<V> withRetry(Retryer<V> retryer) {
    callableBuilder.withRetryer(retryer);
    return this;
  }

  public CommandBuilder<V> withExecutor(Executor executor) {
    this.executor = executor;
    return this;
  }

  public AsyncCommand<V> build(final Callable<V> callable, final FutureFallback<V> fallback) {
    final Function<Callable<V>, Callable<V>> f = callableBuilder.build();
    return new AsyncCommand<V>() {
      @Override
      public ListenableFuture<V> execute() {
        Callable<V> c = f.apply(callable);
        ListenableFutureTask<V> t = ListenableFutureTask.create(c);
        executor.execute(t);
        return withFallback(t, fallback);
      }
    };
  }

  public AsyncCommand<V> build(final Callable<V> callable) {
    final Function<Callable<V>, Callable<V>> f = callableBuilder.build();
    return new AsyncCommand<V>() {
      @Override
      public ListenableFuture<V> execute() {
        Callable<V> c = f.apply(callable);
        ListenableFutureTask<V> t = ListenableFutureTask.create(c);
        executor.execute(t);
        return t;
      }
    };
  }

}
