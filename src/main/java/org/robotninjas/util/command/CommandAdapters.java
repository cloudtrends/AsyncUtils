package org.robotninjas.util.command;

import com.google.common.util.concurrent.AsyncFunction;

public class CommandAdapters {

  public <O> AsyncFunction<Void, O> adapt(AsyncCommand<O> command) {
    return new CommandToFunctionAdapter<O>(command);
  }

}
