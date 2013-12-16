package org.robotninjas.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class FluentFuture<I, X, O> implements ListenableFuture<O> {

  private final Executor executor;
  private final ListenableFuture<O> future;

  private FluentFuture(ListenableFuture<O> future, Executor executor) {
    this.future = future;
    this.executor = executor;
  }

  private FluentFuture(ListenableFuture<O> future) {
    this(future, MoreExecutors.sameThreadExecutor());
  }

  public static <Y> FluentFuture<Y, Y, Y> from(Y value) {
    return new FluentFuture<Y, Y, Y>(Futures.immediateFuture(value));
  }

  public static <Y> FluentFuture<Y, Y, Y> from(Exception exception) {
    return new FluentFuture<Y, Y, Y>(Futures.<Y>immediateFailedFuture(exception));
  }

  public static <Y> FluentFuture<Y, Y, Y> from(Y value, Executor executor) {
    return new FluentFuture<Y, Y, Y>(Futures.immediateFuture(value), executor);
  }

  public static <Y> FluentFuture<Y, Y, Y> from(ListenableFuture<Y> future) {
    return new FluentFuture<Y, Y, Y>(future);
  }

  public static <Y> FluentFuture<Y, Y, Y> from(ListenableFuture<Y> future, Executor executor) {
    return new FluentFuture<Y, Y, Y>(future, executor);
  }

  public static <Y> FluentFuture<Y, Y, List<Y>> from(ListenableFuture<Y>... futures) {
    return new FluentFuture<Y, Y, List<Y>>(Futures.allAsList(Arrays.asList(futures)));
  }

  public static <Y> FluentFuture<Y, Y, List<Y>> from(Executor executor, ListenableFuture<Y>... futures) {
    return new FluentFuture<Y, Y, List<Y>>(Futures.allAsList(Arrays.asList(futures)), executor);
  }

  public static <Y> FluentFuture<Y, Y, List<Y>> from(Iterable<ListenableFuture<Y>> futures) {
    return new FluentFuture<Y, Y, List<Y>>(Futures.allAsList(futures));
  }

  public static <Y> FluentFuture<Y, Y, List<Y>> from(Iterable<ListenableFuture<Y>> futures, Executor executor) {
    return new FluentFuture<Y, Y, List<Y>>(Futures.allAsList(futures), executor);
  }

  public <Y> FluentFuture<I, O, Y> transform(Function<O, Y> func) {
    return new FluentFuture<I, O, Y>(Futures.transform(future, func));
  }

  public <Y> FluentFuture<I, O, Y> transform(Function<O, Y> func, Executor executor) {
    return new FluentFuture<I, O, Y>(Futures.transform(future, func, executor), this.executor);
  }

  public <Y> FluentFuture<I, O, Y> transform(AsyncFunction<O, Y> func) {
    return new FluentFuture<I, O, Y>(Futures.transform(future, func));
  }

  public <Y> FluentFuture<I, O, Y> transform(AsyncFunction<O, Y> func, Executor executor) {
    return new FluentFuture<I, O, Y>(Futures.transform(future, func, executor), this.executor);
  }

  public FluentFuture<I, X, O> withFallback(FutureFallback<O> fallback) {
    return new FluentFuture<I, X, O>(Futures.withFallback(future, fallback));
  }

  public FluentFuture<I, X, O> withFallback(FutureFallback<O> fallback, Executor executor) {
    return new FluentFuture<I, X, O>(Futures.withFallback(future, fallback, executor), this.executor);
  }

  public FluentFuture<I, X, O> addCallback(FutureCallback<O> callback) {
    Futures.addCallback(future, callback);
    return this;
  }

  public FluentFuture<I, X, O> addCallback(FutureCallback<O> callback, Executor executor) {
    Futures.addCallback(future, callback, executor);
    return this;
  }

  public <E extends Exception> CheckedFuture<O, E> makeChecked(Function<Exception, E> func) {
    return Futures.makeChecked(future, func);
  }

  public FluentFuture<I, O, O> filter(final Predicate<O> predicate) {
    return transform(new AsyncFunction<O, O>() {
      @Override
      public ListenableFuture<O> apply(O input) throws Exception {
        if (!predicate.apply(input)) {
          throw new Exception("Predicate does not match");
        }
        return Futures.immediateFuture(input);
      }
    });
  }

  public <Y> FluentFuture<I, O, Zip<O, Y>> zip(final ListenableFuture<Y> other) {
    return transform(new AsyncFunction<O, Zip<O, Y>>() {
      public ListenableFuture<Zip<O, Y>> apply(final O left) throws Exception {
        return Futures.transform(other, new Function<Y, Zip<O, Y>>() {
          public Zip<O, Y> apply(Y right) {
            return new Zip<O, Y>(left, right);
          }
        });
      }
    });
  }

  @Override
  public void addListener(Runnable listener, Executor executor) {
    future.addListener(listener, executor);
  }

  @Override
  public O get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(l, timeUnit);
  }

  public <X extends Exception> O get(long l, TimeUnit timeUnit, Class<X> exceptionClass) throws X {
    return Futures.get(future, l, timeUnit, exceptionClass);
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public O get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  public <V extends Exception> O get(Class<V> exceptionClass) throws V {
    return Futures.get(future, exceptionClass);
  }

  @Override
  public boolean cancel(boolean b) {
    return future.cancel(b);
  }

  private static class Zip<L, R>
  {
    private final L left;
    private final R right;

    Zip(L left, R right) {
      this.left = left;
      this.right = right;
    }

    public L getLeft() {
      return left;
    }

    public R getRight() {
      return right;
    }

  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    Executor executor = Executors.newCachedThreadPool();
    ListenableFuture<Integer> future =
      FluentFuture.from(1000, executor)
        .transform(new Function<Integer, Double>() {
          public Double apply(Integer input) {
            return input * 2.0;
          }
        })
        .transform(new AsyncFunction<Double, Integer>() {
          public ListenableFuture<Integer> apply(Double input) throws Exception {
            return Futures.immediateFuture((int) (input + 2));
          }
        })
        .transform(new Function<Integer, Integer>() {
          public Integer apply(Integer input) {
            return input * 3;
          }
        })
        .transform(new AsyncFunction<Integer, Integer>() {
          public ListenableFuture<Integer> apply(Integer input) throws Exception {
            throw new Exception("poop");
          }
        })
        .withFallback(new FutureFallback<Integer>() {
          public ListenableFuture<Integer> create(Throwable t) throws Exception {
            return Futures.immediateFuture(1000);
          }
        })
        .addCallback(new FutureCallback<Integer>() {
          public void onSuccess(Integer result) {
            System.out.println("YAY, SUCCESS!!!");
          }

          public void onFailure(Throwable t) {
            System.out.println("I am so ashamed of myself");
          }
        });

    System.out.println(future.get());
  }
}
