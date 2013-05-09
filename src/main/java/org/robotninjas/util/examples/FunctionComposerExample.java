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
package org.robotninjas.util.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import jsr166y.ForkJoinPool;
import org.robotninjas.util.composition.FunctionComposition;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Lists.newArrayList;
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
        System.out.println("1");
        return immediateFuture((List<String>) newArrayList("1", "2", "3"));
      }
    };
  }

  AsyncFunction<List<String>, List<URL>> locateFiles() {
    return new AsyncFunction<List<String>, List<URL>>() {
      @Override
      public ListenableFuture<List<URL>> apply(List<String> input) throws Exception {
        System.out.println("2");
        return immediateFuture((List<URL>) Lists.<URL>newArrayList());
      }
    };
  }

  AsyncFunction<List<URL>, List<File>> downloadFiles() {
    return new AsyncFunction<List<URL>, List<File>>() {
      @Override
      public ListenableFuture<List<File>> apply(List<URL> input) throws Exception {
        System.out.println("3");
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
        System.out.println("4");
        return immediateFuture(new File(""));
      }
    };
  }

  AsyncFunction<List<File>, Void> logFiles() {
    return new AsyncFunction<List<File>, Void>() {
      @Override
      public ListenableFuture<Void> apply(List<File> input) throws Exception {
        System.out.println("5");
        return immediateFuture(null);
      }
    };
  }

  AsyncFunction<File, File> copyFile(final String name) {
    return new AsyncFunction<File, File>() {
      @Override
      public ListenableFuture<File> apply(File input) throws Exception {
        System.out.println("6 " + name);
        return immediateFuture(new File("copy"));
      }
    };
  }

  public ListenableFuture<List<File>> getMergedFileForUser(String user) throws Exception {

    AsyncFunction<String, List<File>> f =
      FunctionComposition.<String>builder(mainPool)
        .transform(getFilesForUser())
        .transform(locateFiles())
        .transform(downloadFiles())
        .fork(FunctionComposition.<List<File>>builder(ioPool)
          .transform(logFiles()))
        .fork(logFiles())
        .transform(mergeFiles(), ioPool)
        .scatter(copyFile("1"), copyFile("2"), copyFile("3"))
        .buildAsync();

    return f.apply(user);
  }

  public static void main(String[] args) {
    ExecutorService mainPool = new ForkJoinPool();
    ExecutorService ioPool = newCachedThreadPool();
    FunctionComposerExample e = new FunctionComposerExample(mainPool, ioPool);
    try {
      ListenableFuture<List<File>> result = e.getMergedFileForUser("dave");
      System.out.println(result.get());
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    mainPool.shutdown();
    ioPool.shutdown();
  }

}
