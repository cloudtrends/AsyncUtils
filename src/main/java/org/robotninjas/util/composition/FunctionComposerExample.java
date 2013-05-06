package org.robotninjas.util.composition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import jsr166y.ForkJoinPool;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class FunctionComposerExample {

  private final Executor mainPool;
  private final Executor ioPool;

  public FunctionComposerExample(Executor mainPool, Executor ioPool) {
    this.mainPool = mainPool;
    this.ioPool = ioPool;
  }

  ListenableFuture<File> downloadFile(URL file) {
    return immediateFuture(new File("local/copy"));
  }

  AsyncFunction<String, List<String>> getFilesForUser() {
    return new AsyncFunction<String, List<String>>() {
      @Override
      public ListenableFuture<List<String>> apply(String input) throws Exception {
        return immediateFuture((List<String>) Lists.newArrayList("1", "2", "3"));
      }
    };
  }

  AsyncFunction<List<String>, List<URL>> locateFiles() {
    return new AsyncFunction<List<String>, List<URL>>() {
      @Override
      public ListenableFuture<List<URL>> apply(List<String> input) throws Exception {
        return immediateFuture((List<URL>) Lists.<URL>newArrayList());
      }
    };
  }

  AsyncFunction<List<URL>, List<File>> downloadFiles() {
    return new AsyncFunction<List<URL>, List<File>>() {
      @Override
      public ListenableFuture<List<File>> apply(List<URL> input) throws Exception {
        ImmutableList.Builder futures = ImmutableList.builder();
        for (URL file : input) {
          futures.add(downloadFile(file));
        }
        return allAsList(futures.build());
      }
    };
  }

  AsyncFunction<List<File>, File> mergeFiles() {
    return new AsyncFunction<List<File>, File>() {
      @Override
      public ListenableFuture<File> apply(List<File> input) throws Exception {
        return immediateFuture(new File(""));
      }
    };
  }

  public ListenableFuture<File> getMergedFileForUser(String user) throws Exception {
    AsyncFunction<String, File> f =
      FunctionComposer.<String>builder(mainPool)
        .transform(getFilesForUser())
        .transform(locateFiles())
        .transform(downloadFiles())
        .transform(mergeFiles(), ioPool)
        .buildAsync();
    return f.apply(user);
  }

  public static void main(String[] args) {
    ExecutorService mainPool = new ForkJoinPool();
    ExecutorService ioPool = newCachedThreadPool();
    FunctionComposerExample e = new FunctionComposerExample(mainPool, ioPool);
    try {
      ListenableFuture<File> result = e.getMergedFileForUser("dave");
      System.out.println(result.get());
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    mainPool.shutdownNow();
    ioPool.shutdownNow();
  }

}
