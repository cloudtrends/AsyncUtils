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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.TimeLimiter;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

class DecoratedCallableFunction<V> implements Function<Callable<V>, Callable<V>> {

  private final TimeLimiter limiter;
  private Optional<TimeUnit> unit = Optional.absent();
  private Optional<Long> duration = Optional.absent();
  private Optional<Retryer<V>> retryer = Optional.absent();

  DecoratedCallableFunction(TimeLimiter limiter) {
    this.limiter = limiter;
  }

  void setTimeout(long duration, TimeUnit unit) {
    this.unit = Optional.of(unit);
    this.duration = Optional.of(duration);
  }

  void setRetryer(Retryer<V> retryer) {
    this.retryer = Optional.of(retryer);
  }

  @Nullable
  @Override
  public Callable<V> apply(@Nullable final Callable<V> c) {

    Callable<V> callable = c;
    if (duration.isPresent() && unit.isPresent()) {
      new Callable<V>() {
        @Override
        public V call() throws Exception {
          return limiter.callWithTimeout(c, duration.get(), unit.get(), true);
        }
      };
    }

    if (retryer.isPresent()) {
      return retryer.get().wrap(callable);
    }

    return callable;
  }
}
