/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public abstract class AbstractBlockAlignmentProcessor implements BlockAlignmentProcessor {

  @Override
  public Result applyAlignment(@NotNull Context context) {
    IndentData indent = calculateAlignmentAnchorIndent(context);
    if (indent == null) {
      return Result.TARGET_BLOCK_PROCESSED_NOT_ALIGNED;
    }
    WhiteSpace whiteSpace = context.targetBlock.getWhiteSpace();
    if (whiteSpace.containsLineFeeds() && applyIndentToTheFirstBlockOnLine(indent, context)) {
      return Result.TARGET_BLOCK_ALIGNED;
    }

    int diff = getAlignmentIndentDiff(indent, context);
    if (diff == 0) {
      return Result.TARGET_BLOCK_ALIGNED;
    }

    if (diff > 0) {
      int alignmentSpaces = whiteSpace.getSpaces() + diff;
      whiteSpace.setSpaces(alignmentSpaces, whiteSpace.getIndentSpaces());

      if (!whiteSpace.containsLineFeeds()) {
        // Avoid tabulations usage for aligning blocks that are not the first blocks on a line.
        whiteSpace.setForceSkipTabulationsUsage(true);
      }
      return Result.TARGET_BLOCK_ALIGNED;
    }

    if (!context.alignment.isAllowBackwardShift()) {
      return Result.TARGET_BLOCK_PROCESSED_NOT_ALIGNED;
    }

    LeafBlockWrapper offsetResponsibleBlock = context.alignment.getOffsetRespBlockBefore(context.targetBlock);
    if (offsetResponsibleBlock == null) {
      return Result.TARGET_BLOCK_PROCESSED_NOT_ALIGNED;
    }

    if (offsetResponsibleBlock.getWhiteSpace().isIsReadOnly()) {
      // We're unable to perform backward shift because white space for the target element is read-only.
      return Result.UNABLE_TO_ALIGN_BACKWARD_BLOCK;
    }

    if (!CoreFormatterUtil.allowBackwardAlignment(offsetResponsibleBlock, context.targetBlock, context.alignmentMappings)) {
      return Result.UNABLE_TO_ALIGN_BACKWARD_BLOCK;
    }

    // There is a possible case that alignment options are defined incorrectly. Consider the following example:
    //     int i1;
    //     int i2, i3;
    // There is a problem if all blocks above use the same alignment - block 'i1' is shifted to right in order to align
    // to block 'i3' and reformatting starts back after 'i1'. Now 'i2' is shifted to left as well in order to align to the
    // new 'i1' position. That changes 'i3' position as well that causes 'i1' to be shifted right one more time.
    // Hence, we have endless cycle here. We remember information about blocks that caused indentation change because of
    // alignment of blocks located before them and skip alignment every time we detect an endless cycle.
    Set<LeafBlockWrapper> blocksCausedRealignment = context.backwardShiftedAlignedBlocks.get(offsetResponsibleBlock);
    if (blocksCausedRealignment != null && blocksCausedRealignment.contains(context.targetBlock)) {
      return Result.RECURSION_DETECTED;
    }

    WhiteSpace previousWhiteSpace = offsetResponsibleBlock.getWhiteSpace();
    previousWhiteSpace.setSpaces(previousWhiteSpace.getSpaces() - diff, previousWhiteSpace.getIndentOffset());
    // Avoid tabulations usage for aligning blocks that are not the first blocks on a line.
    if (!previousWhiteSpace.containsLineFeeds()) {
      previousWhiteSpace.setForceSkipTabulationsUsage(true);
    }

    return Result.BACKWARD_BLOCK_ALIGNED;
  }

  /**
   * Asks to calculate indent used for the anchor block of the alignment used by the {@link Context#targetBlock target block}.
   *
   * @param context     current processing context
   * @return            indent to use for the white space of the given block
   */
  @Nullable
  protected abstract IndentData calculateAlignmentAnchorIndent(@NotNull Context context);

  /**
   * Encapsulates logic of applying alignment anchor indent to the target block that starts new line.
   * 
   * @param alignmentAnchorIndent   alignment anchor indent
   * @param context                 current processing context
   * @return                        {@code true} if desired alignment indent is applied to the current block;
   *                                {@code false} otherwise
   */
  protected abstract boolean applyIndentToTheFirstBlockOnLine(@NotNull IndentData alignmentAnchorIndent, @NotNull Context context);

  /**
   * Calculates the difference between alignment anchor indent and current target block indent.
   * 
   * @param alignmentAnchorIndent   alignment anchor indent
   * @param context                 current processing context
   * @return                        alignment anchor indent minus current target block indent
   */
  protected abstract int getAlignmentIndentDiff(@NotNull IndentData alignmentAnchorIndent, @NotNull Context context);
}
