package org.robotninjas.util;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncCommand<V> extends Command<ListenableFuture<V>> {
}
