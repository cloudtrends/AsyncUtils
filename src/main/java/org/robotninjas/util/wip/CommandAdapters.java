package org.robotninjas.util.wip;

import com.google.common.util.concurrent.AsyncFunction;

public class CommandAdapters {

  public <I, O>AsyncFunction<I, O> adapt(AsyncCommand<I, O> command) {
    return new CommandToFunctionAdapter<I, O>(command);
  }

}
