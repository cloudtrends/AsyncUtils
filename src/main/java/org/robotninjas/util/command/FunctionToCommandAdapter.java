package org.robotninjas.util.command;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

class FunctionToCommandAdapter<I, O> implements AsyncCommand<O> {

  final AsyncFunction<I, O> func;
  final I input;

  public FunctionToCommandAdapter(AsyncFunction<I, O> func, I input) {
    this.func = func;
    this.input = input;
  }

  @Override
  public ListenableFuture<O> execute() {
    try {
      return func.apply(input);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
