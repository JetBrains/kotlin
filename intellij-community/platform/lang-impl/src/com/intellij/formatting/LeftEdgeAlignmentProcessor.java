/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

/**
 * {@link BlockAlignmentProcessor} implementation for {@link Alignment} that
 * {@link Alignment.Anchor#LEFT anchors to the left block edge}.
 * 
 * @author Denis Zhdanov
 */
public class LeftEdgeAlignmentProcessor extends AbstractBlockAlignmentProcessor {

  @Override
  protected IndentData calculateAlignmentAnchorIndent(@NotNull Context context) {
    LeafBlockWrapper offsetResponsibleBlock = context.alignment.getOffsetRespBlockBefore(context.targetBlock);
    if (offsetResponsibleBlock == null) {
      return null;
    }

    final WhiteSpace whiteSpace = offsetResponsibleBlock.getWhiteSpace();
    if (whiteSpace.containsLineFeeds()) {
      return new IndentData(whiteSpace.getIndentSpaces(), whiteSpace.getSpaces());
    }
    else {
      final int offsetBeforeBlock = CoreFormatterUtil.getStartColumn(offsetResponsibleBlock);
      final AbstractBlockWrapper prevIndentedBlock = CoreFormatterUtil.getIndentedParentBlock(context.targetBlock);
      if (prevIndentedBlock == null) {
        return new IndentData(0, offsetBeforeBlock);
      }
      else {
        final int parentIndent = prevIndentedBlock.getWhiteSpace().getIndentOffset();
        if (parentIndent > offsetBeforeBlock) {
          return new IndentData(0, offsetBeforeBlock);
        }
        else {
          return new IndentData(parentIndent, offsetBeforeBlock - parentIndent);
        }
      }
    }
  }

  @Override
  protected boolean applyIndentToTheFirstBlockOnLine(@NotNull IndentData alignmentAnchorIndent, @NotNull Context context) {
    WhiteSpace whiteSpace = context.targetBlock.getWhiteSpace();
    whiteSpace.setSpaces(alignmentAnchorIndent.getSpaces(), alignmentAnchorIndent.getIndentSpaces());
    return true;
  }

  @Override
  protected int getAlignmentIndentDiff(@NotNull IndentData alignmentAnchorIndent, @NotNull Context context) {
    IndentData indentBeforeBlock = context.targetBlock.getNumberOfSymbolsBeforeBlock();
    return alignmentAnchorIndent.getTotalSpaces() - indentBeforeBlock.getTotalSpaces();
  }
}
