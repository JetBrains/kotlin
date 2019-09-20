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

package com.intellij.util.indexing;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.impl.ChangeTrackingValueContainer;
import com.intellij.util.indexing.impl.DebugAssertions;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.UpdatableValueContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * This storage is needed for indexing yet unsaved data without saving those changes to 'main' backend storage
 *
 * @author Eugene Zhuravlev
 */
public class MemoryIndexStorage<Key, Value> implements VfsAwareIndexStorage<Key, Value> {
  private final Map<Key, ChangeTrackingValueContainer<Value>> myMap = new HashMap<>();
  @NotNull
  private final VfsAwareIndexStorage<Key, Value> myBackendStorage;
  private final List<BufferingStateListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  @NotNull
  private final ID<?, ?> myIndexId;
  private boolean myBufferingEnabled;

  public interface BufferingStateListener {
    void bufferingStateChanged(boolean newState);

    void memoryStorageCleared();
  }

  public MemoryIndexStorage(@NotNull IndexStorage<Key, Value> backend, @NotNull ID<?, ?> indexId) {
    myBackendStorage = (VfsAwareIndexStorage<Key, Value>)backend;
    myIndexId = indexId;
  }

  @NotNull
  VfsAwareIndexStorage<Key, Value> getBackendStorage() {
    return myBackendStorage;
  }

  public void addBufferingStateListener(@NotNull BufferingStateListener listener) {
    myListeners.add(listener);
  }

  public void setBufferingEnabled(boolean enabled) {
    final boolean wasEnabled = myBufferingEnabled;
    assert wasEnabled != enabled;

    myBufferingEnabled = enabled;
    for (BufferingStateListener listener : myListeners) {
      listener.bufferingStateChanged(enabled);
    }
  }

  public boolean clearMemoryMap() {
    boolean modified = !myMap.isEmpty();
    myMap.clear();
    return modified;
  }

  public boolean clearMemoryMapForId(Key key, int fileId) {
    ChangeTrackingValueContainer<Value> container = myMap.get(key);
    if (container != null) {
      container.dropAssociatedValue(fileId);
      return true;
    }
    return false;
  }

  public void fireMemoryStorageCleared() {
    for (BufferingStateListener listener : myListeners) {
      listener.memoryStorageCleared();
    }
  }

  @Override
  public void clearCaches() {
    try {
      if (myMap.size() == 0) return;

      if (DebugAssertions.DEBUG) {
        String message = "Dropping caches for " + myIndexId + ", number of items:" + myMap.size();
        FileBasedIndexImpl.LOG.info(message);
      }

      for (ChangeTrackingValueContainer<Value> v : myMap.values()) {
        v.dropMergedData();
      }
    } finally {
      myBackendStorage.clearCaches();
    }
  }

  @Override
  public void close() throws StorageException {
    myBackendStorage.close();
  }

  @Override
  public void clear() throws StorageException {
    clearMemoryMap();
    myBackendStorage.clear();
  }

  @Override
  public void flush() throws IOException {
    myBackendStorage.flush();
  }

  @Override
  public boolean processKeys(@NotNull final Processor<? super Key> processor, GlobalSearchScope scope, IdFilter idFilter) throws StorageException {
    final Set<Key> stopList = new HashSet<>();

    Processor<Key> decoratingProcessor = key -> {
      if (stopList.contains(key)) return true;

      final UpdatableValueContainer<Value> container = myMap.get(key);
      if (container != null && container.size() == 0) {
        return true;
      }
      return processor.process(key);
    };

    for (Key key : myMap.keySet()) {
      if (!decoratingProcessor.process(key)) {
        return false;
      }
      stopList.add(key);
    }
    return myBackendStorage.processKeys(stopList.isEmpty() && myMap.isEmpty() ? processor : decoratingProcessor, scope, idFilter);
  }

  @Override
  public void addValue(final Key key, final int inputId, final Value value) throws StorageException {
    if (myBufferingEnabled) {
      getMemValueContainer(key).addValue(inputId, value);
      return;
    }
    final ChangeTrackingValueContainer<Value> valueContainer = myMap.get(key);
    if (valueContainer != null) {
      valueContainer.dropMergedData();
    }

    myBackendStorage.addValue(key, inputId, value);
  }

  @Override
  public void removeAllValues(@NotNull Key key, int inputId) throws StorageException {
    if (myBufferingEnabled) {
      getMemValueContainer(key).removeAssociatedValue(inputId);
      return;
    }
    final ChangeTrackingValueContainer<Value> valueContainer = myMap.get(key);
    if (valueContainer != null) {
      valueContainer.dropMergedData();
    }

    myBackendStorage.removeAllValues(key, inputId);
  }

  private UpdatableValueContainer<Value> getMemValueContainer(final Key key) {
    ChangeTrackingValueContainer<Value> valueContainer = myMap.get(key);
    if (valueContainer == null) {
      valueContainer = new ChangeTrackingValueContainer<>(new ChangeTrackingValueContainer.Initializer<Value>() {
        @Override
        public Object getLock() {
          return this;
        }

        @Override
        public ValueContainer<Value> compute() {
          try {
            return myBackendStorage.read(key);
          }
          catch (StorageException e) {
            throw new RuntimeException(e);
          }
        }
      });
      myMap.put(key, valueContainer);
    }
    return valueContainer;
  }

  @Override
  @NotNull
  public ValueContainer<Value> read(final Key key) throws StorageException {
    final ValueContainer<Value> valueContainer = myMap.get(key);
    if (valueContainer != null) {
      return valueContainer;
    }

    return myBackendStorage.read(key);
  }
}
