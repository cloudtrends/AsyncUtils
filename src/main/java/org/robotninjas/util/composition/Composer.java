package org.robotninjas.util.composition;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;

import java.util.concurrent.Executor;

public interface Composer<I, X, O> {

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param <Y>
   *   the output of this step
   * @return a begin for the next step
   */
  public <Y> Composer<I, O, Y> transform(final AsyncFunction<O, Y> f);

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
  public <Y> Composer<I, O, Y> transform(final AsyncFunction<O, Y> f, Executor e);

  /**
   * Add a step to the computation
   *
   * @param f
   *   this step
   * @param <Y>
   *   the output of this step
   * @return a begin for the next step
   */
  public <Y> Composer<I, O, Y> transform(final Function<O, Y> f);

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
  public <Y> Composer<I, O, Y> transform(final Function<O, Y> f, Executor e);
}
