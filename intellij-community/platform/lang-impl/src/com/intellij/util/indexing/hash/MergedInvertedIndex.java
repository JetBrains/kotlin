// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.util.Computable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.impl.MergedValueContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public class MergedInvertedIndex<Key, Value> implements UpdatableIndex<Key, Value, FileContent> {
  private final @NotNull SharedIndexChunkConfiguration mySharedIndexChunkConfiguration;

  @NotNull
  private final FileContentHashIndex myHashIndex;
  @NotNull
  public final UpdatableIndex<Key, Value, FileContent> myBaseIndex;
  private final ID<Key, Value> myId;

  public MergedInvertedIndex(@NotNull ID<Key, Value> id,
                             @NotNull FileContentHashIndex hashIndex,
                             @NotNull UpdatableIndex<Key, Value, FileContent> baseIndex) {
    myId = id;
    mySharedIndexChunkConfiguration = SharedIndexChunkConfiguration.getInstance();
    myHashIndex = hashIndex;
    myBaseIndex = baseIndex;
  }


  @NotNull
  public FileContentHashIndex getHashIndex() {
    return myHashIndex;
  }

  @NotNull
  @Override
  public Computable<Boolean> update(int inputId, @Nullable FileContent content) {
    if (content != null) {
      long hashId = FileContentHashIndexExtension.getHashId(content);
      if (hashId != FileContentHashIndexExtension.NULL_HASH_ID &&
          mySharedIndexChunkConfiguration.getChunk(myId, FileContentHashIndexExtension.getIndexId(hashId)) != null) {
        return () -> Boolean.TRUE;
      }
      //TODO if content == null
      Computable<Boolean> update = myHashIndex.update(inputId, content);
      if (!((FileContentHashIndex.HashIndexUpdateComputable)update).isEmptyInput()) return update;
    }
    return myBaseIndex.update(inputId, content);
  }


  @Override
  public void updateWithMap(@NotNull AbstractUpdateData<Key, Value> updateData) throws StorageException {
    int fileId = updateData.getInputId();
    if (myHashIndex.getHashId(fileId) != 0) {
      return;
    }
    myBaseIndex.updateWithMap(updateData);
  }

  @Override
  public void setBufferingEnabled(boolean enabled) {
    myBaseIndex.setBufferingEnabled(enabled);
  }

  @Override
  public void cleanupMemoryStorage() {
    myBaseIndex.cleanupMemoryStorage();
  }

  @TestOnly
  @Override
  public void cleanupForNextTest() {
    myBaseIndex.cleanupForNextTest();
  }

  @NotNull
  @Override
  public ValueContainer<Value> getData(@NotNull Key key) throws StorageException {
    List<ValueContainer<Value>> data = new SmartList<>();
    data.add(myBaseIndex.getData(key));
    mySharedIndexChunkConfiguration.processChunks(myId, index -> {
      try {
        data.add(index.getData(key));
        return true;
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    });
    return new MergedValueContainer<>(data);
  }

  @Override
  public boolean processAllKeys(@NotNull Processor<? super Key> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter)
    throws StorageException {
    if (!myBaseIndex.processAllKeys(processor, scope, idFilter)) {
      return false;
    }
    mySharedIndexChunkConfiguration.processChunks(myId, index -> {
      try {
        return index.processAllKeys(processor, scope, idFilter);
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    });
    return true;
  }

  @NotNull
  @Override
  public ReadWriteLock getLock() {
    return myBaseIndex.getLock();
  }

  @NotNull
  @Override
  public Map<Key, Value> getIndexedFileData(int fileId) throws StorageException {
    Map<Key, Value> data = myBaseIndex.getIndexedFileData(fileId);
    if (!data.isEmpty()) return data;
    long hashId = myHashIndex.getHashId(fileId);
    if (hashId == FileContentHashIndexExtension.NULL_HASH_ID) return Collections.emptyMap();
    int chunkId = FileContentHashIndexExtension.getIndexId(hashId);
    int internalHashId = FileContentHashIndexExtension.getInternalHashId(hashId);
    return mySharedIndexChunkConfiguration.getChunk(myId, chunkId).getIndexedFileData(internalHashId);
  }

  @Override
  public void setIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    myBaseIndex.setIndexedStateForFile(fileId, file);
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    myBaseIndex.resetIndexedStateForFile(fileId);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    return myBaseIndex.isIndexedStateForFile(fileId, file);
  }

  @Override
  public long getModificationStamp() {
    return myBaseIndex.getModificationStamp();
  }

  @Override
  public void removeTransientDataForFile(int inputId) {
    myBaseIndex.removeTransientDataForFile(inputId);
  }

  @Override
  public void removeTransientDataForKeys(int inputId, @NotNull Collection<? extends Key> keys) {
    myBaseIndex.removeTransientDataForKeys(inputId, keys);
  }

  @NotNull
  @Override
  public IndexExtension<Key, Value, FileContent> getExtension() {
    return myBaseIndex.getExtension();
  }


  @Override
  public void clear() throws StorageException {
    myBaseIndex.clear();
  }

  @Override
  public void dispose() {
    myBaseIndex.dispose();
  }

  @Override
  public void flush() throws StorageException {
    myBaseIndex.flush();
  }
}
