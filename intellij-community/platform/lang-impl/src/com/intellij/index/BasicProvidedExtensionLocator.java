// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.index;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.psi.stubs.provided.StubProvidedIndexExtension;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import com.intellij.util.indexing.provided.ProvidedIndexExtensionLocator;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class BasicProvidedExtensionLocator implements ProvidedIndexExtensionLocator {
  private static final String PREBUILT_INDEX_PATH_PROP = "prebuilt.hash.index.dir";

  @Nullable
  @Override
  public <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtension(@NotNull FileBasedIndexExtension<K, V> originalExtension) {
    File root = getPrebuiltIndexPath();
    if (root == null || !new File(root, StringUtil.toLowerCase(originalExtension.getName().getName())).exists()) return null;

    return originalExtension.getName().equals(StubUpdatingIndex.INDEX_ID)
           ? (ProvidedIndexExtension<K, V>)new StubProvidedIndexExtension(root)
           : new ProvidedIndexExtensionImpl<>(root, originalExtension);
  }

  @Nullable
  private static File getPrebuiltIndexPath() {
    String path = System.getProperty(PREBUILT_INDEX_PATH_PROP);
    if (path == null) return null;
    File file = new File(path);
    return file.exists() ? file : null;
  }

  private static class ProvidedIndexExtensionImpl<K, V> implements ProvidedIndexExtension<K, V> {
    @NotNull
    private final File myIndexFile;
    @NotNull
    private final ID<K, V> myIndexId;
    @NotNull
    private final KeyDescriptor<K> myKeyDescriptor;
    @NotNull
    private final DataExternalizer<V> myValueExternalizer;

    private ProvidedIndexExtensionImpl(@NotNull File file,
                                       @NotNull FileBasedIndexExtension<K, V> originalExtension) {
      myIndexFile = file;
      myIndexId = originalExtension.getName();
      myKeyDescriptor = originalExtension.getKeyDescriptor();
      myValueExternalizer = originalExtension.getValueExternalizer();
    }

    @NotNull
    @Override
    public File getIndexPath() {
      return myIndexFile;
    }

    @NotNull
    @Override
    public ID<K, V> getIndexId() {
      return myIndexId;
    }

    @NotNull
    @Override
    public KeyDescriptor<K> createKeyDescriptor() {
      return myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<V> createValueExternalizer() {
      return myValueExternalizer;
    }
  }
}
