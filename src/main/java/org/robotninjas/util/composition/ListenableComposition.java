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
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.Executor;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

/**
 * Build a composition of any number of functions
 *
 * @param <O>
 *   The composition and the next step's output
 */
@Immutable
public class ListenableComposition<O> {

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
  ListenableComposition(ListenableFuture<?> start, ListenableFuture<O> end, Executor executor) {
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
  public static <Z> ListenableComposition<Z> begin(ListenableFuture<Z> begin) {
    return new ListenableComposition<Z>(begin, begin, sameThreadExecutor());
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
  public static <Z> ListenableComposition<Z> begin(ListenableFuture<Z> begin, Executor e) {
    return new ListenableComposition<Z>(begin, begin, e);
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
  public <Y> ListenableComposition<Y> transform(final AsyncFunction<O, Y> f) {
    return new ListenableComposition<Y>(start, Futures.transform(end, f), executor);
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
  public <Y> ListenableComposition<Y> transform(final AsyncFunction<O, Y> f, Executor e) {
    return new ListenableComposition<Y>(start, Futures.transform(end, f, e), executor);
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
  public <Y> ListenableComposition<Y> transform(final Function<O, Y> f) {
    return new ListenableComposition<Y>(start, Futures.transform(end, f), executor);
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
  public <Y> ListenableComposition<Y> transform(final Function<O, Y> f, Executor e) {
    return new ListenableComposition<Y>(start, Futures.transform(end, f, e), executor);
  }

  /**
   * Build an asynchronous function which is a composition of the functions added via the begin
   *
   * @return an asynchronous function representing the composition
   */
  public ListenableFuture<O> build() {
    return end;
  }

}
