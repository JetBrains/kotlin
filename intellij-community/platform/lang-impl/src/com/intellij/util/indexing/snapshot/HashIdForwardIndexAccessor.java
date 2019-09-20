// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot;

import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.MapInputDataDiffBuilder;
import com.intellij.util.indexing.impl.forward.AbstractMapForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.IntForwardIndexAccessor;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public class HashIdForwardIndexAccessor<Key, Value, Input>
  extends AbstractMapForwardIndexAccessor<Key, Value, Integer>
  implements IntForwardIndexAccessor<Key, Value> {
  private final UpdatableSnapshotInputMappingIndex<Key, Value, Input> mySnapshotInputMappingIndex;

  HashIdForwardIndexAccessor(@NotNull UpdatableSnapshotInputMappingIndex<Key, Value, Input> snapshotInputMappingIndex) {
    super(EnumeratorIntegerDescriptor.INSTANCE);
    mySnapshotInputMappingIndex = snapshotInputMappingIndex;
  }

  @Nullable
  @Override
  protected Map<Key, Value> convertToMap(@Nullable Integer hashId) throws IOException {
    return hashId == null ? null : mySnapshotInputMappingIndex.readData(hashId);
  }

  @NotNull
  @Override
  public InputDataDiffBuilder<Key, Value> getDiffBuilderFromInt(int inputId, int hashId) throws IOException {
    return new MapInputDataDiffBuilder<>(inputId, convertToMap(hashId));
  }

  @Override
  public int serializeIndexedDataToInt(@NotNull InputData<Key, Value> data) {
    return data == InputData.empty() ? 0 : ((HashedInputData<Key, Value>)data).getHashId();
  }

  @Nullable
  @Override
  public Integer convertToDataType(@NotNull InputData<Key, Value> data) {
    return serializeIndexedDataToInt(data);
  }
}
