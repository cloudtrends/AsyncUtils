package org.robotninjas.util;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.*;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.util.concurrent.Futures.*;
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
public class FunctionCompositionBuilder<I, X, O> {

  private final SettableFuture<I> start;
  private final ListenableFuture<O> end;
  private final Executor executor;

  /**
   * Construct a builder for the next step
   *
   * @param start
   *   the composition's starting settable future
   * @param end
   *   the ListenableFuture which returns the final result
   */
  FunctionCompositionBuilder(SettableFuture<I> start, ListenableFuture<O> end, Executor executor) {
    this.start = start;
    this.end = end;
    this.executor = executor;
  }

  /**
   * Construct an initial builder
   *
   * @param <Z>
   *   the input to the composed function
   * @return a builder for the next step of the coposition
   */
  public static <Z> FunctionCompositionBuilder<Z, Z, Z> builder() {
    final SettableFuture<Z> begin = SettableFuture.create();
    return new FunctionCompositionBuilder<Z, Z, Z>(begin, begin, sameThreadExecutor());
  }

  /**
   * Construct an initial builder
   *
   * @param e
   *   an executor to runn all transform operations
   * @param <Z>
   *   the input to the composed function
   * @return a builder for the next step of the coposition
   */
  public static <Z> FunctionCompositionBuilder<Z, Z, Z> builder(Executor e) {
    final SettableFuture<Z> begin = SettableFuture.create();
    return new FunctionCompositionBuilder<Z, Z, Z>(begin, begin, e);
  }

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param <Y>
   *   the output of this step
   * @return a builder for the next step
   */
  public <Y> FunctionCompositionBuilder<I, O, Y> andThen(final AsyncFunction<O, Y> f) {
    return new FunctionCompositionBuilder<I, O, Y>(start, transform(end, f), executor);
  }

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param e
   *   an executor on which to perform this step
   * @param <Y>
   *   the output of this step
   * @return a builder for the next step
   */
  public <Y> FunctionCompositionBuilder<I, O, Y> andThen(final AsyncFunction<O, Y> f, Executor e) {
    return new FunctionCompositionBuilder<I, O, Y>(start, transform(end, f, e), executor);
  }

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param <Y>
   *   the output of this step
   * @return a builder for the next step
   */
  public <Y> FunctionCompositionBuilder<I, O, Y> andThen(final Function<O, Y> f) {
    return new FunctionCompositionBuilder<I, O, Y>(start, transform(end, f), executor);
  }

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param e
   *   an executor on which to perform this step
   * @param <Y>
   *   the output of this step
   * @return a builder for the next step
   */
  public <Y> FunctionCompositionBuilder<I, O, Y> andThen(final Function<O, Y> f, Executor e) {
    return new FunctionCompositionBuilder<I, O, Y>(start, transform(end, f, e), executor);
  }

  /**
   * Build an asynchronous function which is a composition of the functions added via the builder
   *
   * @return an asynchronous function representing the composition
   */
  public AsyncFunction<I, O> buildAsync() {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) throws Exception {
        start.set(input);
        return end;
      }
    };
  }

  /**
   * Build a synchronous function which is a composition of the functions added via the builder.
   *
   * @return a synchronous function representing the composition
   */
  public Function<I, O> buildSync() {
    return new Function<I, O>() {
      @Override
      public O apply(I input) {
        start.set(input);
        try {
          return end.get();
        } catch (Exception e) {
          throw propagate(e);
        }
      }
    };
  }

  /**
   * This is funny if you've seen "Dude Where's My Car"
   *
   * @return an asynchronous function representing the composition
   */
  public AsyncFunction<I, O> noAndThenAsync() {
    return buildAsync();
  }

  /**
   * This is funny if you've seen "Dude Where's My Car"
   *
   * @return a synchronous function representing the composition
   */
  public Function<I, O> noAndThen() {
    return buildSync();
  }

  public static void main(String[] args) throws Exception {

    // run some operations async
    ExecutorService executor = new ForkJoinPool();
    System.out.println(Thread.currentThread().getId());

    // This is how you call all the things
    AsyncFunction<Integer, String> f = FunctionCompositionBuilder.<Integer>builder(executor)
      .andThen(new Function<Integer, Integer>() {
        @Override
        public Integer apply(Integer input) {
          System.out.println(Thread.currentThread().getId());
          return input - 1;
        }
      })
      .andThen(new Function<Integer, Double>() {
        @Override
        public Double apply(Integer input) {
          System.out.println(Thread.currentThread().getId());
          return input * 2.0;
        }
      }, executor)
      .andThen(new AsyncFunction<Double, String>() {
        @Override
        public ListenableFuture<String> apply(Double input) throws Exception {
          System.out.println(Thread.currentThread().getId());
          return immediateFuture(Double.toString(input));
        }
      })
      .andThen(new AsyncFunction<String, String>() {
        @Override
        public ListenableFuture<String> apply(String input) throws Exception {
          System.out.println(Thread.currentThread().getId());
          return immediateFuture(input + " stuff");
        }
      }, executor)
      .buildAsync();

    // used to wait for the result before shutting down the executor service
    final CountDownLatch latch = new CountDownLatch(1);

    ListenableFuture<String> result = f.apply(2);
    addCallback(result, new FutureCallback<String>() {
      @Override
      public void onSuccess(String result) {
        System.out.println(result);
        latch.countDown();
      }

      @Override
      public void onFailure(Throwable t) {
        t.printStackTrace();
      }
    });

    latch.await();
    executor.shutdownNow();

  }

}
