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

public class CommandBuilder {

  private final DecoratedCallableBuilder callableBuilder = DecoratedCallableBuilder.builder();
  private Executor executor = sameThreadExecutor();

  public static CommandBuilder builder() {
    return new CommandBuilder();
  }

  public CommandBuilder withTimelimit(long duration, TimeUnit unit) {
    callableBuilder.within(duration, unit);
    return this;
  }

  public CommandBuilder withRetry(Retryer retryer) {
    callableBuilder.withRetryer(retryer);
    return this;
  }

  public CommandBuilder withExecutor(Executor executor) {
    this.executor = executor;
    return this;
  }

  public <V> AsyncCommand<V> build(final Callable<V> callable, final FutureFallback<V> fallback) {
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

  public <V> AsyncCommand<V> build(final Callable<V> callable) {
    final Function<Callable<V>, Callable<V>> f = callableBuilder.build();
    return new AsyncCommand<V>() {
      @Override
      public ListenableFuture<V> execute() throws Exception {
        Callable<V> c = f.apply(callable);
        ListenableFutureTask<V> t = ListenableFutureTask.create(c);
        executor.execute(t);
        return t;
      }
    };
  }

}
