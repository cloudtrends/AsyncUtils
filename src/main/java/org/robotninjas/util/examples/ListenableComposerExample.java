package org.robotninjas.util.examples;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.robotninjas.util.composition.ListenableComposition;

import java.util.concurrent.ExecutionException;

public class ListenableComposerExample {

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    SettableFuture<String> begin = SettableFuture.create();
    ListenableFuture<Double> end = ListenableComposition
      .begin(begin)
      .transform(new AsyncFunction<String, Integer>() {
        @Override
        public ListenableFuture<Integer> apply(String input) throws Exception {
          return Futures.immediateFuture(1);
        }
      }).transform(new AsyncFunction<Integer, String>() {
        @Override
        public ListenableFuture<String> apply(Integer input) throws Exception {
          return Futures.immediateFuture("hi");
        }
      }).transform(new AsyncFunction<String, Double>() {
        @Override
        public ListenableFuture<Double> apply(String input) throws Exception {
          return Futures.immediateFuture(2.0);
        }
      }).build();

    begin.set("");
    end.get();
  }

}
