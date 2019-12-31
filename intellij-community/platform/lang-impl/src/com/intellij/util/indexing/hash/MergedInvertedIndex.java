// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.impl.MergedValueContainer;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

public class MergedInvertedIndex<Key, Value> implements UpdatableIndex<Key, Value, FileContent> {

  private static final Logger LOG = Logger.getInstance(MergedInvertedIndex.class);

  @NotNull
  private final HashBasedMapReduceIndex<Key, Value>[] myProvidedIndexes;
  @NotNull
  private final FileContentHashIndex myHashIndex;
  @NotNull
  public final UpdatableIndex<Key, Value, FileContent> myBaseIndex;

  @NotNull
  public static <K, V> UpdatableIndex<K, V, FileContent> wrapWithProvidedIndex(@NotNull List<ProvidedIndexExtension<K, V>> providedIndexExtensions,
                                                                               @NotNull FileBasedIndexExtension<K, V> originalExtension,
                                                                               @NotNull UpdatableIndex<K, V, FileContent> baseIndex,
                                                                               @NotNull FileContentHashIndex contentHashIndex) {
    try {
      HashBasedMapReduceIndex[] providedIndexes = new HashBasedMapReduceIndex[providedIndexExtensions.size()];
      for (int i = 0; i < providedIndexExtensions.size(); i++) {
        ProvidedIndexExtension<K, V> extension = providedIndexExtensions.get(i);
        providedIndexes[i] = extension != null && Files.exists(extension.getIndexPath()) ? HashBasedMapReduceIndex.create(extension, originalExtension, contentHashIndex, i) : null;
      }
      return new MergedInvertedIndex<>(providedIndexes, contentHashIndex, baseIndex);
    }
    catch (IOException e) {
      LOG.error(e);
      return baseIndex;
    }
  }

  public MergedInvertedIndex(@NotNull HashBasedMapReduceIndex<Key, Value>[] indexes,
                             @NotNull FileContentHashIndex hashIndex,
                             @NotNull UpdatableIndex<Key, Value, FileContent> baseIndex) {
    myProvidedIndexes = indexes;
    myHashIndex = hashIndex;
    myBaseIndex = baseIndex;
  }


  @NotNull
  public FileContentHashIndex getHashIndex() {
    return myHashIndex;
  }

  @NotNull
  public Stream<ProvidedIndexExtension<Key, Value>> getProvidedExtensions() {
    return Stream.of(myProvidedIndexes).map(index -> index.getProvidedExtension());
  }

  @NotNull
  @Override
  public Computable<Boolean> update(int inputId, @Nullable FileContent content) {
    if (content != null) {
      long hashId = FileContentHashIndexExtension.getHashId(content);
      if (hashId != FileContentHashIndexExtension.NULL_HASH_ID) {
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
    for (HashBasedMapReduceIndex<Key, Value> index : myProvidedIndexes) {
      if (index == null) continue;
      data.add(index.getData(key));
    }
    return new MergedValueContainer<>(data);
  }

  @Override
  public boolean processAllKeys(@NotNull Processor<? super Key> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter)
    throws StorageException {
    if (!myBaseIndex.processAllKeys(processor, scope, idFilter)) {
      return false;
    }
    for (HashBasedMapReduceIndex<Key, Value> index : myProvidedIndexes) {
      if (index == null) continue;
      if (!index.processAllKeys(processor, scope, idFilter)) {
        return false;
      }
    }
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
    Long hashId = myHashIndex.getHashId(fileId);
    if (hashId == null || hashId == FileContentHashIndexExtension.NULL_HASH_ID) return Collections.emptyMap();
    return myProvidedIndexes[FileContentHashIndexExtension.getIndexId(hashId)].getIndexedFileData(FileContentHashIndexExtension.getInternalHashId(hashId));
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
