// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface ProvidedIndexExtension<K, V> {
  @NotNull
  Path getIndexPath();

  @NotNull
  ID<K, V> getIndexId();

  @NotNull
  KeyDescriptor<K> createKeyDescriptor();

  @NotNull
  DataExternalizer<V> createValueExternalizer();
}