// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class IntObjectMap<T> {
  private final TIntObjectHashMap<T> myObjectsMap = new TIntObjectHashMap<T>();

  public T computeIfAbsent(int objectID, @NotNull ObjectFactory<T> objectFactory) {
    T object = myObjectsMap.get(objectID);
    if (object == null) {
      object = objectFactory.newInstance();
      myObjectsMap.put(objectID, object);
      objectFactory.fill(object);
    }
    return object;
  }

  public interface ObjectFactory<T> {
    T newInstance();

    void fill(T object);
  }

  public static abstract class SimpleObjectFactory<T> implements ObjectFactory<T> {
    public abstract T create();

    @Override
    public T newInstance() {
      return create();
    }

    @Override
    public void fill(T object) { }
  }
}
