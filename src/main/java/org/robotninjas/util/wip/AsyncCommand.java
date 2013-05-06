package org.robotninjas.util.wip;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncCommand<I, O> {

  ListenableFuture<O> execute(I input);

}
