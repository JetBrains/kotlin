// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.index;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.indexing.IndexExtension;
import com.intellij.util.indexing.SnapshotInputMappingIndex;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface IndexImporterFactory {
  ExtensionPointName<IndexImporterFactory> EP_NAME = ExtensionPointName.create("com.intellij.indexImporterFactory");
  @Nullable
  <Key, Value, Input> SnapshotInputMappingIndex<Key, Value, Input> createImporter(@NotNull IndexExtension<Key, Value, Input> extension);
}
