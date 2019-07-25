/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.formatting.engine;

import com.intellij.formatting.LeafBlockWrapper;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleConstraints;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.Nullable;

public class BlockRangesMap {
  private final LeafBlockWrapper myLastBlock;
  private final TIntObjectHashMap<LeafBlockWrapper> myTextRangeToWrapper;

  public BlockRangesMap(LeafBlockWrapper first, LeafBlockWrapper last) {
    myLastBlock = last;
    myTextRangeToWrapper = buildTextRangeToInfoMap(first);
  }

  private static TIntObjectHashMap<LeafBlockWrapper> buildTextRangeToInfoMap(final LeafBlockWrapper first) {
    final TIntObjectHashMap<LeafBlockWrapper> result = new TIntObjectHashMap<>();
    LeafBlockWrapper current = first;
    while (current != null) {
      result.put(current.getStartOffset(), current);
      current = current.getNextBlock();
    }
    return result;
  }
  
  public boolean containsLineFeedsOrTooLong(final TextRange dependency) {
    LeafBlockWrapper child = myTextRangeToWrapper.get(dependency.getStartOffset());
    if (child == null) return false;
    final int endOffset = dependency.getEndOffset();
    final int startOffset = child.getStartOffset();
    while (child != null && child.getEndOffset() < endOffset) {
      if (child.containsLineFeeds() || (child.getStartOffset() - startOffset) > CodeStyleConstraints.MAX_RIGHT_MARGIN) return true;
      child = child.getNextBlock();
      if (child != null &&
          child.getWhiteSpace().getEndOffset() <= endOffset &&
          child.getWhiteSpace().containsLineFeeds()) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public LeafBlockWrapper getBlockAtOrAfter(final int startOffset) {
    int current = startOffset;
    LeafBlockWrapper result = null;
    while (current < myLastBlock.getEndOffset()) {
      final LeafBlockWrapper currentValue = myTextRangeToWrapper.get(current);
      if (currentValue != null) {
        result = currentValue;
        break;
      }
      current++;
    }

    LeafBlockWrapper prevBlock = getPrevBlock(result);

    if (prevBlock != null && prevBlock.contains(startOffset)) {
      return prevBlock;
    }
    else {
      return result;
    }
  }

  @Nullable
  private LeafBlockWrapper getPrevBlock(@Nullable final LeafBlockWrapper result) {
    if (result != null) {
      return result.getPreviousBlock();
    }
    else {
      return myLastBlock;
    }
  }
}