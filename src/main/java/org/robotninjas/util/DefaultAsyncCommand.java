package org.robotninjas.util;

import com.github.rholder.retry.Retryer;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.TimeLimiter;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Futures.withFallback;

class DefaultAsyncCommand<V> implements AsyncCommand<V> {

  private final TimeLimiter limiter;
  private final Callable<V> callable;
  private final Executor executor;
  private Optional<TimeUnit> unit = Optional.absent();
  private Optional<Long> duration = Optional.absent();
  private Optional<FutureFallback<V>> fallback = Optional.absent();
  private Optional<Retryer<V>> retryer = Optional.absent();

  DefaultAsyncCommand(TimeLimiter limiter, Callable<V> callable, Executor executor) {
    this.limiter = limiter;
    this.callable = callable;
    this.executor = executor;
  }

  void setTimeout(long duration, TimeUnit unit) {
    this.unit = Optional.of(unit);
    this.duration = Optional.of(duration);
  }

  void setFallback(FutureFallback<V> fallback) {
    this.fallback = Optional.of(fallback);
  }

  void setRetryer(Retryer<V> retryer) {
    this.retryer = Optional.of(retryer);
  }

  public ListenableFuture<V> execute() {

    Callable<V> userCallable = callable;
    if (duration.isPresent() && unit.isPresent()) {
      new Callable<V>() {
        @Override
        public V call() throws Exception {
          return limiter.callWithTimeout(callable, duration.get(), unit.get(), true);
        }
      };
    }

    if (retryer.isPresent()) {
      userCallable = retryer.get().wrap(userCallable);
    }

    ListenableFutureTask<V> task = ListenableFutureTask.create(userCallable);
    executor.execute(task);

    if (fallback.isPresent()) {
      return withFallback(task, fallback.get());
    }

    return task;
  }

}
