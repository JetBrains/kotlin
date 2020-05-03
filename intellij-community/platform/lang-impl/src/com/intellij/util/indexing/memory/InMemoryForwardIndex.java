// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.memory;

import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class InMemoryForwardIndex implements ForwardIndex {
  private final TIntObjectHashMap<byte[]> myMap = new TIntObjectHashMap<>();

  @Override
  public synchronized @Nullable ByteArraySequence get(@NotNull Integer key) throws IOException {
    byte[] bytes = myMap.get(key);
    return bytes == null ? null : new ByteArraySequence(bytes);
  }

  @Override
  public synchronized void put(@NotNull Integer key, @Nullable ByteArraySequence value) {
    if (value == null) {
      myMap.remove(key);
    } else {
      myMap.put(key, value.toBytes());
    }
  }

  @Override
  public void force() {

  }

  @Override
  public synchronized void clear() {
    myMap.clear();
  }

  @Override
  public void close() {

  }
}
