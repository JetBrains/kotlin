/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.util;

import org.jetbrains.plugins.gradle.model.ExternalDependency;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Vladislav.Soroka
 */
public class DependencyTraverser implements Iterable<ExternalDependency> {
  private final Collection<ExternalDependency> collection;

  public DependencyTraverser(Collection<ExternalDependency> c) {
    collection = c;
  }

  @Override
  public Iterator<ExternalDependency> iterator() {
    return new Itr(collection);
  }

  private static class Itr implements Iterator<ExternalDependency> {
    Queue<ExternalDependency> queue;

    Itr(Collection<ExternalDependency> c) {
      queue = new LinkedList<ExternalDependency>(c);
    }

    @Override
    public boolean hasNext() {
      return !queue.isEmpty();
    }

    @Override
    public ExternalDependency next() {
      ExternalDependency dependency = queue.remove();
      queue.addAll(dependency.getDependencies());
      return dependency;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}