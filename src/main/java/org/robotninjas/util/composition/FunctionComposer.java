package org.robotninjas.util.composition;

import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import jsr166y.ForkJoinPool;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.*;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.util.concurrent.Futures.*;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static java.util.concurrent.Executors.newFixedThreadPool;

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
public class FunctionComposer<I, X, O> implements Composer<I, X, O> {

  private final SettableFuture<I> start;
  private final ListenableFuture<O> end;
  private final Executor executor;

  /**
   * Construct a begin for the next step
   *
   * @param start
   *   the composition's starting settable future
   * @param end
   *   the ListenableFuture which returns the final result
   */
  FunctionComposer(SettableFuture<I> start, ListenableFuture<O> end, Executor executor) {
    this.start = start;
    this.end = end;
    this.executor = executor;
  }

  /**
   * Construct an initial begin
   *
   * @param <Z>
   *   the input to the composed function
   * @return a begin for the next step of the composition
   */
  public static <Z> FunctionComposer<Z, Z, Z> builder() {
    final SettableFuture<Z> begin = SettableFuture.create();
    return new FunctionComposer<Z, Z, Z>(begin, begin, sameThreadExecutor());
  }

  /**
   * Construct an initial begin
   *
   * @param e
   *   an executor to run all transform operations
   * @param <Z>
   *   the input to the composed function
   * @return a begin for the next step of the composition
   */
  public static <Z> FunctionComposer<Z, Z, Z> builder(Executor e) {
    final SettableFuture<Z> begin = SettableFuture.create();
    return new FunctionComposer<Z, Z, Z>(begin, begin, e);
  }

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param <Y>
   *   the output of this step
   * @return a begin for the next step
   */
  public <Y> FunctionComposer<I, O, Y> transform(final AsyncFunction<O, Y> f) {
    return new FunctionComposer<I, O, Y>(start, Futures.transform(end, f), executor);
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
   * @return a begin for the next step
   */
  public <Y> FunctionComposer<I, O, Y> transform(final AsyncFunction<O, Y> f, Executor e) {
    return new FunctionComposer<I, O, Y>(start, Futures.transform(end, f, e), executor);
  }

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param <Y>
   *   the output of this step
   * @return a begin for the next step
   */
  public <Y> FunctionComposer<I, O, Y> transform(final Function<O, Y> f) {
    return new FunctionComposer<I, O, Y>(start, Futures.transform(end, f), executor);
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
   * @return a begin for the next step
   */
  public <Y> FunctionComposer<I, O, Y> transform(final Function<O, Y> f, Executor e) {
    return new FunctionComposer<I, O, Y>(start, Futures.transform(end, f, e), executor);
  }

  /**
   * Build an asynchronous function which is a composition of the functions added via the begin
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
   * Build a synchronous function which is a composition of the functions added via the begin.
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

  public static void main(String[] args) throws Exception {

    // run some operations async
    final ExecutorService mainPool = new ForkJoinPool();
    final ExecutorService fixedPool = newFixedThreadPool(1);

    // This is how you call all the things
    final AsyncFunction<Integer, String> f =
      FunctionComposer.<Integer>builder(mainPool)
        .transform(new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer input) {
            return input - 1;
          }
        })
        .transform(new Function<Integer, Double>() {
          @Override
          public Double apply(Integer input) {
            return input * 2.0;
          }
        }, fixedPool)
        .transform(new AsyncFunction<Double, String>() {
          @Override
          public ListenableFuture<String> apply(Double input) throws Exception {
            return immediateFuture(Double.toString(input));
          }
        })
        .transform(new AsyncFunction<String, String>() {
          @Override
          public ListenableFuture<String> apply(String input) throws Exception {
            return immediateFuture(input + " stuff");
          }
        }, fixedPool)
        .buildAsync();

    // used to wait for the result before shutting down the executor service
    final CountDownLatch latch = new CountDownLatch(1);

    final ListenableFuture<String> result = f.apply(2);

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
    mainPool.shutdownNow();
    fixedPool.shutdownNow();

  }

}
