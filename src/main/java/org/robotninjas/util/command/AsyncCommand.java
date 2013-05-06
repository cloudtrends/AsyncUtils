package org.robotninjas.util.command;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncCommand<V> {

  ListenableFuture<V> execute();

}
