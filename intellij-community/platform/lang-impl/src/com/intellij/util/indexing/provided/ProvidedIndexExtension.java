// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.UpdatableIndex;
import com.intellij.util.indexing.hash.FileContentHashIndex;
import com.intellij.util.indexing.hash.MergedInvertedIndex;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ProvidedIndexExtension<K, V> {
  Logger LOG = Logger.getInstance(ProvidedIndexExtension.class);

  @NotNull
  Path getIndexPath();

  @NotNull
  ID<K, V> getIndexId();

  @NotNull
  KeyDescriptor<K> createKeyDescriptor();

  @NotNull
  DataExternalizer<V> createValueExternalizer();

  @NotNull
  static <K, V> UpdatableIndex<K, V, FileContent> wrapWithProvidedIndex(@NotNull List<ProvidedIndexExtension<K, V>> providedIndexExtensions,
                                                                        @NotNull FileBasedIndexExtension<K, V> originalExtension,
                                                                        @NotNull UpdatableIndex<K, V, FileContent> index,
                                                                        @NotNull FileContentHashIndex contentHashIndex) {
    try {
      return MergedInvertedIndex.create(providedIndexExtensions, originalExtension, index, contentHashIndex);
    }
    catch (IOException e) {
      LOG.error(e);
      return index;
    }
  }
}