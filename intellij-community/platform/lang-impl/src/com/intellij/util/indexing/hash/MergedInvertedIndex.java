// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class MergedInvertedIndex<Key, Value> implements UpdatableIndex<Key, Value, FileContent> {
  @NotNull
  private final HashBasedMapReduceIndex<Key, Value> myProvidedIndex;
  @NotNull
  private final FileContentHashIndex myHashIndex;
  @NotNull
  private final UpdatableIndex<Key, Value, FileContent> myBaseIndex;

  @NotNull
  public static <Key, Value> MergedInvertedIndex<Key, Value> create(@NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                                                    @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                                                    @NotNull UpdatableIndex<Key, Value, FileContent> baseIndex)
    throws IOException {
    File file = providedExtension.getIndexPath();
    HashBasedMapReduceIndex<Key, Value> index = HashBasedMapReduceIndex.create(providedExtension, originalExtension);
    return new MergedInvertedIndex<>(index, ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getFileContentHashIndex(file), baseIndex);
  }

  public MergedInvertedIndex(@NotNull HashBasedMapReduceIndex<Key, Value> index,
                             @NotNull FileContentHashIndex hashIndex,
                             @NotNull UpdatableIndex<Key, Value, FileContent> baseIndex) {
    myProvidedIndex = index;
    myHashIndex = hashIndex;
    myBaseIndex = baseIndex;
  }

  @NotNull
  public ProvidedIndexExtension<Key, Value> getProvidedExtension() {
    return myProvidedIndex.getProvidedExtension();
  }

  @NotNull
  @Override
  public Computable<Boolean> update(int inputId, @Nullable FileContent content) {
    if (content != null) {
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
    return MergedValueContainer.merge(myBaseIndex.getData(key), myProvidedIndex.getData(key));
  }

  @Override
  public boolean processAllKeys(@NotNull Processor<? super Key> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter)
    throws StorageException {
    return myBaseIndex.processAllKeys(processor, scope, idFilter) && myProvidedIndex.processAllKeys(processor, scope, idFilter);
  }

  @NotNull
  @Override
  public Lock getReadLock() {
    return myBaseIndex.getReadLock();
  }

  @NotNull
  @Override
  public Lock getWriteLock() {
    return myBaseIndex.getWriteLock();
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
    int hashId = myHashIndex.getHashId(fileId);
    if (hashId == 0) return Collections.emptyMap();
    return myProvidedIndex.getIndexedFileData(hashId);
  }

  @Override
  public void setIndexedStateForFile(int fileId, @NotNull VirtualFile file) {
    myBaseIndex.setIndexedStateForFile(fileId, file);
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    myBaseIndex.resetIndexedStateForFile(fileId);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull VirtualFile file) {
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
