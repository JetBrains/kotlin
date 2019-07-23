// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import com.intellij.openapi.util.Getter;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class IntObjectMap<T> {
  private final TIntObjectHashMap<T> myObjectsMap = new TIntObjectHashMap<T>();

  public T computeIfAbsent(int objectID, @NotNull Getter<? extends T> objectFactory) {
    T object = myObjectsMap.get(objectID);
    if (object == null) {
      object = objectFactory.get();
      myObjectsMap.put(objectID, object);
    }
    return object;
  }
}
