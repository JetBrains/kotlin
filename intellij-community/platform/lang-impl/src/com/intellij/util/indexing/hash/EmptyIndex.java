// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.util.Computable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.snapshot.SnapshotSingleValueIndexStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

class EmptyIndex<Key, Value> implements UpdatableIndex<Key, Value, FileContent> {
  @SuppressWarnings("rawtypes")
  private static final EmptyIndex INSTANCE = new EmptyIndex();

  @SuppressWarnings("unchecked")
  static <Key, Value> EmptyIndex<Key, Value> getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean processAllKeys(@NotNull Processor<? super Key> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    return true;
  }

  @Override
  public @NotNull ReadWriteLock getLock() {
    return LOCK;
  }

  @Override
  public @NotNull Map<Key, Value> getIndexedFileData(int fileId) {
    return Collections.emptyMap();
  }

  @Override
  public void setIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }

  @Override
  public void removeTransientDataForFile(int inputId) { }

  @Override
  public void removeTransientDataForKeys(int inputId, @NotNull Collection<? extends Key> keys) { }

  @Override
  public @NotNull IndexExtension<Key, Value, FileContent> getExtension() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateWithMap(@NotNull AbstractUpdateData<Key, Value> updateData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setBufferingEnabled(boolean enabled) { }

  @Override
  public void cleanupMemoryStorage() { }

  @Override
  public void cleanupForNextTest() { }

  @Override
  public @NotNull ValueContainer<Value> getData(@NotNull Key key) {
    return SnapshotSingleValueIndexStorage.empty();
  }

  @Override
  public @NotNull Computable<Boolean> update(int inputId, @Nullable FileContent content) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void flush() { }

  @Override
  public void clear() { }

  @Override
  public void dispose() { }

  private static final Lock NO_LOCK = new Lock() {
    @Override
    public void lock() { }

    @Override
    public void lockInterruptibly() { }

    @Override
    public boolean tryLock() {
      return false;
    }

    @Override
    public boolean tryLock(long time, @NotNull TimeUnit unit) {
      return false;
    }

    @Override
    public void unlock() { }

    @NotNull
    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException();
    }
  };

  private static final ReadWriteLock LOCK = new ReadWriteLock() {
    @NotNull
    @Override
    public Lock readLock() {
      return NO_LOCK;
    }

    @Override
    public Lock writeLock() {
      throw new UnsupportedOperationException();
    }
  };
}
