/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.compiler.instrumentation;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Invoked via reflection from InstrumentationClassFinder
 */
public class JrtLoader extends InstrumentationClassFinder.ClassFinderClasspath.Loader{
  private static final URI ROOT_URI = URI.create("jrt:/");
  private static final InstrumentationClassFinder.Resource NULL_RESOURCE = ()-> null;
  private List<Path> myRoots;
  private final Map<String, InstrumentationClassFinder.Resource> myCache = new HashMap<>();

  public JrtLoader(URL url, int index) {
    super(url, index);
  }

  @Override
  public InstrumentationClassFinder.Resource getResource(String name) {
    final InstrumentationClassFinder.Resource cached = myCache.get(name);
    if (cached != null) {
      return cached != NULL_RESOURCE ? cached : null;
    }
    try {
      for (Path root : getRoots()) {
        final Path path = root.resolve(name);
        if (Files.exists(path)) {
          final InstrumentationClassFinder.Resource res = () -> Files.newInputStream(path, StandardOpenOption.READ);
          myCache.put(name, res);
          return res;
        }
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    myCache.put(name, NULL_RESOURCE);
    return null;
  }

  @Override
  public synchronized void  releaseResources() {
    myCache.clear();
    final List<Path> roots = myRoots;
    if (roots != null) {
      try {
        if (!roots.isEmpty()) {
          roots.iterator().next().getFileSystem().close();
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      finally {
        roots.clear();
        myRoots = null;
      }
    }
  }

  private synchronized List<Path> getRoots() throws IOException {
    final List<Path> cached = myRoots;
    if (cached != null) {
      return cached;
    }
    final FileSystem fs = FileSystems.newFileSystem(ROOT_URI, Collections.singletonMap("java.home", getBaseURL().getPath()));
    final List<Path> roots = new ArrayList<>();
    final Path modulesDir = fs.getPath("modules");
    Files.walkFileTree(modulesDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!modulesDir.equals(dir)) {
          roots.add(dir);
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }
    });
    myRoots = roots;
    return roots;
  }
}
