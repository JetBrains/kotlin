// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AlignmentCyclesDetector {
  private final int myTotalAlignmentsCount;
  private int myBlockRollbacks;

  private LeafBlockWrapper myOffsetResponsibleBlock;
  private int myBeforeTotalSpaces;

  private final Map<List<LeafBlockWrapper>, Set<Pair<Integer, Integer>>> map = new HashMap<>();

  public AlignmentCyclesDetector(int totalAlignmentsCount) {
    myTotalAlignmentsCount = totalAlignmentsCount;
  }

  public void registerOffsetResponsibleBlock(@NotNull LeafBlockWrapper block) {
    myOffsetResponsibleBlock = block;
    final WhiteSpace whitespace = block.getWhiteSpace();
    myBeforeTotalSpaces = whitespace.getTotalSpaces();
  }

  public boolean isCycleDetected() {
    return myBlockRollbacks > myTotalAlignmentsCount;
  }

  public void registerBlockRollback(LeafBlockWrapper currentBlock) {
    List<LeafBlockWrapper> pairId = Arrays.asList(currentBlock, myOffsetResponsibleBlock);

    Set<Pair<Integer, Integer>> pairs = map.get(pairId);
    if (pairs == null) {
      pairs = new HashSet<>();
      map.put(pairId, pairs);
    }

    final WhiteSpace whitespace = myOffsetResponsibleBlock.getWhiteSpace();
    int newSpaces = whitespace.getTotalSpaces();
    boolean added = pairs.add(Pair.create(myBeforeTotalSpaces, newSpaces));
    if (added) {
      myBlockRollbacks++;
    }
  }
}
