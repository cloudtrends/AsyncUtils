package org.robotninjas.util;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.Iterator;

public class CommandChainBuilder {

  private final ImmutableList.Builder<AsyncCommand<?>> commandsBuilder = ImmutableList.builder();

  public CommandChainBuilder andThen(AsyncCommand<?> command) {
    commandsBuilder.add(command);
    return this;
  }

  public AsyncCommand<?> buildChain() {
    final ImmutableList<AsyncCommand<?>> commands = commandsBuilder.build();
    return new ChainExecutor(commands.iterator());
  }

  private static class ChainExecutor implements AsyncCommand<Void> {

    private final Iterator<AsyncCommand<?>> commands;
    private final SettableFuture<Void> last;

    private ChainExecutor(Iterator<AsyncCommand<?>> commands) {
      this.commands = commands;
      this.last = SettableFuture.create();
    }

    private void doChain() {

      if (!commands.hasNext()) {
        last.set(null);
        return;
      }

      ListenableFuture<?> previousResult = commands.next().execute();
      Futures.addCallback(previousResult, new FutureCallback<Object>() {
        @Override
        public void onSuccess(Object result) {
          doChain();
        }

        @Override
        public void onFailure(Throwable t) {
          last.setException(t);
        }
      });

    }

    @Override
    public ListenableFuture<Void> execute() {
      doChain();
      return last;
    }
  }

}
