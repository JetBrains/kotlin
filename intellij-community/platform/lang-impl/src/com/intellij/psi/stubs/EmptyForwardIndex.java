// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EmptyForwardIndex implements ForwardIndex {
  @Nullable
  @Override
  public ByteArraySequence get(@NotNull Integer key) {
    return null;
  }

  @Override
  public void put(@NotNull Integer key, @Nullable ByteArraySequence value) { }

  @Override
  public void clear() { }

  @Override
  public void close() { }

  @Override
  public void force() { }
}
