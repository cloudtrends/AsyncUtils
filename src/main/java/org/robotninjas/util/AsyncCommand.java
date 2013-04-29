package org.robotninjas.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class AsyncCommand<V> {

  private final TimeLimiter limiter;
  private final Callable<V> callable;
  private final Executor executor;
  private Optional<TimeUnit> unit = Optional.absent();
  private Optional<Long> duration = Optional.absent();
  private Optional<FutureFallback<V>> fallback = Optional.absent();

  AsyncCommand(TimeLimiter limiter, Callable<V> callable, Executor executor) {
    this.limiter = limiter;
    this.callable = callable;
    this.executor = executor;
  }

  void setTimeout(TimeUnit unit, long duration) {
    this.unit = Optional.of(unit);
    this.duration = Optional.of(duration);
  }

  void setFallback(FutureFallback<V> fallback) {
    this.fallback = Optional.of(fallback);
  }

  public ListenableFuture<V> execute() {

    Callable<V> userCallable = callable;
    if (duration.isPresent()) {
      new Callable<V>() {
        @Override
        public V call() throws Exception {
          return limiter.callWithTimeout(callable, duration.get(), unit.get(), false);
        }
      };
    }

    ListenableFutureTask<V> task = ListenableFutureTask.create(userCallable);
    executor.execute(task);

    if (fallback.isPresent()) {
      return Futures.withFallback(task, fallback.get());
    }

    return task;
  }

}
