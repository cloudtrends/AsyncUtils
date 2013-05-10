package org.robotninjas.util.composition;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;

import java.util.List;
import java.util.concurrent.Executor;

public interface Composition<I, X, O> {

  public <Y> FunctionComposition<I, O, Y> transform(AsyncFunction<O, Y> f);

  public <Y> FunctionComposition<I, O, Y> transform(Function<O, Y> f);

  public <Y> FunctionComposition<I, O, Y> transform(AsyncFunction<O, Y> f, Executor e);

  public <Y> FunctionComposition<I, O, Y> transform(Function<O, Y> f, Executor e);

  public FunctionComposition<I, X, O> fork(FunctionComposition<O, ?, ?> composition);

  public <Z> FunctionComposition<I, X, O> fork(Iterable<AsyncFunction<O, Z>> f, Executor e);

  public <Z> FunctionComposition<I, X, O> fork(Iterable<AsyncFunction<O, Z>> f);

  public <Z> FunctionComposition<I, X, O> fork(AsyncFunction<O, Z>... f);

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> allAsList(Iterable<AsyncFunction<O, Y>> f, Executor e);

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> allAsList(Iterable<AsyncFunction<O, Y>> f);

  public <Y, Z extends List<Y>> FunctionComposition<I, X, Z> allAsList(AsyncFunction<O, Y>... f);
}
