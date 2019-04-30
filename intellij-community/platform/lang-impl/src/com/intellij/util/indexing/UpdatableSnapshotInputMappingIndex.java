// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

@ApiStatus.Experimental
public interface UpdatableSnapshotInputMappingIndex<Key, Value, Input> extends SnapshotInputMappingIndex<Key, Value, Input> {
  @NotNull
  Map<Key, Value> readData(int hashId) throws IOException;

  void putData(@NotNull Input content, @NotNull Map<Key, Value> data) throws IOException;

  int getHashId(@Nullable Input content) throws IOException;

  void flush() throws IOException;

  void clear() throws IOException;
}
