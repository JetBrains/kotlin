// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
    assert myEnumerates != null;
    DataInputOutputUtil.writeINT(stream, myIds.size());
    myIds.forEach(id -> {
      try {
        DataInputOutputUtil.writeINT(stream, id);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      return true;
    });
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
