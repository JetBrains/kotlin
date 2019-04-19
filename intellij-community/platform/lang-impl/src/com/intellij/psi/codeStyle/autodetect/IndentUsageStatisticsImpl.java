/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.codeStyle.autodetect;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class IndentUsageStatisticsImpl implements IndentUsageStatistics {
  private static final Comparator<IndentUsageInfo> DECREASING_ORDER =
    (o1, o2) -> Integer.compare(o2.getTimesUsed(), o1.getTimesUsed());

  private final List<? extends LineIndentInfo> myLineInfos;

  private int myPreviousLineIndent;
  private int myPreviousRelativeIndent;

  private int myTotalLinesWithTabs = 0;
  private int myTotalLinesWithWhiteSpaces = 0;

  private final TIntIntHashMap myIndentToUsagesMap = new TIntIntHashMap();
  private List<IndentUsageInfo> myIndentUsages = ContainerUtil.newArrayList();
  private final Stack<IndentData> myParentIndents = ContainerUtil.newStack(new IndentData(0, 0));

  public IndentUsageStatisticsImpl(@NotNull List<? extends LineIndentInfo> lineInfos) {
    myLineInfos = lineInfos;
    buildIndentToUsagesMap();
    myIndentUsages = toIndentUsageList(myIndentToUsagesMap);
    ContainerUtil.sort(myIndentUsages, DECREASING_ORDER);
  }

  @NotNull
  private static List<IndentUsageInfo> toIndentUsageList(@NotNull TIntIntHashMap indentToUsages) {
    List<IndentUsageInfo> indentUsageInfos = ContainerUtil.newArrayList();
    TIntIntIterator it = indentToUsages.iterator();
    while (it.hasNext()) {
      it.advance();
      indentUsageInfos.add(new IndentUsageInfo(it.key(), it.value()));
    }
    return indentUsageInfos;
  }

  public void buildIndentToUsagesMap() {
    myPreviousLineIndent = 0;
    myPreviousRelativeIndent = 0;

    for (LineIndentInfo lineInfo : myLineInfos) {
      if (lineInfo.isLineWithTabs()) {
        myTotalLinesWithTabs++;
      }
      else if (lineInfo.isLineWithNormalIndent()) {
        handleNormalIndent(lineInfo.getIndentSize());
      }
    }
  }

  @NotNull
  private IndentData findParentIndent(int indent) {
    while (myParentIndents.size() != 1 && myParentIndents.peek().indent > indent) {
      myParentIndents.pop();
    }
    return myParentIndents.peek();
  }

  private void handleNormalIndent(int currentIndent) {
    int relativeIndent = currentIndent - myPreviousLineIndent;
    if (relativeIndent < 0) {
      IndentData indentData = findParentIndent(currentIndent);
      myPreviousLineIndent = indentData.indent;
      myPreviousRelativeIndent = indentData.relativeIndent;
      relativeIndent = currentIndent - myPreviousLineIndent;
    }

    if (relativeIndent == 0) {
      relativeIndent = myPreviousRelativeIndent;
    }
    else {
      myParentIndents.push(new IndentData(currentIndent, relativeIndent));
    }

    increaseIndentUsage(relativeIndent);

    myPreviousRelativeIndent = relativeIndent;
    myPreviousLineIndent = currentIndent;

    if (currentIndent > 0) {
      myTotalLinesWithWhiteSpaces++;
    }
  }

  private void increaseIndentUsage(int relativeIndent) {
    int timesUsed = myIndentToUsagesMap.get(relativeIndent);
    myIndentToUsagesMap.put(relativeIndent, ++timesUsed);
  }

  @Override
  public int getTotalLinesWithLeadingTabs() {
    return myTotalLinesWithTabs;
  }

  @Override
  public int getTotalLinesWithLeadingSpaces() {
    return myTotalLinesWithWhiteSpaces;
  }

  @Override
  public IndentUsageInfo getKMostUsedIndentInfo(int k) {
    return myIndentUsages.get(k);
  }

  @Override
  public int getTimesIndentUsed(int indent) {
    return myIndentToUsagesMap.get(indent);
  }

  @Override
  public int getTotalIndentSizesDetected() {
    return myIndentToUsagesMap.size();
  }

  private static class IndentData {
    public final int indent;
    public final int relativeIndent;

    IndentData(int indent, int relativeIndent) {
      this.indent = indent;
      this.relativeIndent = relativeIndent;
    }
  }
}
