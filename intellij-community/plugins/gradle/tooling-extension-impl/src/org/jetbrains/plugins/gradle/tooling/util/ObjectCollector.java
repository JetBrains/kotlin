// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class ObjectCollector<T, E extends Exception> {
  @SuppressWarnings("unchecked")
  private final TObjectIntHashMap<T> myObjectMap = new TObjectIntHashMap<T>(TObjectHashingStrategy.CANONICAL);
  private int instanceCounter = 0;

  public void add(@NotNull T object, @NotNull Processor<? extends E> consumer) throws E {
    int objectId = myObjectMap.get(object);
    boolean isNew = objectId == 0;
    if (isNew) {
      int newId = ++instanceCounter;
      myObjectMap.put(object, newId);
      objectId = newId;
    }
    consumer.process(isNew, objectId);
  }

  public interface Processor<E extends Exception> {
    void process(boolean isAdded, int objectId) throws E;
  }
}
