/**
 *    Copyright 2013 David Rusek <dave.rusek@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

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

public class DecoratedCallableBuilder {

  private final TimeLimiter limiter;
  private Optional<TimeUnit> unit = Optional.absent();
  private Optional<Long> duration = Optional.absent();
  private Optional<Retryer> retryer = Optional.absent();

  @VisibleForTesting
  DecoratedCallableBuilder(TimeLimiter limiter) {
    this.limiter = limiter;
  }

  DecoratedCallableBuilder() {
    this(new SimpleTimeLimiter());
  }

  public static DecoratedCallableBuilder builder() {
    return new DecoratedCallableBuilder();
  }

  public DecoratedCallableBuilder within(long duration, TimeUnit unit) {
    this.unit = Optional.of(checkNotNull(unit));
    this.duration = Optional.of(checkNotNull(duration));
    return this;
  }

  public DecoratedCallableBuilder withRetryer(Retryer retryer) {
    this.retryer = Optional.of(retryer);
    return this;
  }

  public <V> Function<Callable<V>, Callable<V>> build() {

    final DecoratedCallableFunction<V> caller =
      new DecoratedCallableFunction(limiter);

    if (unit.isPresent() && duration.isPresent()) {
      caller.setTimeout(duration.get(), unit.get());
    }

    if (retryer.isPresent()) {
      caller.setRetryer(retryer.get());
    }

    return caller;
  }

}
