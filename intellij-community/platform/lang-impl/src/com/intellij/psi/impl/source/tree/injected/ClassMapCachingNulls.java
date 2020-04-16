// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class ClassMapCachingNulls<T> {
  private final Map<Class<?>, T[]> myBackingMap;
  private final T[] myEmptyArray;
  private final List<? extends T> myOrderingArray;
  private final Map<Class<?>, T[]> myMap = new ConcurrentHashMap<>();

  ClassMapCachingNulls(@NotNull Map<Class<?>, T[]> backingMap, T[] emptyArray, @NotNull List<? extends T> orderingArray) {
    myBackingMap = backingMap;
    myEmptyArray = emptyArray;
    myOrderingArray = orderingArray;
  }

  T @Nullable [] get(Class<?> aClass) {
    T[] value = myMap.get(aClass);
    if (value != null) {
      if (value == myEmptyArray) {
        return null;
      }
      else {
        assert value.length != 0;
        return value;
      }
    }
    List<T> result = getFromBackingMap(aClass);
    return cache(aClass, result);
  }

  private T[] cache(Class<?> aClass, List<T> result) {
    T[] value;
    if (result == null) {
      myMap.put(aClass, myEmptyArray);
      value = null;
    }
    else {
      assert !result.isEmpty();
      value = result.toArray(myEmptyArray);
      myMap.put(aClass, value);
    }
    return value;
  }

  private @Nullable List<T> getFromBackingMap(Class<?> aClass) {
    T[] value = myBackingMap.get(aClass);
    Set<T> result = null;
    if (value != null) {
      assert value.length != 0;
      result = ContainerUtil.set(value);
    }
    for (Class<?> superclass : JBIterable.<Class<?>>of(aClass.getSuperclass()).append(aClass.getInterfaces())) {
      result = addFromUpper(result, superclass);
    }

    if (result == null) return null;
    return ContainerUtil.filter(myOrderingArray, result::contains);
  }

  private Set<T> addFromUpper(Set<T> value, Class<?> superclass) {
    T[] fromUpper = get(superclass);
    if (fromUpper != null) {
      assert fromUpper.length != 0;
      if (value == null) {
        value = new HashSet<>(fromUpper.length);
      }
      Collections.addAll(value, fromUpper);
      assert !value.isEmpty();
    }
    return value;
  }

  Map<Class<?>, T[]> getBackingMap() {
    return myBackingMap;
  }

}