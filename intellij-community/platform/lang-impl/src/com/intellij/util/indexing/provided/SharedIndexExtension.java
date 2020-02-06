// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface SharedIndexExtension<K, V> {
  @NotNull
  KeyDescriptor<K> createKeyDescriptor(@NotNull Path indexPath);

  @NotNull
  DataExternalizer<V> createValueExternalizer(@NotNull Path indexPath);

  default int getVersion() {
    return 0;
  }
}
