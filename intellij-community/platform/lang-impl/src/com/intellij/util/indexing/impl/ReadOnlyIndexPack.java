// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.SmartList;
import com.intellij.util.containers.BidirectionalMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.InvertedIndex;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.ValueContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class ReadOnlyIndexPack<K, V, Input, Index extends InvertedIndex<K, V, Input>> implements InvertedIndex<K, V, Input> {
  private static final Logger LOG = Logger.getInstance(ReadOnlyIndexPack.class);

  private final Function<Path, Index> myIndexGenerator;
  private final BidirectionalMap<Path, Index> myLoadedIndexes = new BidirectionalMap<>();
  private final MultiMap<Project, Index> myProject2Index = new MultiMap<>();
  private final MultiMap<Index, Project> myIndex2Project = new MultiMap<>();

  private final ReentrantReadWriteLock myPackStructureAccessLock = new ReentrantReadWriteLock();

  public ReadOnlyIndexPack(@NotNull Function<Path, Index> generator) {
    myIndexGenerator = generator;
  }

  public void attach(@NotNull Path indexPath, @NotNull Project project) {
    ReentrantReadWriteLock.WriteLock writeLock = myPackStructureAccessLock.writeLock();
    writeLock.lock();
    try {
      Index index = myLoadedIndexes.computeIfAbsent(indexPath, myIndexGenerator);
      LOG.assertTrue(!myIndex2Project.get(index).contains(project));
      LOG.assertTrue(!myProject2Index.get(project).contains(index));
    } finally {
      writeLock.unlock();
    }
  }

  public void detachIndex(@NotNull Path indexPath, @NotNull Project project) {
    ReentrantReadWriteLock.WriteLock writeLock = myPackStructureAccessLock.writeLock();
    writeLock.lock();
    try {
      Index index = myLoadedIndexes.get(indexPath);
      LOG.assertTrue(index != null);
      LOG.assertTrue(myIndex2Project.remove(index, project));
      tryCleanIndexData(index);
    } finally {
      writeLock.unlock();
    }
  }

  public void detachProject(@NotNull Project project) {
    ReentrantReadWriteLock.WriteLock writeLock = myPackStructureAccessLock.writeLock();
    writeLock.lock();
    try {
      Collection<Index> removedIndexes = myProject2Index.remove(project);
      if (removedIndexes != null) {
        for (Index index : removedIndexes) {
          tryCleanIndexData(index);
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  private void tryCleanIndexData(@NotNull Index index) {
    assert myPackStructureAccessLock.writeLock().isHeldByCurrentThread();
    if (myIndex2Project.get(index).isEmpty()) {
      myLoadedIndexes.removeValue(index);
      try {
        index.dispose();
      } catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  @NotNull
  @Override
  public ValueContainer<V> getData(@NotNull K k) throws StorageException {
    List<ValueContainer<V>> result = new SmartList<>();
    ReentrantReadWriteLock.ReadLock readLock = myPackStructureAccessLock.readLock();
    readLock.lock();
    try {
      for (InvertedIndex<K, V, Input> index : myLoadedIndexes.values()) {
        ValueContainer<V> currentData = index.getData(k);
        if (currentData.size() != 0) {
          result.add(currentData);
        }
      }
      return result.isEmpty() ? new ValueContainerImpl<>() : new MergedValueContainer<>(result);
    } finally {
      readLock.unlock();
    }
  }

  @NotNull
  @Override
  public Computable<Boolean> update(int inputId, @Nullable Input content) {
    throw new UnsupportedOperationException("index pack is read-only");
  }

  @Override
  public void flush() throws StorageException {
    ReentrantReadWriteLock.ReadLock readLock = myPackStructureAccessLock.readLock();
    readLock.lock();
    try {
      List<StorageException> exceptions = new SmartList<>();
      for (InvertedIndex<K, V, Input> index : myLoadedIndexes.values()) {
        try {
          index.flush();
        } catch (StorageException e) {
          exceptions.add(e);
        }
      }
      if (!exceptions.isEmpty()) {
        throw exceptions.get(0);
      }
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("index pack is read-only");
  }

  @Override
  public void dispose() {
    ReentrantReadWriteLock.WriteLock writeLock = myPackStructureAccessLock.writeLock();
    writeLock.lock();
    try {
      for (Project project : myProject2Index.keySet()) {
        detachProject(project);
      }
    } finally {
      writeLock.unlock();
    }
  }
}
