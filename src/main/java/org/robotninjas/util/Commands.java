package org.robotninjas.util;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;

public class Commands {

  public static <I, O> AsyncFunction<I, O> adaptCommand(final AsyncCommand<O> command) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) throws Exception {
        return command.execute();
      }
    };
  }

  public static <I, O> Function<I, O> adaptCommand(final Command<O> command) {
    return new Function<I, O>() {
      @Nullable
      @Override
      public O apply(@Nullable I input) {
        return command.execute();
      }
    };
  }

}
