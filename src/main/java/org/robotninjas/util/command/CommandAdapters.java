/**
 *    Copyright 2013 David Rusek <dave.rusek@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.robotninjas.util.command;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

public class CommandAdapters {

  public <O> AsyncFunction<?, O> adapt(AsyncCommand<O> command) {
    return new CommandToFunctionAdapter<O>(command);
  }

  private static class CommandToFunctionAdapter<O> implements AsyncFunction<Void, O> {

    private final AsyncCommand<O> command;

    public CommandToFunctionAdapter(AsyncCommand<O> command) {
      this.command = command;
    }

    @Override
    public ListenableFuture<O> apply(Void input) throws Exception {
      return command.execute();
    }
  }

  public <I, O> AsyncCommand<O> adapt(AsyncFunction<I, O> func, I input) {
    return new FunctionToCommandAdapter<I, O>(func, input);
  }

  private static class FunctionToCommandAdapter<I, O> implements AsyncCommand<O> {

    private final AsyncFunction<I, O> func;
    private final I input;

    private FunctionToCommandAdapter(AsyncFunction<I, O> func, I input) {
      this.func = func;
      this.input = input;
    }

    @Override
    public ListenableFuture<O> execute() throws Exception {
      return func.apply(input);
    }
  }

}
