package org.robotninjas.util;

import com.google.common.util.concurrent.Futures;

public class SyncCommandWrapper<V> implements Command<V> {

  private final AsyncCommand<V> async;

  SyncCommandWrapper(AsyncCommand<V> async) {
    this.async = async;
  }

  @Override
  public V execute() {
    return Futures.getUnchecked(async.execute());
  }

}
