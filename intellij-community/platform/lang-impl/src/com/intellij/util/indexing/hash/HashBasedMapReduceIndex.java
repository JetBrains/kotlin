// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.IntIntFunction;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

class HashBasedMapReduceIndex<Key, Value> extends VfsAwareMapReduceIndex<Key, Value, FileContent> {
  @NotNull
  private final ProvidedIndexExtension<Key, Value> myProvidedExtension;

  @NotNull
  static <Key, Value> HashBasedMapReduceIndex<Key, Value> create(@NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                                                 @NotNull FileBasedIndexExtension<Key, Value> originalExtension)
    throws IOException {
    File file = providedExtension.getIndexPath();
    return new HashBasedMapReduceIndex<>(file, originalExtension, providedExtension, ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getFileContentHashIndex(file));
  }

  private HashBasedMapReduceIndex(@NotNull File baseFile,
                                  @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                  @NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                  @NotNull FileContentHashIndex hashIndex) throws IOException {
    super(originalExtension, createStorage(baseFile, originalExtension, providedExtension, hashIndex.toHashIdToFileIdFunction()), null, null, null, null);
    myProvidedExtension = providedExtension;
  }

  @NotNull
  public ProvidedIndexExtension<Key, Value> getProvidedExtension() {
    return myProvidedExtension;
  }

  private static <Key, Value> IndexStorage<Key, Value> createStorage(@NotNull File baseFile,
                                                                     @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                                                     @NotNull ProvidedIndexExtension<Key, Value> providedExtension,
                                                                     @NotNull IntIntFunction hashToFileId) throws IOException {
    return new MapIndexStorage<Key, Value>(new File(baseFile, originalExtension.getName().getName()),
                                           providedExtension.createKeyDescriptor(),
                                           providedExtension.createValueExternalizer(),
                                           originalExtension.getCacheSize(),
                                           originalExtension.keyIsUniqueForIndexedFile(),
                                           true,
                                           true,
                                           hashToFileId) {
      @Override
      protected void checkCanceled() {
        ProgressManager.checkCanceled();
      }
    };
  }

}
