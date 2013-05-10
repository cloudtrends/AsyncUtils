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
package org.robotninjas.util.composition;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

/**
 * Build a composition of any number of functions
 *
 * @param <I>
 *   The composition's input
 * @param <X>
 *   The next step's input
 * @param <O>
 *   The composition and the next step's output
 */
@Immutable
public class FunctionComposition<I, X, O> implements Composition<I, X, O> {

  private final Executor executor;
  private final ImmutableList<Stage> stages;

  FunctionComposition(ImmutableList<Stage> stages, Executor executor) {
    this.stages = stages;
    this.executor = executor;
  }

  public static <Z> FunctionComposition<Z, Z, Z> builder() {
    return new FunctionComposition<Z, Z, Z>(ImmutableList.<Stage>of(), sameThreadExecutor());
  }

  public static <Z> FunctionComposition<Z, Z, Z> builder(Executor e) {
    return new FunctionComposition<Z, Z, Z>(ImmutableList.<Stage>of(), e);
  }

  public <Y> FunctionComposition<I, O, Y> transform(AsyncFunction<O, Y> f) {
    ImmutableList<Stage> next = addStage(stages, new AsyncStage(f, executor));
    return new FunctionComposition<I, O, Y>(next, executor);
  }

  public <Y> FunctionComposition<I, O, Y> transform(Function<O, Y> f) {
    ImmutableList<Stage> next = addStage(stages, new SyncStage(f, executor));
    return new FunctionComposition<I, O, Y>(next, executor);
  }

  public <Y> FunctionComposition<I, O, Y> transform(AsyncFunction<O, Y> f, Executor e) {
    ImmutableList<Stage> next = addStage(stages, new AsyncStage(f, e));
    return new FunctionComposition<I, O, Y>(next, executor);
  }

  public <Y> FunctionComposition<I, O, Y> transform(Function<O, Y> f, Executor e) {
    ImmutableList<Stage> next = addStage(stages, new SyncStage(f, e));
    return new FunctionComposition<I, O, Y>(next, executor);
  }

  public FunctionComposition<I, X, O> fork(FunctionComposition<O, ?, ?> composition) {
    ImmutableList<Stage> next = addStage(stages, new BuilderForkStage(composition, executor));
    return new FunctionComposition<I, X, O>(next, executor);
  }

  public <Z> FunctionComposition<I, X, O> fork(Iterable<AsyncFunction<O, Z>> f, Executor e) {
    ImmutableList<Stage> next = addStage(stages, new FunctionsForkStage(f, e));
    return new FunctionComposition<I, X, O>(next, executor);
  }

  public <Z> FunctionComposition<I, X, O> fork(Iterable<AsyncFunction<O, Z>> f) {
    return fork(f, executor);
  }

  public <Z> FunctionComposition<I, X, O> fork(AsyncFunction<O, Z>... f) {
    return fork(Arrays.asList(f));
  }

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> allAsList(Iterable<AsyncFunction<O, Y>> f, Executor e) {
    ImmutableList<Stage> next = addStage(stages, new AllAsListStage(f, executor));
    return new FunctionComposition<I, X, Z>(next, e);
  }

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> allAsList(Iterable<AsyncFunction<O, Y>> f) {
    return allAsList(f, executor);
  }

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> allAsList(AsyncFunction<O, Y>... f) {
    return allAsList(Arrays.asList(f));
  }

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> successfulAsList(Iterable<AsyncFunction<O, Y>> f, Executor e) {
    ImmutableList<Stage> next = addStage(stages, new SuccessfulAsListStage(f, executor));
    return new FunctionComposition<I, X, Z>(next, e);
  }

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> successfulAsList(Iterable<AsyncFunction<O, Y>> f) {
    return allAsList(f, executor);
  }

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> successfulAsList(AsyncFunction<O, Y>... f) {
    return allAsList(Arrays.asList(f));
  }

  private ImmutableList<Stage> addStage(ImmutableList<Stage> stages, Stage stage) {
    ImmutableList.Builder builder = ImmutableList.builder();
    return builder.addAll(stages).add(stage).build();
  }

  private ListenableFuture<O> buildChain(ListenableFuture<I> start) {
    final Iterator<Stage> itr = stages.iterator();
    final Stage firstStage = itr.next();
    ListenableFuture end = firstStage.transform(start);
    while (itr.hasNext()) {
      final Stage nextStage = itr.next();
      end = nextStage.transform(end);
    }
    return end;
  }

  public AsyncFunction<I, O> buildAsyncFunction() {

    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) throws Exception {
        final SettableFuture<I> start = SettableFuture.create();
        ListenableFuture<O> end = buildChain(start);
        start.set(input);
        return end;
      }
    };

  }

  public Function<I, O> buildFunction() {
    return new Function<I, O>() {
      @Nullable
      @Override
      public O apply(@Nullable I input) {
        try {
          return buildAsyncFunction().apply(input).get();
        } catch (Exception e) {
          throw propagate(e);
        }
      }
    };
  }

  public ListenableFuture<O> buildFrom(ListenableFuture<I> start) {
    return buildChain(start);
  }

  private interface Stage<I, O> {
    ListenableFuture<O> transform(ListenableFuture<I> f);
  }

  private static class AsyncStage<I, O> implements Stage<I, O> {

    private final AsyncFunction<I, O> func;
    private final Executor executor;

    private AsyncStage(AsyncFunction<I, O> func, Executor executor) {
      this.func = func;
      this.executor = executor;
    }

    @Override
    public ListenableFuture<O> transform(ListenableFuture f) {
      return Futures.transform(f, func, executor);
    }
  }

  private static class SyncStage<I, O> implements Stage<I, O> {

    private final Function<I, O> func;
    private final Executor executor;

    private SyncStage(Function<I, O> func, Executor executor) {
      this.func = func;
      this.executor = executor;
    }

    @Override
    public ListenableFuture<O> transform(ListenableFuture<I> f) {
      return Futures.transform(f, func, executor);
    }
  }

  private static class BuilderForkStage<O> implements Stage<O, O> {

    private final FunctionComposition<O, ?, O> builder;
    private final Executor executor;

    private BuilderForkStage(FunctionComposition<O, ?, O> builder, Executor executor) {
      this.builder = builder;
      this.executor = executor;
    }

    @Override
    public ListenableFuture<O> transform(ListenableFuture<O> f) {
      //TODO do something with result
      Futures.transform(f, builder.buildAsyncFunction(), executor);
      return f;
    }
  }

  private static class FunctionsForkStage<O> implements Stage<O, O> {

    private final Iterable<AsyncFunction<O, ?>> funcs;
    private final Executor executor;

    private FunctionsForkStage(Iterable<AsyncFunction<O, ?>> funcs, Executor executor) {
      this.funcs = funcs;
      this.executor = executor;
    }

    @Override
    public ListenableFuture<O> transform(ListenableFuture<O> f) {

      for(AsyncFunction func : funcs) {
        Futures.transform(f, func, executor);
      }

      return f;
    }
  }

  private static class AllAsListStage<I, O> implements Stage {

    private final Iterable<AsyncFunction<I, O>> funcs;
    private final Executor executor;

    private AllAsListStage(Iterable<AsyncFunction<I, O>> funcs, Executor executor) {
      this.funcs = funcs;
      this.executor = executor;
    }

    @Override
    public ListenableFuture<O> transform(ListenableFuture f) {
      return Futures.transform(f, new AsyncFunction<I, O>() {
        @Override
        public ListenableFuture apply(I input) throws Exception {
          final List<ListenableFuture<O>> futures = Lists.newArrayList();
          for (AsyncFunction f : funcs) {
            futures.add(f.apply(input));
          }
          return Futures.allAsList(futures);
        }
      });
    }
  }

  private static class SuccessfulAsListStage<I, O> implements Stage {

    private final Iterable<AsyncFunction<I, O>> funcs;
    private final Executor executor;

    private SuccessfulAsListStage(Iterable<AsyncFunction<I, O>> funcs, Executor executor) {
      this.funcs = funcs;
      this.executor = executor;
    }

    @Override
    public ListenableFuture transform(ListenableFuture f) {
      return Futures.transform(f, new AsyncFunction<I, O>() {
        @Override
        public ListenableFuture apply(I input) throws Exception {
          final List<ListenableFuture<O>> futures = Lists.newArrayList();
          for (AsyncFunction f : funcs) {
            futures.add(f.apply(input));
          }
          return Futures.successfulAsList(futures);
        }
      });
    }
  }

}
