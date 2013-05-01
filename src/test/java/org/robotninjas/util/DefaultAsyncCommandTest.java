package org.robotninjas.util;

import com.google.common.util.concurrent.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class DefaultAsyncCommandTest {

  @Test
  public void testDefaultExecute() throws ExecutionException, InterruptedException {

    TimeLimiter limiter = new FakeTimeLimiter();

    Callable<Integer> callable = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return 0;
      }
    };

    ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();

    DefaultAsyncCommand<Integer> command =
      new DefaultAsyncCommand<Integer>(limiter, callable, executor);

    ListenableFuture<Integer> result = command.execute();

    Assert.assertEquals((long) result.get(), 0L);
    
  }

  @Test
  public void testDefaultExecuteWithFallback() throws ExecutionException, InterruptedException {


    TimeLimiter limiter = new FakeTimeLimiter();

    Callable<Integer> callable = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        throw new Exception();
      }
    };

    ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();

    DefaultAsyncCommand<Integer> command =
      new DefaultAsyncCommand<Integer>(limiter, callable, executor);
    command.setFallback(new FutureFallback<Integer>() {
      @Override
      public ListenableFuture<Integer> create(Throwable t) throws Exception {
        return Futures.immediateFuture(0);
      }
    });

    ListenableFuture<Integer> result = command.execute();

    Assert.assertEquals((long) result.get(), 0L);

  }

  @Test
  public void testDefaultExecuteWithTimeout() throws ExecutionException, InterruptedException {

    TimeLimiter limiter = new SimpleTimeLimiter();

    Callable<Integer> callable = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        Thread.sleep(1000000);
        return -1;
      }
    };

    ListeningExecutorService executor = listeningDecorator(newCachedThreadPool());

    DefaultAsyncCommand<Integer> command =
      new DefaultAsyncCommand<Integer>(limiter, callable, executor);
    command.setTimeout(1, TimeUnit.SECONDS);

    command.execute().get();

  }

}
