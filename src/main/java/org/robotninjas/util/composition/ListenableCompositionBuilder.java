package org.robotninjas.util.composition;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

/**
 * Build a composition of any number of functions
 *
 * @param <O>
 *   The composition and the next step's output
 */
@Immutable
public class ListenableCompositionBuilder<O> {

  private final ListenableFuture<?> start;
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
  ListenableCompositionBuilder(ListenableFuture<?> start, ListenableFuture<O> end, Executor executor) {
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
  public static <Z> ListenableCompositionBuilder<Z> begin(ListenableFuture<Z> begin) {
    return new ListenableCompositionBuilder<Z>(begin, begin, sameThreadExecutor());
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
  public static <Z> ListenableCompositionBuilder<Z> begin(ListenableFuture<Z> begin, Executor e) {
    return new ListenableCompositionBuilder<Z>(begin, begin, e);
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
  public <Y> ListenableCompositionBuilder<Y> transform(final AsyncFunction<O, Y> f) {
    return new ListenableCompositionBuilder<Y>(start, Futures.transform(end, f), executor);
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
  public <Y> ListenableCompositionBuilder<Y> transform(final AsyncFunction<O, Y> f, Executor e) {
    return new ListenableCompositionBuilder<Y>(start, Futures.transform(end, f, e), executor);
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
  public <Y> ListenableCompositionBuilder<Y> transform(final Function<O, Y> f) {
    return new ListenableCompositionBuilder<Y>(start, Futures.transform(end, f), executor);
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
  public <Y> ListenableCompositionBuilder<Y> transform(final Function<O, Y> f, Executor e) {
    return new ListenableCompositionBuilder<Y>(start, Futures.transform(end, f, e), executor);
  }

  /**
   * Build an asynchronous function which is a composition of the functions added via the begin
   *
   * @return an asynchronous function representing the composition
   */
  public ListenableFuture<O> build() {
    return end;
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    SettableFuture<String> begin = SettableFuture.create();
    ListenableFuture<Double> end = ListenableCompositionBuilder.begin(begin)
      .transform(new AsyncFunction<String, Integer>() {
        @Override
        public ListenableFuture<Integer> apply(String input) throws Exception {
          return null;
        }
      }).transform(new AsyncFunction<Integer, String>() {
        @Override
        public ListenableFuture<String> apply(Integer input) throws Exception {
          return null;
        }
      }).transform(new AsyncFunction<String, Double>() {
        @Override
        public ListenableFuture<Double> apply(String input) throws Exception {
          return null;
        }
      }).build();

    end.get();
  }

}
