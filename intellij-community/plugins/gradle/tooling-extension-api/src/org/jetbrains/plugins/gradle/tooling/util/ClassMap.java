// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// duplicate of com.intellij.util.containers.ClassMap
public class ClassMap<T> {
  protected final Map<Class, T> myMap;

  public ClassMap() {
    this(new HashMap<Class, T>());
  }
  protected ClassMap(@NotNull Map<Class, T> map) {
    myMap = map;
  }

  public void put(@NotNull Class aClass, T value) {
    myMap.put(aClass, value);
  }
  public void remove(@NotNull Class aClass) {
    myMap.remove(aClass);
  }

  public T get(@NotNull Class aClass) {
    T t = myMap.get(aClass);
    if (t != null) {
      return t;
    }
    for (final Class aClass1 : aClass.getInterfaces()) {
      t = get(aClass1);
      if (t != null) {
        myMap.put(aClass, t);
        return t;
      }
    }
    final Class superclass = aClass.getSuperclass();
    if (superclass != null) {
      t = get(superclass);
      if (t != null) {
        myMap.put(aClass, t);
        return t;
      }
    }
    return null;
  }

  @NotNull
  public final Collection<T> values() {
    return myMap.values();
  }

  public void clear() {
    myMap.clear();
  }
}
