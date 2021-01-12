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
package com.intellij.formatting.engine;

import com.intellij.formatting.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdjustWhiteSpacesState extends State {

  private final FormattingProgressCallback myProgressCallback;
  
  private final WrapBlocksState myWrapBlocksState;


  private LeafBlockWrapper myCurrentBlock;
  
  private DependentSpacingEngine myDependentSpacingEngine;
  private WrapProcessor myWrapProcessor;
  private BlockRangesMap myBlockRangesMap;
  private IndentAdjuster myIndentAdjuster;


  private final boolean myReformatContext;
  private Set<Alignment> myAlignmentsInsideRangesToModify = null;

  private final HashSet<WhiteSpace> myAlignAgain = new HashSet<>();
  private LeafBlockWrapper myFirstBlock;

  public AdjustWhiteSpacesState(WrapBlocksState state, 
                                FormattingProgressCallback progressCallback,
                                boolean isReformatContext) {
    myWrapBlocksState = state;
    myProgressCallback = progressCallback;
    myReformatContext = isReformatContext;
  }

  @Override
  public void prepare() {
    if (myWrapBlocksState != null) {
      myFirstBlock = myWrapBlocksState.getFirstBlock();
      myCurrentBlock = myFirstBlock;
      myDependentSpacingEngine = myWrapBlocksState.getDependentSpacingEngine();
      myWrapProcessor = myWrapBlocksState.getWrapProcessor();
      myIndentAdjuster = myWrapBlocksState.getIndentAdjuster();
      myBlockRangesMap = myWrapBlocksState.getBlockRangesMap();
      myAlignmentsInsideRangesToModify = myWrapBlocksState.getAlignmentsInsideRangesToModify();
    }
  }
  
  public LeafBlockWrapper getCurrentBlock() {
    return myCurrentBlock;
  }

  @Override
  public void doIteration() {
    LeafBlockWrapper blockToProcess = myCurrentBlock;
    processToken();
    if (blockToProcess != null) {
      myProgressCallback.afterProcessingBlock(blockToProcess);
    }

    if (myCurrentBlock != null) {
      return;
    }

    if (myAlignAgain.isEmpty()) {
      setDone(true);
    }
    else {
      myAlignAgain.clear();
      myDependentSpacingEngine.clear();
      myCurrentBlock = myFirstBlock;
    }
  }

  boolean isReformatSelectedRangesContext() {
    return myReformatContext && !ContainerUtil.isEmpty(myAlignmentsInsideRangesToModify);
  }

  void defineAlignOffset(final LeafBlockWrapper block) {
    AbstractBlockWrapper current = myCurrentBlock;
    while (true) {
      final AlignmentImpl alignment = current.getAlignment();
      if (alignment != null) {
        alignment.setOffsetRespBlock(block);
      }
      current = current.getParent();
      if (current == null) return;
      if (current.getStartOffset() != myCurrentBlock.getStartOffset()) return;
    }
  }

  private void onCurrentLineChanged() {
    myWrapProcessor.onCurrentLineChanged();
  }

  private boolean isCurrentBlockAlignmentUsedInRangesToModify() {
    AbstractBlockWrapper block = myCurrentBlock;
    AlignmentImpl alignment = myCurrentBlock.getAlignment();

    while (alignment == null) {
      block = block.getParent();
      if (block == null || block.getStartOffset() != myCurrentBlock.getStartOffset()) {
        return false;
      }
      alignment = block.getAlignment();
    }

    return myAlignmentsInsideRangesToModify.contains(alignment);
  }

  private static List<TextRange> getDependentRegionRangesAfterCurrentWhiteSpace(final SpacingImpl spaceProperty,
                                                                                final WhiteSpace whiteSpace) {
    if (!(spaceProperty instanceof DependantSpacingImpl)) return ContainerUtil.emptyList();

    if (whiteSpace.isReadOnly() || whiteSpace.isLineFeedsAreReadOnly()) return ContainerUtil.emptyList();

    DependantSpacingImpl spacing = (DependantSpacingImpl)spaceProperty;
    return ContainerUtil.filter(spacing.getDependentRegionRanges(),
                                dependencyRange -> whiteSpace.getStartOffset() < dependencyRange.getEndOffset());
  }


  private void processToken() {
    final SpacingImpl spaceProperty = myCurrentBlock.getSpaceProperty();
    final WhiteSpace whiteSpace = myCurrentBlock.getWhiteSpace();

    if (isReformatSelectedRangesContext()) {
      if (isCurrentBlockAlignmentUsedInRangesToModify() &&
          whiteSpace.isReadOnly() &&
          spaceProperty != null &&
          !spaceProperty.isReadOnly()) {
        whiteSpace.setReadOnly(false);
        whiteSpace.setLineFeedsAreReadOnly(true);
      }
    }

    whiteSpace.arrangeLineFeeds(spaceProperty, myBlockRangesMap);

    if (!whiteSpace.containsLineFeeds()) {
      whiteSpace.arrangeSpaces(spaceProperty);
    }

    try {
      LeafBlockWrapper newBlock = myWrapProcessor.processWrap(myCurrentBlock);
      if (newBlock != null) {
        myCurrentBlock = newBlock;
        return;
      }
    }
    finally {
      if (whiteSpace.containsLineFeeds()) {
        onCurrentLineChanged();
      }
    }

    LeafBlockWrapper newCurrentBlock = myIndentAdjuster.adjustIndent(myCurrentBlock);
    if (newCurrentBlock != null) {
      myCurrentBlock = newCurrentBlock;
      onCurrentLineChanged();
      return;
    }

    defineAlignOffset(myCurrentBlock);

    if (myCurrentBlock.containsLineFeeds()) {
      onCurrentLineChanged();
    }


    final List<TextRange> ranges = getDependentRegionRangesAfterCurrentWhiteSpace(spaceProperty, whiteSpace);
    if (!ranges.isEmpty()) {
      myDependentSpacingEngine.registerUnresolvedDependentSpacingRanges(spaceProperty, ranges);
    }

    if (!whiteSpace.isIsReadOnly() && myDependentSpacingEngine.shouldReformatPreviouslyLocatedDependentSpacing(whiteSpace)) {
      myAlignAgain.add(whiteSpace);
    }
    else if (!myAlignAgain.isEmpty()) {
      myAlignAgain.remove(whiteSpace);
    }

    myCurrentBlock = myCurrentBlock.getNextBlock();
  }
}