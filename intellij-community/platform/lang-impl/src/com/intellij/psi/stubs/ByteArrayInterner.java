// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;

import java.util.Arrays;

class ByteArrayInterner {
  private static final TObjectHashingStrategy<byte[]> BYTE_ARRAY_STRATEGY = new TObjectHashingStrategy<byte[]>() {
    @Override
    public int computeHashCode(byte[] object) {
      return Arrays.hashCode(object);
    }

    @Override
    public boolean equals(byte[] o1, byte[] o2) {
      return Arrays.equals(o1, o2);
    }
  };
  private final TObjectIntHashMap<byte[]> arrayToStart = new TObjectIntHashMap<>(BYTE_ARRAY_STRATEGY);
  final BufferExposingByteArrayOutputStream joinedBuffer = new BufferExposingByteArrayOutputStream();

  int internBytes(byte[] bytes) {
    if (bytes.length == 0) return 0;

    int start = arrayToStart.get(bytes);
    if (start == 0) {
      start = joinedBuffer.size() + 1; // should be positive
      arrayToStart.put(bytes, start);
      joinedBuffer.write(bytes, 0, bytes.length);
    }
    return start;
  }
}
