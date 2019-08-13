/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.slicer;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.usageView.UsageInfo;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
* @author cdr
*/ // rehash map on each PSI modification since SmartPsiPointer's hashCode() and equals() are changed
public class DuplicateMap {
  private static final TObjectHashingStrategy<SliceUsage> USAGE_INFO_EQUALITY = new TObjectHashingStrategy<SliceUsage>() {
    @Override
    public int computeHashCode(SliceUsage object) {
      UsageInfo info = object.getUsageInfo();
      TextRange range = info.getRangeInElement();
      return range == null ? 0 : range.hashCode();
    }

    @Override
    public boolean equals(SliceUsage o1, SliceUsage o2) {
      return o1.getUsageInfo().equals(o2.getUsageInfo());
    }
  };
  private final Map<SliceUsage, SliceNode> myDuplicates = new THashMap<>(USAGE_INFO_EQUALITY);

  SliceNode putNodeCheckDupe(@NotNull final SliceNode node) {
    return ReadAction.compute(() -> {
      SliceUsage usage = node.getValue();
      return myDuplicates.putIfAbsent(usage, node);
    });
  }

  public void clear() {
    myDuplicates.clear();
  }
}
