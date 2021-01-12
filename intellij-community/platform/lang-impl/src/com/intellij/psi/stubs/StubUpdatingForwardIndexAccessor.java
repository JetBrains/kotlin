// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.forward.SingleEntryIndexForwardIndexAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

class StubUpdatingForwardIndexAccessor extends SingleEntryIndexForwardIndexAccessor<SerializedStubTree> {
  StubUpdatingForwardIndexAccessor(@NotNull FileBasedIndexExtension<Integer, SerializedStubTree> extension) {
    super(extension);
  }

  @Override
  public @NotNull InputDataDiffBuilder<Integer, SerializedStubTree> createDiffBuilderByMap(int inputId,
                                                                                           @Nullable Map<Integer, SerializedStubTree> data)
    throws IOException {
    SerializedStubTree tree = ContainerUtil.isEmpty(data) ? null : ContainerUtil.getFirstItem(data.values());
    if (tree != null) {
      tree.restoreIndexedStubs();
    }
    return new StubCumulativeInputDiffBuilder(inputId, tree);
  }
}
