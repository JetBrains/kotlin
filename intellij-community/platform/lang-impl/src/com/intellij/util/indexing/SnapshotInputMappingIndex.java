// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

@ApiStatus.Experimental
public interface SnapshotInputMappingIndex<Key, Value, Input> extends Closeable {
  @Nullable
  Map<Key, Value> readData(@NotNull Input content) throws IOException;
}
