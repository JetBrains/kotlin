// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.index;

import com.intellij.psi.stubs.StubSharedIndexExtension;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.OnDiskSharedIndexChunkLocator;
import com.intellij.util.indexing.provided.SharedIndexExtension;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class SharedIndexExtensions {
  private static final String SHARED_INDEX_ENABLED = "shared.index.enabled";

  public static boolean areSharedIndexesEnabled() {
    return System.getProperty(OnDiskSharedIndexChunkLocator.ROOT_PROP) != null || System.getProperty(SHARED_INDEX_ENABLED) != null;
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public static <K, V> SharedIndexExtension<K, V> findExtension(@NotNull FileBasedIndexExtension<K, V> extension) {
    if (extension.getName().equals(StubUpdatingIndex.INDEX_ID)) {
      return (SharedIndexExtension<K, V>)new StubSharedIndexExtension();
    }
    return new SharedIndexExtensionImpl<>(extension);
  }

  private static class SharedIndexExtensionImpl<K, V> implements SharedIndexExtension<K, V> {
    private final FileBasedIndexExtension<K, V> myOriginalExtension;

    SharedIndexExtensionImpl(@NotNull FileBasedIndexExtension<K, V> originalExtension) {
      myOriginalExtension = originalExtension;
    }

    @NotNull
    @Override
    public ID<K, V> getIndexId() {
      return myOriginalExtension.getName();
    }

    @NotNull
    @Override
    public KeyDescriptor<K> createKeyDescriptor(@NotNull Path indexPath) {
      return myOriginalExtension.getKeyDescriptor();
    }

    @NotNull
    @Override
    public DataExternalizer<V> createValueExternalizer(@NotNull Path indexPath) {
      return myOriginalExtension.getValueExternalizer();
    }
  }
}
