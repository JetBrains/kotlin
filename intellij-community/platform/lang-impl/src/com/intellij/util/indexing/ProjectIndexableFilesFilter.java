// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;

public final class ProjectIndexableFilesFilter extends IdFilter {
  private static final int SHIFT = 6;
  private static final int MASK = (1 << SHIFT) - 1;
  private final long[] myBitMask;
  private final int myModificationCount;
  private final int myMinId;
  private final int myMaxId;

  ProjectIndexableFilesFilter(@NotNull TIntArrayList set, int modificationCount) {
    myModificationCount = modificationCount;
    final int[] minMax = new int[2];
    if (!set.isEmpty()) {
      minMax[0] = minMax[1] = set.get(0);
    }
    set.forEach(value -> {
      minMax[0] = Math.min(minMax[0], value);
      minMax[1] = Math.max(minMax[1], value);
      return true;
    });
    myMaxId = minMax[1];
    myMinId = minMax[0];
    myBitMask = new long[((myMaxId - myMinId) >> SHIFT) + 1];
    set.forEach(value -> {
      value -= myMinId;
      myBitMask[value >> SHIFT] |= (1L << (value & MASK));
      return true;
    });
  }

  @Override
  public boolean containsFileId(int id) {
    if (id < myMinId) return false;
    if (id > myMaxId) return false;
    id -= myMinId;
    return (myBitMask[id >> SHIFT] & (1L << (id & MASK))) != 0;
  }

  public int getModificationCount() {
    return myModificationCount;
  }
}
