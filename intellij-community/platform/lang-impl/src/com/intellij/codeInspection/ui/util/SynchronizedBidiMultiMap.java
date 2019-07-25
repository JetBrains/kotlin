/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInspection.ui.util;

import com.intellij.util.ArrayFactory;
import com.intellij.util.ArrayUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class SynchronizedBidiMultiMap<K, V> {
  private final Map<K, V[]> myKey2Values = new THashMap<>();
  private final Map<V, K> myValue2Keys = new THashMap<>();

  public synchronized Collection<K> keys() {
    return new ArrayList<>(myKey2Values.keySet());
  }

  public synchronized boolean containsKey(K key) {
    return myKey2Values.containsKey(key);
  }

  public synchronized boolean containsValue(V value) {
    return myValue2Keys.containsKey(value);
  }

  public synchronized K getKeyFor(V value) {
    return myValue2Keys.get(value);
  }

  public synchronized V[] get(K key) {
    return myKey2Values.get(key);
  }
  public synchronized V[] getOrDefault(K key, V[] defaultValue) {
    V[] values = get(key);
    return values == null ? defaultValue : values;
  }

  public synchronized void put(K key, V... values) {
    myKey2Values.merge(key, values, (/*@NotNull*/ arr1, arr2) -> {
      V[] mergeResult = arrayFactory().create(arr1.length + arr2.length);
      System.arraycopy(arr1, 0, mergeResult, 0, arr1.length);
      System.arraycopy(arr2, 0, mergeResult, arr1.length, arr2.length);
      return mergeResult;
    });
    for (V value : values) {
      myValue2Keys.put(value, key);
    }
  }

  /**
   * @return new elements or null!
   */
  public synchronized V[] remove(K key, V value) {
    V[] newValues = myKey2Values.computeIfPresent(key, (k, vs) -> {
      V[] removed = ArrayUtil.remove(vs, value, arrayFactory());
      if (removed.length == 0) return null;
      return removed;
    });
    myValue2Keys.remove(value);
    return newValues;
  }

  public synchronized K removeValue(V value) {
    K key = myValue2Keys.get(value);
    if (key != null) {
      remove(key, value);
    }
    return key;
  }

  public synchronized V[] remove(K key) {
    V[] removed = myKey2Values.remove(key);
    if (removed != null) {
      for (V v : removed) {
        myValue2Keys.remove(v);
      }
    }
    return removed;
  }

  public synchronized Collection<V> getValues() {
    return myValue2Keys.keySet();
  }

  public synchronized boolean isEmpty() {
    return myValue2Keys.isEmpty();
  }

  @TestOnly
  public Map<K, V[]> getMap() { return myKey2Values; }

  @NotNull
  protected abstract ArrayFactory<V> arrayFactory();
}
