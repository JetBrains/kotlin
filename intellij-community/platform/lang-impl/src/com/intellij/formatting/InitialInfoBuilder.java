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

import com.intellij.formatting.engine.ExpandableIndent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.Stack;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allows to build {@link AbstractBlockWrapper formatting block wrappers} for the target {@link Block formatting blocks}.
 * The main idea of block wrapping is to associate information about {@link WhiteSpace white space before block} with the block itself.
 */
public class InitialInfoBuilder {
  private static final RangesAssert ASSERT = new RangesAssert();
  private static final boolean INLINE_TABS_ENABLED = "true".equalsIgnoreCase(System.getProperty("inline.tabs.enabled"));

  private final Map<AbstractBlockWrapper, Block> myResult = new THashMap<>();
  private final MultiMap<ExpandableIndent, AbstractBlockWrapper> myBlocksToForceChildrenIndent = new LinkedMultiMap<>();
  private final MultiMap<Alignment, Block> myBlocksToAlign = new MultiMap<>();
  private final Set<Alignment> myAlignmentsInsideRangeToModify = ContainerUtil.newHashSet();
  
  private boolean myCollectAlignmentsInsideFormattingRange = false;

  private final FormattingDocumentModel myModel;
  private final FormatTextRanges myAffectedRanges;
  private final List<TextRange> myExtendedAffectedRanges;
  private final int myPositionOfInterest;

  private final FormattingProgressCallback myProgressCallback;
  
  private final FormatterTagHandler myFormatterTagHandler;

  private final CommonCodeStyleSettings.IndentOptions myOptions;

  private final Stack<InitialInfoBuilderState> myStates = new Stack<>();
  
  private @NotNull WhiteSpace              myCurrentWhiteSpace;
  private CompositeBlockWrapper            myRootBlockWrapper;
  private LeafBlockWrapper                 myPreviousBlock;
  private LeafBlockWrapper                 myFirstTokenBlock;
  private LeafBlockWrapper                 myLastTokenBlock;
  private SpacingImpl                      myCurrentSpaceProperty;
  private boolean                          myInsideFormatRestrictingTag;

  private InitialInfoBuilder(final Block rootBlock,
                             final FormattingDocumentModel model,
                             @Nullable final FormatTextRanges affectedRanges,
                             @NotNull CodeStyleSettings settings,
                             final CommonCodeStyleSettings.IndentOptions options,
                             final int positionOfInterest,
                             @NotNull FormattingProgressCallback progressCallback)
  {
    myModel = model;
    myAffectedRanges = affectedRanges;
    myExtendedAffectedRanges = affectedRanges != null ? affectedRanges.getExtendedFormattingRanges() : null;
    myProgressCallback = progressCallback;
    myCurrentWhiteSpace = new WhiteSpace(getStartOffset(rootBlock), true);
    myOptions = options;
    myPositionOfInterest = positionOfInterest;
    myInsideFormatRestrictingTag = false;
    myFormatterTagHandler = new FormatterTagHandler(settings);
  }

  protected static InitialInfoBuilder prepareToBuildBlocksSequentially(
    Block root, 
    FormattingDocumentModel model, 
    FormatProcessor.FormatOptions formatOptions, 
    CodeStyleSettings settings, 
    CommonCodeStyleSettings.IndentOptions options, 
    @NotNull FormattingProgressCallback progressCallback) 
  {
    InitialInfoBuilder builder = new InitialInfoBuilder(root, model, formatOptions.myAffectedRanges, settings, options, formatOptions.myInterestingOffset, progressCallback);
    builder.setCollectAlignmentsInsideFormattingRange(formatOptions.myReformatContext);
    builder.buildFrom(root, 0, null, null, null);
    return builder;
  }

  private int getStartOffset(@NotNull Block rootBlock) {
    int minOffset = rootBlock.getTextRange().getStartOffset();
    if (myAffectedRanges != null) {
      for (FormatTextRange range : myAffectedRanges.getRanges()) {
        if (range.getStartOffset() < minOffset) minOffset = range.getStartOffset();
      }
    }
    return minOffset;
  }
  
  public FormattingDocumentModel getFormattingDocumentModel() {
    return myModel;
  }

  public int getEndOffset() {
    int maxDocOffset = myModel.getTextLength();
    int maxOffset = myRootBlockWrapper != null ? myRootBlockWrapper.getEndOffset() : 0;
    if (myAffectedRanges != null) {
      for (FormatTextRange range : myAffectedRanges.getRanges()) {
        if (range.getTextRange().getEndOffset() > maxOffset) maxOffset = range.getTextRange().getEndOffset();
      }
    }
    return   maxOffset < maxDocOffset ? maxOffset : maxDocOffset;
  }

  public boolean iteration() {
    if (myStates.isEmpty()) {
      return true;
    }

    InitialInfoBuilderState state = myStates.peek();
    doIteration(state);
    return myStates.isEmpty();
  }
  
  private AbstractBlockWrapper buildFrom(final Block rootBlock,
                                         final int index,
                                         @Nullable final CompositeBlockWrapper parent,
                                         @Nullable WrapImpl currentWrapParent,
                                         @Nullable final Block parentBlock)
  {
    final WrapImpl wrap = (WrapImpl)rootBlock.getWrap();
    if (wrap != null) {
      wrap.registerParent(currentWrapParent);
      currentWrapParent = wrap;
    }
    
    TextRange textRange = rootBlock.getTextRange();
    final int blockStartOffset = textRange.getStartOffset();

    if (parent != null) {
      checkRanges(parent, textRange);
    }

    myCurrentWhiteSpace.changeEndOffset(blockStartOffset, myModel, myOptions);

    collectAlignments(rootBlock);

    if (isInsideFormattingRanges(rootBlock) || shouldCollectAlignmentsAround(rootBlock)) {
      final List<Block> subBlocks = rootBlock.getSubBlocks();
      if (subBlocks.isEmpty()) {
        final AbstractBlockWrapper wrapper = buildLeafBlock(rootBlock, parent, false, index, parentBlock);
        if (!subBlocks.isEmpty()) {
          wrapper.setIndent((IndentImpl)subBlocks.get(0).getIndent());
        }
        return wrapper;
      }
      return buildCompositeBlock(rootBlock, parent, index, currentWrapParent);
    }
    else {
      return buildLeafBlock(rootBlock, parent, true, index, parentBlock);
    }
  }

  private boolean shouldCollectAlignmentsAround(Block rootBlock) {
    return myCollectAlignmentsInsideFormattingRange && isInsideExtendedAffectedRange(rootBlock);
  }

  private void collectAlignments(Block rootBlock) {
    if (myCollectAlignmentsInsideFormattingRange && rootBlock.getAlignment() != null
        && isAffectedByFormatting(rootBlock) && !myInsideFormatRestrictingTag)
    {
      myAlignmentsInsideRangeToModify.add(rootBlock.getAlignment());
    }
    
    if (rootBlock.getAlignment() != null) {
      myBlocksToAlign.putValue(rootBlock.getAlignment(), rootBlock);
    }
  }

  private void checkRanges(@NotNull CompositeBlockWrapper parent, TextRange textRange) {
    if (textRange.getStartOffset() < parent.getStartOffset()) {
      ASSERT.assertInvalidRanges(
        textRange.getStartOffset(),
        parent.getStartOffset(),
        myModel,
        "child block start is less than parent block start"
      );
    }

    if (textRange.getEndOffset() > parent.getEndOffset()) {
      ASSERT.assertInvalidRanges(
        textRange.getEndOffset(),
        parent.getEndOffset(),
        myModel,
        "child block end is after parent block end"
      );
    }
  }

  private boolean isInsideExtendedAffectedRange(Block rootBlock) {
    if (myExtendedAffectedRanges == null) return false;

    TextRange blockRange = rootBlock.getTextRange();
    for (TextRange affectedRange : myExtendedAffectedRanges) {
      if (affectedRange.intersects(blockRange)) return true;
    }

    return false;
  }

  private CompositeBlockWrapper buildCompositeBlock(Block rootBlock,
                                                    @Nullable CompositeBlockWrapper parent,
                                                    int index,
                                                    @Nullable WrapImpl currentWrapParent) 
  {
    final CompositeBlockWrapper wrappedRootBlock = new CompositeBlockWrapper(rootBlock, myCurrentWhiteSpace, parent);
    if (index == 0) {
      wrappedRootBlock.arrangeParentTextRange();
    }

    if (myRootBlockWrapper == null) {
      myRootBlockWrapper = wrappedRootBlock;
      myRootBlockWrapper.setIndent((IndentImpl)Indent.getNoneIndent());
    }
    boolean blocksMayBeOfInterest = false;

    if (myPositionOfInterest != -1) {
      myResult.put(wrappedRootBlock, rootBlock);
      blocksMayBeOfInterest = true;
    }
    
    final boolean blocksAreReadOnly = rootBlock instanceof ReadOnlyBlockContainer || blocksMayBeOfInterest;
    
    InitialInfoBuilderState state = new InitialInfoBuilderState(rootBlock, wrappedRootBlock, currentWrapParent, blocksAreReadOnly);
    
    myStates.push(state);
    return wrappedRootBlock;
  }

  private void doIteration(@NotNull InitialInfoBuilderState state) {
    Block currentRoot = state.parentBlock;
    
    List<Block> subBlocks = currentRoot.getSubBlocks();
    int currentBlockIndex = state.getIndexOfChildBlockToProcess();
    final Block currentBlock = subBlocks.get(currentBlockIndex);

    initCurrentWhiteSpace(currentRoot, state.previousBlock, currentBlock);

    final AbstractBlockWrapper wrapper = buildFrom(
      currentBlock, currentBlockIndex, state.wrappedBlock, state.parentBlockWrap, currentRoot
    );
    
    registerExpandableIndents(currentBlock, wrapper);

    if (wrapper.getIndent() == null) {
      wrapper.setIndent((IndentImpl)currentBlock.getIndent());
    }
    if (!state.readOnly) {
      try {
        subBlocks.set(currentBlockIndex, null); // to prevent extra strong refs during model building
      } catch (Throwable ex) {
        // read-only blocks
      }
    }
    
    if (state.childBlockProcessed(currentBlock, wrapper, myOptions)) {
      while (!myStates.isEmpty() && myStates.peek().isProcessed()) {
        myStates.pop();
      }
    }
  }
  
  private void initCurrentWhiteSpace(@NotNull Block currentRoot, @Nullable Block previousBlock, @NotNull Block currentBlock) {
    if (previousBlock != null || myCurrentWhiteSpace.isIsFirstWhiteSpace()) {
      myCurrentSpaceProperty = (SpacingImpl)currentRoot.getSpacing(previousBlock, currentBlock);
    }
  }

  private void registerExpandableIndents(@NotNull Block block, @NotNull AbstractBlockWrapper wrapper) {
    if (block.getIndent() instanceof ExpandableIndent) {
      ExpandableIndent indent = (ExpandableIndent)block.getIndent();
      myBlocksToForceChildrenIndent.putValue(indent, wrapper);
    }
  }
  
  private AbstractBlockWrapper buildLeafBlock(final Block rootBlock,
                                              @Nullable final CompositeBlockWrapper parent,
                                              final boolean readOnly,
                                              final int index,
                                              @Nullable Block parentBlock) 
  {
    LeafBlockWrapper result = doProcessSimpleBlock(rootBlock, parent, readOnly, index, parentBlock);
    myProgressCallback.afterWrappingBlock(result);
    return result;
  }

  private LeafBlockWrapper doProcessSimpleBlock(final Block rootBlock,
                                                @Nullable final CompositeBlockWrapper parent,
                                                final boolean readOnly,
                                                final int index,
                                                @Nullable Block parentBlock)
  {
    if (!INLINE_TABS_ENABLED && !myCurrentWhiteSpace.containsLineFeeds()) {
      myCurrentWhiteSpace.setForceSkipTabulationsUsage(true);
    }
    LeafBlockWrapper info = new LeafBlockWrapper(rootBlock, parent, myCurrentWhiteSpace, myModel, myOptions, myPreviousBlock, readOnly);
    if (index == 0) {
      info.arrangeParentTextRange();
    }

    checkInsideFormatterOffTag(rootBlock);

    TextRange textRange = rootBlock.getTextRange();

    if (myPreviousBlock != null) {
      myPreviousBlock.setNextBlock(info);
    }
    if (myFirstTokenBlock == null) {
      myFirstTokenBlock = info;
    }
    myLastTokenBlock = info;
    if (currentWhiteSpaceIsReadOnly()) {
      myCurrentWhiteSpace.setReadOnly(true);
    }
    if (myCurrentSpaceProperty != null) {
      myCurrentWhiteSpace.setIsSafe(myCurrentSpaceProperty.isSafe());
      myCurrentWhiteSpace.setKeepFirstColumn(myCurrentSpaceProperty.shouldKeepFirstColumn());
    }

    if (info.isEndOfCodeBlock()) {
      myCurrentWhiteSpace.setBeforeCodeBlockEnd(true);
    }

    info.setSpaceProperty(myCurrentSpaceProperty);
    myCurrentWhiteSpace = new WhiteSpace(textRange.getEndOffset(), false);
    if (myInsideFormatRestrictingTag) myCurrentWhiteSpace.setReadOnly(true);
    myPreviousBlock = info;

    if (myPositionOfInterest != -1 && (textRange.contains(myPositionOfInterest) || textRange.getEndOffset() == myPositionOfInterest)) {
      myResult.put(info, rootBlock);
      if (parent != null) myResult.put(parent, parentBlock);
    }
    return info;
  }

  private void checkInsideFormatterOffTag(Block rootBlock) {
    switch (myFormatterTagHandler.getFormatterTag(rootBlock)) {
      case ON:
        myInsideFormatRestrictingTag = false;
        break;
      case OFF:
        myInsideFormatRestrictingTag = true;
        break;
      case NONE:
        break;
    }
  }

  private boolean currentWhiteSpaceIsReadOnly() {
    if (myCurrentSpaceProperty != null && myCurrentSpaceProperty.isReadOnly()) {
      return true;
    }
    else {
      if (myAffectedRanges == null) return false;
      return myAffectedRanges.isWhitespaceReadOnly(myCurrentWhiteSpace.getTextRange());
    }
  }

  private boolean isAffectedByFormatting(final Block block) {
    if (myAffectedRanges == null) return true;

    List<FormatTextRange> allRanges = myAffectedRanges.getRanges();
    Document document = myModel.getDocument();
    int docLength = document.getTextLength();
    
    for (FormatTextRange range : allRanges) {
      int startOffset = range.getStartOffset();
      if (startOffset >= docLength) continue;
      
      int lineNumber = document.getLineNumber(startOffset);
      int lineEndOffset = document.getLineEndOffset(lineNumber);

      int blockStartOffset = block.getTextRange().getStartOffset();
      if (blockStartOffset >= startOffset && blockStartOffset < lineEndOffset) {
        return true;
      }
    }
    
    return false;
  }

  private boolean isInsideFormattingRanges(final Block block) {
    if (myAffectedRanges == null) return true;
    return !myAffectedRanges.isReadOnly(block.getTextRange());
  }

  public Map<AbstractBlockWrapper, Block> getBlockToInfoMap() {
    return myResult;
  }

  public LeafBlockWrapper getFirstTokenBlock() {
    return myFirstTokenBlock;
  }

  public LeafBlockWrapper getLastTokenBlock() {
    return myLastTokenBlock;
  }
  
  public Set<Alignment> getAlignmentsInsideRangeToModify() {
    return myAlignmentsInsideRangeToModify;
  }

  public MultiMap<ExpandableIndent, AbstractBlockWrapper> getExpandableIndentsBlocks() {
    return myBlocksToForceChildrenIndent;
  }

  public MultiMap<Alignment, Block> getBlocksToAlign() {
    return myBlocksToAlign;
  }
  
  public void setCollectAlignmentsInsideFormattingRange(boolean value) {
    myCollectAlignmentsInsideFormattingRange = value;
  }
  
}


