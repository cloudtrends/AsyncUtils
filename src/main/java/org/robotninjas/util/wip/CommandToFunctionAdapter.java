package org.robotninjas.util.wip;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

class CommandToFunctionAdapter<I, O> implements AsyncFunction<I, O> {

  private final AsyncCommand<I, O> command;

  public CommandToFunctionAdapter(AsyncCommand<I, O> command) {
    this.command = command;
  }

  @Override
  public ListenableFuture<O> apply(I input) throws Exception {
    return command.execute(input);
  }
}
