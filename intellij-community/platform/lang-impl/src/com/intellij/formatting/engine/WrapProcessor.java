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

import com.intellij.formatting.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class WrapProcessor {
  private LeafBlockWrapper myFirstWrappedBlockOnLine = null;
  private final BlockRangesMap myBlockRangesMap;
  private LeafBlockWrapper myWrapCandidate = null;
  private final IndentAdjuster myIndentAdjuster;
  private final int myRightMargin;

  public WrapProcessor(BlockRangesMap blockHelper, IndentAdjuster indentAdjuster, int rightMargin) {
    myIndentAdjuster = indentAdjuster;
    myBlockRangesMap = blockHelper;
    myRightMargin = rightMargin;
  }

  private boolean isSuitableInTheCurrentPosition(final WrapImpl wrap, LeafBlockWrapper currentBlock) {
    if (wrap.getWrapOffset() < currentBlock.getStartOffset()) {
      return true;
    }

    if (wrap.isWrapFirstElement()) {
      return true;
    }

    if (wrap.getType() == WrapImpl.Type.WRAP_AS_NEEDED) {
      return positionAfterWrappingIsSuitable(currentBlock);
    }

    return wrap.getType() == WrapImpl.Type.CHOP_IF_NEEDED && lineOver(currentBlock) && positionAfterWrappingIsSuitable(currentBlock);
  }

  private boolean lineOver(LeafBlockWrapper currentBlock) {
    return !currentBlock.containsLineFeeds() &&
           CoreFormatterUtil.getStartColumn(currentBlock) + currentBlock.getLength() > myRightMargin;
  }


  /**
   * Ensures that offset of the currentBlock is not increased if we make a wrap on it.
   */
  private boolean positionAfterWrappingIsSuitable(LeafBlockWrapper currentBlock) {
    final WhiteSpace whiteSpace = currentBlock.getWhiteSpace();
    if (whiteSpace.containsLineFeeds()) return true;
    final int spaces = whiteSpace.getSpaces();
    int indentSpaces = whiteSpace.getIndentSpaces();
    try {
      final int startColumnNow = CoreFormatterUtil.getStartColumn(currentBlock);
      whiteSpace.ensureLineFeed();
      myIndentAdjuster.adjustLineIndent(currentBlock);
      final int startColumnAfterWrap = CoreFormatterUtil.getStartColumn(currentBlock);
      return startColumnNow > startColumnAfterWrap;
    }
    finally {
      whiteSpace.removeLineFeeds(currentBlock.getSpaceProperty(), myBlockRangesMap);
      whiteSpace.setSpaces(spaces, indentSpaces);
    }
  }


  @Nullable
  private WrapImpl getWrapToBeUsed(final ArrayList<WrapImpl> wraps, LeafBlockWrapper currentBlock) {
    if (wraps.isEmpty()) {
      return null;
    }
    if (myWrapCandidate == currentBlock) return wraps.get(0);

    for (final WrapImpl wrap : wraps) {
      if (!isSuitableInTheCurrentPosition(wrap, currentBlock)) continue;
      if (wrap.isActive()) return wrap;

      final WrapImpl.Type type = wrap.getType();
      if (type == WrapImpl.Type.WRAP_ALWAYS) return wrap;
      if (type == WrapImpl.Type.WRAP_AS_NEEDED || type == WrapImpl.Type.CHOP_IF_NEEDED) {
        if (lineOver(currentBlock)) {
          return wrap;
        }
      }
    }
    return null;
  }

  private boolean isCandidateToBeWrapped(final WrapImpl wrap, LeafBlockWrapper currentBlock) {
    return isSuitableInTheCurrentPosition(wrap, currentBlock) &&
           (wrap.getType() == WrapImpl.Type.WRAP_AS_NEEDED || wrap.getType() == WrapImpl.Type.CHOP_IF_NEEDED) &&
           !currentBlock.getWhiteSpace().isReadOnly();
  }

  /**
   * Allows to answer if wrap of the {@link #myWrapCandidate} object (if any) may be replaced by the given wrap.
   *
   * @param wrap wrap candidate to check
   * @return {@code true} if wrap of the {@link #myWrapCandidate} object (if any) may be replaced by the given wrap;
   * {@code false} otherwise
   */
  private boolean canReplaceWrapCandidate(WrapImpl wrap, LeafBlockWrapper currentBlock) {
    if (myWrapCandidate == null) return true;
    WrapImpl.Type type = wrap.getType();
    if (wrap.isActive() && (type == WrapImpl.Type.CHOP_IF_NEEDED || type == WrapImpl.Type.WRAP_ALWAYS)) return true;
    final WrapImpl currentWrap = myWrapCandidate.getWrap();
    return wrap == currentWrap || !wrap.isChildOf(currentWrap, currentBlock);
  }

  LeafBlockWrapper processWrap(LeafBlockWrapper currentBlock) {
    final SpacingImpl spacing = currentBlock.getSpaceProperty();
    final WhiteSpace whiteSpace = currentBlock.getWhiteSpace();

    final boolean wrapWasPresent = whiteSpace.containsLineFeeds();

    if (wrapWasPresent) {
      myFirstWrappedBlockOnLine = null;

      if (!whiteSpace.containsLineFeedsInitially()) {
        whiteSpace.removeLineFeeds(spacing, myBlockRangesMap);
      }
    }

    final boolean wrapIsPresent = whiteSpace.containsLineFeeds();

    final ArrayList<WrapImpl> wraps = currentBlock.getWraps();
    for (WrapImpl wrap : wraps) {
      wrap.setWrapOffset(currentBlock.getStartOffset());
    }

    final WrapImpl wrap = getWrapToBeUsed(wraps, currentBlock);

    if (wrap != null || wrapIsPresent) {
      if (!wrapIsPresent && !canReplaceWrapCandidate(wrap, currentBlock)) {
        return myWrapCandidate;
      }
      if (wrap != null && wrap.getChopStartBlock() != null) {
        // getWrapToBeUsed() returns the block only if it actually exceeds the right margin. In this case, we need to go back to the
        // first block that has the CHOP_IF_NEEDED wrap type and start wrapping from there.
        LeafBlockWrapper newCurrentBlock = wrap.getChopStartBlock();
        wrap.setActive();
        return newCurrentBlock;
      }
      if (wrap != null && isChopNeeded(wrap, currentBlock)) {
        wrap.setActive();
      }

      if (!wrapIsPresent) {
        whiteSpace.ensureLineFeed();
        if (!wrapWasPresent) {
          if (myFirstWrappedBlockOnLine != null && wrap.isChildOf(myFirstWrappedBlockOnLine.getWrap(), currentBlock)) {
            wrap.ignoreParentWrap(myFirstWrappedBlockOnLine.getWrap(), currentBlock);
            return myFirstWrappedBlockOnLine;
          }
          else {
            myFirstWrappedBlockOnLine = currentBlock;
          }
        }
      }

      myWrapCandidate = null;
    }
    else {
      for (final WrapImpl wrap1 : wraps) {
        if (isCandidateToBeWrapped(wrap1, currentBlock) && canReplaceWrapCandidate(wrap1, currentBlock)) {
          myWrapCandidate = currentBlock;
        }
        if (isChopNeeded(wrap1, currentBlock)) {
          wrap1.saveChopBlock(currentBlock);
        }
      }
    }

    if (!whiteSpace.containsLineFeeds() && myWrapCandidate != null && !whiteSpace.isReadOnly() && lineOver(currentBlock)) {
      return myWrapCandidate;
    }

    return null;
  }

  private boolean isChopNeeded(final WrapImpl wrap, LeafBlockWrapper currentBlock) {
    return wrap != null && wrap.getType() == WrapImpl.Type.CHOP_IF_NEEDED && isSuitableInTheCurrentPosition(wrap, currentBlock);
  }

  void onCurrentLineChanged() {
    myWrapCandidate = null;
  }
  
}
