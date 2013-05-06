package org.robotninjas.util.callable;

import com.github.rholder.retry.Retryer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class CallableBuilder<V> {

  private final TimeLimiter limiter;
  private Optional<TimeUnit> unit = Optional.absent();
  private Optional<Long> duration = Optional.absent();
  private Optional<Retryer<V>> retryer = Optional.absent();

  @VisibleForTesting
  CallableBuilder(TimeLimiter limiter) {
    this.limiter = limiter;
  }

  CallableBuilder() {
    this(new SimpleTimeLimiter());
  }

  public static <V> CallableBuilder<V> newBuilder() {
    return new CallableBuilder();
  }

  public CallableBuilder<V> within(long duration, TimeUnit unit) {
    this.unit = Optional.of(checkNotNull(unit));
    this.duration = Optional.of(checkNotNull(duration));
    return this;
  }

  public CallableBuilder<V> withRetryer(Retryer<V> retryer) {
    this.retryer = Optional.of(retryer);
    return this;
  }

  public Function<Callable<V>, Callable<V>> build() {

    final CallableFunction<V> caller =
      new CallableFunction(limiter);

    if (unit.isPresent() && duration.isPresent()) {
      caller.setTimeout(duration.get(), unit.get());
    }

    if (retryer.isPresent()) {
      caller.setRetryer(retryer.get());
    }

    return caller;
  }

}
