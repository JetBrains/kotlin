// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.forward.AbstractForwardIndexAccessor;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

class StubUpdatingForwardIndexAccessor extends AbstractForwardIndexAccessor<Integer, SerializedStubTree, IndexedStubs> {
  StubUpdatingForwardIndexAccessor() {super(new StubForwardIndexExternalizer<Void>() {
    @Override
    protected void writeStubIndexKey(@NotNull DataOutput out, @NotNull StubIndexKey key, Void aVoid) throws IOException {
      DataInputOutputUtil.writeINT(out, key.getUniqueId());
    }

    @Override
    protected Void createStubIndexKeySerializationState(@NotNull DataOutput out, @NotNull Set<StubIndexKey> set) {
      return null;
    }

    @Override
    protected ID<?, ?> readStubIndexKey(@NotNull DataInput input, Void aVoid) throws IOException {
      return ID.findById(DataInputOutputUtil.readINT(input));
    }

    @Override
    protected Void createStubIndexKeySerializationState(@NotNull DataInput input, int stubIndexKeyCount) {
      return null;
    }
  });}

  @Nullable
  @Override
  public IndexedStubs convertToDataType(@NotNull InputData<Integer, SerializedStubTree> data) {
    return getIndexedStubs(data.getKeyValues());
  }

  @Override
  protected InputDataDiffBuilder<Integer, SerializedStubTree> createDiffBuilder(int inputId,
                                                                                @Nullable IndexedStubs inputData) {
    return new StubCumulativeInputDiffBuilder(inputId, inputData);
  }

  @Nullable
  static IndexedStubs getIndexedStubs(@Nullable Map<? extends Integer, ? extends SerializedStubTree> map) {
    SerializedStubTree serializedStubTree = ContainerUtil.getFirstItem(ContainerUtil.notNullize(map).values());
    return serializedStubTree == null ? null : serializedStubTree.getIndexedStubs();
  }
}
