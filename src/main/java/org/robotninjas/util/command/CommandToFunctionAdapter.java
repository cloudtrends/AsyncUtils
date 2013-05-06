package org.robotninjas.util.command;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

public class CommandToFunctionAdapter<O> implements AsyncFunction<Void, O> {

  private final AsyncCommand<O> command;

  public CommandToFunctionAdapter(AsyncCommand<O> command) {
    this.command = command;
  }

  @Override
  public ListenableFuture<O> apply(Void input) throws Exception {
    return command.execute();
  }
}
