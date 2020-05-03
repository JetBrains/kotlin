// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.memory;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.indexing.IdFilter;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.ValueContainer;
import com.intellij.util.indexing.VfsAwareIndexStorage;
import com.intellij.util.indexing.impl.ValueContainerImpl;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class InMemoryIndexStorage<K, V> implements VfsAwareIndexStorage<K, V> {

  private final Map<K, ValueContainerImpl<V>> myMap = new THashMap<>();

  @Override
  public boolean processKeys(@NotNull Processor<? super K> processor, GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    return myMap.keySet().stream().allMatch(processor::process);
  }

  @Override
  public void addValue(K k, int inputId, V v) {
    myMap.computeIfAbsent(k, __ -> new ValueContainerImpl<>()).addValue(inputId, v);
  }

  @Override
  public void removeAllValues(@NotNull K k, int inputId) {
    ValueContainerImpl<V> container = myMap.get(k);
    if (container == null) return;
    container.removeAssociatedValue(inputId);
    if (container.size() == 0) {
      myMap.remove(k);
    }
  }

  @Override
  public void clear() {
    myMap.clear();
  }

  @Override
  public @NotNull ValueContainer<V> read(K k) throws StorageException {
    return ObjectUtils.notNull(myMap.get(k), new ValueContainerImpl<>());
  }

  @Override
  public void clearCaches() {

  }

  @Override
  public void close() {

  }

  @Override
  public void flush() {

  }
}
