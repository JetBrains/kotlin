// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.IntUnaryOperator;

class IntEnumerator {
  private final TIntIntHashMap myEnumerates;
  private final TIntArrayList myIds;
  private int myNext = 0;

  IntEnumerator() {
    this(true);
  }

  private IntEnumerator(boolean forSavingStub) {
    myEnumerates = forSavingStub ? new TIntIntHashMap(1) : null;
    myIds = new TIntArrayList();
  }

  int enumerate(int number) {
    assert myEnumerates != null;
    int i = myEnumerates.get(number);
    if (i == 0) {
      i = myNext;
      myEnumerates.put(number, myNext++);
      myIds.add(number);
    }
    return i;
  }

  int valueOf(int id) {
    return myIds.get(id);
  }

  void dump(DataOutputStream stream) throws IOException {
    dump(stream, IntUnaryOperator.identity());
  }

  void dump(DataOutputStream stream, IntUnaryOperator idRemapping) throws IOException {
    DataInputOutputUtil.writeINT(stream, myIds.size());
    IOException[] exception = new IOException[1];
    myIds.forEach(id -> {
      try {
        int remapped = idRemapping.applyAsInt(id);
        if (remapped == 0) {
          exception[0] = new IOException("remapping is not found for " + id);
          return false;
        }
        DataInputOutputUtil.writeINT(stream, remapped);
      }
      catch (IOException e) {
        exception[0] = e;
        return false;
      }
      return true;
    });
    if (exception[0] != null) {
      throw exception[0];
    }
  }

  static IntEnumerator read(DataInputStream stream) throws IOException {
    int size = DataInputOutputUtil.readINT(stream);
    IntEnumerator enumerator = new IntEnumerator(false);
    for (int i = 1; i < size + 1; i++) {
      enumerator.myIds.add(DataInputOutputUtil.readINT(stream));
    }
    return enumerator;
  }
}
