// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IntIntFunction;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.UpdatableValueContainer;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import com.intellij.util.io.PersistentEnumeratorBase;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

class HashBasedMapReduceIndex<Key, Value> extends VfsAwareMapReduceIndex<Key, Value, FileContent> {
  @NotNull
  private final ProvidedIndexExtension<Key, Value> myProvidedExtension;

  @NotNull
  static <Key, Value> HashBasedMapReduceIndex<Key, Value> create(@NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                                                 @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                                                 @NotNull FileContentHashIndex hashIndex,
                                                                 int providedIndexId)
    throws IOException {
    Path file = providedExtension.getIndexPath();
    return new HashBasedMapReduceIndex<>(file, originalExtension, providedExtension, hashIndex, providedIndexId);
  }

  private HashBasedMapReduceIndex(@NotNull Path baseFile,
                                  @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                  @NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                  @NotNull FileContentHashIndex hashIndex,
                                  int providedIndexId) throws IOException {
    super(originalExtension, createStorage(baseFile, originalExtension, providedExtension, hashIndex.toHashIdToFileIdFunction(providedIndexId)), null, null, null, null);
    myProvidedExtension = providedExtension;
  }

  @NotNull
  public ProvidedIndexExtension<Key, Value> getProvidedExtension() {
    return myProvidedExtension;
  }

  private static <Key, Value> IndexStorage<Key, Value> createStorage(@NotNull Path baseFile,
                                                                     @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                                                     @NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                                                     @NotNull IntIntFunction hashToFileId) throws IOException {
    return new MyMapIndexStorage<>(baseFile, originalExtension, providedExtension, hashToFileId);
  }

  private static class MyMapIndexStorage<Key, Value>
    extends MapIndexStorage<Key, Value>
    implements VfsAwareIndexStorage<Key, Value> {
    public MyMapIndexStorage(Path baseFile,
                             FileBasedIndexExtension<Key, Value> originalExtension,
                             ProvidedIndexExtension<Key, Value> providedExtension,
                             IntIntFunction hashToFileId) throws IOException {
      super(baseFile.resolve(originalExtension.getName().getName()), providedExtension.createKeyDescriptor(),
            providedExtension.createValueExternalizer(), originalExtension.getCacheSize(), originalExtension.keyIsUniqueForIndexedFile(),
            true, true, hashToFileId);
    }

    @Override
    protected void checkCanceled() {
      ProgressManager.checkCanceled();
    }

    @Override
    public boolean processKeys(@NotNull Processor<? super Key> processor, GlobalSearchScope scope, @Nullable IdFilter idFilter)
      throws StorageException {
      l.lock();
      try {
        myCache.clear(); // this will ensure that all new keys are made into the map
        return doProcessKeys(processor);
      }
      catch (IOException e) {
        throw new StorageException(e);
      }
      catch (RuntimeException e) {
        return unwrapCauseAndRethrow(e);
      }
      finally {
        l.unlock();
      }
    }

    private boolean doProcessKeys(@NotNull Processor<? super Key> processor) throws IOException {
      return myMap instanceof PersistentHashMap && PersistentEnumeratorBase.inlineKeyStorage(myKeyDescriptor)
             // process keys and check that they're already present in map because we don't have separated key storage we must check keys
             ? ((PersistentHashMap<Key, UpdatableValueContainer<Value>>)myMap).processKeysWithExistingMapping(processor)
             // optimization: process all keys, some of them might be already deleted but we don't care. We just read key storage file here
             : myMap.processKeys(processor);
    }
  }
}
