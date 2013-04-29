package org.robotninjas.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.util.concurrent.Futures.*;

public class Sandbox {

  private static ListenableFuture<Integer> callFlakyService(int input) {
    return immediateFuture(100);
  }

  public static void main(String[] args) {

    final Cache<Integer, Integer> cache = CacheBuilder.newBuilder().build();

    final int input = 0;
    ListenableFuture<Integer> result = withFallback(callFlakyService(input), new FutureFallback<Integer>() {
      @Override
      public ListenableFuture<Integer> create(Throwable t) throws Exception {
        return immediateFuture(cache.getIfPresent(input));
      }
    });

    addCallback(result, new FutureCallback<Integer>() {
      @Override
      public void onSuccess(Integer result) {

      }

      @Override
      public void onFailure(Throwable t) {

      }
    });


  }

}
