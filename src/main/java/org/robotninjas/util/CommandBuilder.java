package org.robotninjas.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

public class CommandBuilder<V> {

  private final TimeLimiter limiter;
  private Optional<Executor> executor = Optional.of((Executor) sameThreadExecutor());
  private Optional<Callable<V>> callable = Optional.absent();
  private Optional<TimeUnit> unit = Optional.absent();
  private Optional<Long> duration = Optional.absent();
  private Optional<FutureFallback<V>> fallback = Optional.absent();

  @VisibleForTesting
  CommandBuilder(TimeLimiter limiter) {
    this.limiter = limiter;
  }

  CommandBuilder() {
    this(new SimpleTimeLimiter());
  }

  public static CommandBuilder newBuilder() {
    return new CommandBuilder();
  }

  public CommandBuilder call(Callable<V> callable) {
    this.callable = Optional.of(checkNotNull(callable));
    return this;
  }

  public CommandBuilder within(long duration, TimeUnit unit) {
    this.unit = Optional.of(checkNotNull(unit));
    this.duration = Optional.of(checkNotNull(duration));
    return this;
  }

  public CommandBuilder withFallback(FutureFallback<V> fallback) {
    this.fallback = Optional.of(checkNotNull(fallback));
    return this;
  }

  public CommandBuilder onExecutor(Executor executor) {
    this.executor = Optional.of(checkNotNull(executor));
    return this;
  }

  public AsyncCommand<V> buildAsync() {
    checkState(callable.isPresent());
    checkState(executor.isPresent());
    final DefaultAsyncCommand<V> caller =
      new DefaultAsyncCommand(limiter, callable.get(), executor.get());
    if (unit.isPresent()) {
      caller.setTimeout(unit.get(), duration.get());
    }
    if (fallback.isPresent()) {
      caller.setFallback(fallback.get());
    }
    return caller;
  }

  public Command<V> buildSync() {
    return new Command<V>() {
      @Override
      public V execute() {
        return Futures.getUnchecked(buildAsync().execute());
      }
    };
  }

}
