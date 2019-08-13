/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class ClassMapCachingNulls<T> {
  private final Map<Class, T[]> myBackingMap;
  private final T[] myEmptyArray;
  private final List<? extends T> myOrderingArray;
  private final Map<Class, T[]> myMap = ContainerUtil.newConcurrentMap();

  ClassMapCachingNulls(@NotNull Map<Class, T[]> backingMap, T[] emptyArray, @NotNull List<? extends T> orderingArray) {
    myBackingMap = backingMap;
    myEmptyArray = emptyArray;
    myOrderingArray = orderingArray;
  }

  @Nullable
  public T[] get(Class aClass) {
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

  private T[] cache(Class aClass, List<T> result) {
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

  @Nullable
  private List<T> getFromBackingMap(Class aClass) {
    T[] value = myBackingMap.get(aClass);
    Set<T> result = null;
    if (value != null) {
      assert value.length != 0;
      result = new HashSet<>(Arrays.asList(value));
    }
    for (final Class superclass : JBIterable.of(aClass.getSuperclass()).append(aClass.getInterfaces())) {
      result = addFromUpper(result, superclass);
    }
    
    if (result == null) return null;
    return ContainerUtil.filter(myOrderingArray, result::contains);
  }

  private Set<T> addFromUpper(Set<T> value, Class superclass) {
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

  Map<Class, T[]> getBackingMap() {
    return myBackingMap;
  }

  public void clearCache() {
    myMap.clear();
  }
}