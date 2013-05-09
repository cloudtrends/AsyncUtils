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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static com.google.common.util.concurrent.Futures.immediateFuture;

public class ChainingExample {

  private final Executor mainPool;
  private final Executor ioPool;

  public ChainingExample(Executor mainPool, Executor ioPool) {
    this.mainPool = mainPool;
    this.ioPool = ioPool;
  }

  public ListenableFuture<File> getMergedFileForUser(String user) throws Exception {
    ListenableFuture<List<String>> files = getFilesForUser("dave");
    ListenableFuture<List<URL>> urls =
      Futures.transform(files, locateFiles(), mainPool);
    ListenableFuture<List<File>> downloadedFiles =
      Futures.transform(urls, downloadFiles(), mainPool);
    return Futures.transform(downloadedFiles, mergeFiles(), ioPool);
  }

  ListenableFuture<List<String>> getFilesForUser(String user) {
    return immediateFuture((List<String>) Collections.EMPTY_LIST);
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
        return immediateFuture((List<File>) Collections.EMPTY_LIST);
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

}
