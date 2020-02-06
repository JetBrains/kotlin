// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.UpdatableValueContainer;
import com.intellij.util.indexing.impl.ValueContainerInputRemapping;
import com.intellij.util.indexing.provided.SharedIndexExtension;
import com.intellij.util.io.PersistentEnumeratorBase;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

@ApiStatus.Internal
public class HashBasedMapReduceIndex<Key, Value> extends VfsAwareMapReduceIndex<Key, Value> {
  HashBasedMapReduceIndex(@NotNull SharedIndexChunk chunk,
                          @NotNull SharedIndexExtension<Key, Value> sharedExtension,
                          @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                          @NotNull FileContentHashIndex hashIndex) throws IOException {
    super(originalExtension, createStorage(chunk.getPath(), originalExtension, sharedExtension, hashIndex.getHashIdToFileIdsFunction(chunk.getChunkId())), null, null, null, null);
  }

  private static <Key, Value> IndexStorage<Key, Value> createStorage(@NotNull Path baseFile,
                                                                     @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                                                     @NotNull SharedIndexExtension<Key, Value> providedExtension,
                                                                     @NotNull ValueContainerInputRemapping hashToFileId) throws IOException {
    return new MyMapIndexStorage<>(baseFile.resolve(originalExtension.getName().getName()), originalExtension, providedExtension, hashToFileId);
  }

  private static class MyMapIndexStorage<Key, Value>
    extends MapIndexStorage<Key, Value>
    implements VfsAwareIndexStorage<Key, Value> {
    private MyMapIndexStorage(Path baseFile,
                              FileBasedIndexExtension<Key, Value> originalExtension,
                              SharedIndexExtension<Key, Value> providedExtension,
                              ValueContainerInputRemapping hashToFileIds) throws IOException {
      super(baseFile,
            providedExtension.createKeyDescriptor(baseFile),
            providedExtension.createValueExternalizer(baseFile),
            originalExtension.getCacheSize(), originalExtension.keyIsUniqueForIndexedFile(),
            true, true, hashToFileIds);
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
