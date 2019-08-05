// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.hash.MergedInvertedIndex;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public interface ProvidedIndexExtension<K, V> {
  Logger LOG = Logger.getInstance(ProvidedIndexExtension.class);

  @NotNull
  File getIndexPath();

  @NotNull
  ID<K, V> getIndexId();

  @NotNull
  KeyDescriptor<K> createKeyDescriptor();

  @NotNull
  DataExternalizer<V> createValueExternalizer();

  @NotNull
  static <K, V> UpdatableIndex<K, V, FileContent> wrapWithProvidedIndex(@NotNull ProvidedIndexExtension<K, V> providedIndexExtension,
                                                                        @NotNull FileBasedIndexExtension<K, V> originalExtension,
                                                                        @NotNull UpdatableIndex<K, V, FileContent> index) {
    try {
      return MergedInvertedIndex.create(providedIndexExtension, originalExtension, index);
    }
    catch (IOException e) {
      LOG.error(e);
      return index;
    }
  }
}
