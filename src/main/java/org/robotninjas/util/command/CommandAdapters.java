package org.robotninjas.util.command;

import com.google.common.util.concurrent.AsyncFunction;

public class CommandAdapters {

  public <O> AsyncFunction<Void, O> adapt(AsyncCommand<O> command) {
    return new CommandToFunctionAdapter<O>(command);
  }

  public <I, O> AsyncCommand<O> adapt(AsyncFunction<I, O> func, I input) {
    return new FunctionToCommandAdapter<I, O>(func, input);
  }

}
