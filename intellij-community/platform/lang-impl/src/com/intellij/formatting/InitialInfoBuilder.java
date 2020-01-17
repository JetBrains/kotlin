// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.formatting;

import com.intellij.formatting.engine.ExpandableIndent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.Stack;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allows to build {@link AbstractBlockWrapper formatting block wrappers} for the target {@link Block formatting blocks}.
 * The main idea of block wrapping is to associate information about {@link WhiteSpace white space before block} with the block itself.
 */
public class InitialInfoBuilder {

  @SuppressWarnings("unused") private static final Logger LOG = Logger.getInstance(InitialInfoBuilder.class);

  private static final RangesAssert ASSERT = new RangesAssert();
  private static final boolean INLINE_TABS_ENABLED = "true".equalsIgnoreCase(System.getProperty("inline.tabs.enabled"));

  private final Map<AbstractBlockWrapper, Block> myResult = new THashMap<>();
  private final MultiMap<ExpandableIndent, AbstractBlockWrapper> myBlocksToForceChildrenIndent = new LinkedMultiMap<>();
  private final MultiMap<Alignment, Block> myBlocksToAlign = new MultiMap<>();
  private final Set<Alignment> myAlignmentsInsideRangeToModify = new HashSet<>();

  private boolean myCollectAlignmentsInsideFormattingRange;

  private final FormattingDocumentModel myModel;
  private final FormatTextRanges myAffectedRanges;
  private final List<TextRange> myExtendedAffectedRanges;
  private final int myPositionOfInterest;

  private final FormattingProgressCallback myProgressCallback;

  private final CommonCodeStyleSettings.IndentOptions myOptions;

  private final Stack<InitialInfoBuilderState> myStates = new Stack<>();

  private @NotNull WhiteSpace              myCurrentWhiteSpace;
  private CompositeBlockWrapper            myRootBlockWrapper;
  private LeafBlockWrapper                 myPreviousBlock;
  private LeafBlockWrapper                 myFirstTokenBlock;
  private LeafBlockWrapper                 myLastTokenBlock;
  private SpacingImpl                      myCurrentSpaceProperty;

  private InitialInfoBuilder(final Block rootBlock,
                             final FormattingDocumentModel model,
                             @Nullable final FormatTextRanges affectedRanges,
                             final CommonCodeStyleSettings.IndentOptions options,
                             final int positionOfInterest,
                             @NotNull FormattingProgressCallback progressCallback)
  {
    myModel = model;
    myAffectedRanges = affectedRanges;
    myExtendedAffectedRanges = affectedRanges != null ? affectedRanges.getExtendedRanges() : null;
    myProgressCallback = progressCallback;
    myCurrentWhiteSpace = new WhiteSpace(getStartOffset(rootBlock), true);
    myOptions = options;
    myPositionOfInterest = positionOfInterest;
  }

  @NotNull
  static InitialInfoBuilder prepareToBuildBlocksSequentially(
    Block root,
    FormattingDocumentModel model,
    FormatProcessor.FormatOptions formatOptions,
    CommonCodeStyleSettings.IndentOptions options,
    @NotNull FormattingProgressCallback progressCallback) {
    InitialInfoBuilder builder = new InitialInfoBuilder(root, model, formatOptions.myAffectedRanges, options, formatOptions.myInterestingOffset, progressCallback);
    builder.setCollectAlignmentsInsideFormattingRange(formatOptions.isReformatWithContext());
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
    return Math.min(maxOffset, maxDocOffset);
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
      ASSERT.checkChildRange(new TextRange(parent.getStartOffset(), parent.getEndOffset()), textRange, myModel);
    }

    myCurrentWhiteSpace.changeEndOffset(blockStartOffset, myModel, myOptions);

    collectAlignments(rootBlock);

    if (isInsideFormattingRanges(textRange) || shouldCollectAlignmentsAround(textRange)) {
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

  private boolean shouldCollectAlignmentsAround(TextRange range) {
    return myCollectAlignmentsInsideFormattingRange && isInsideExtendedAffectedRange(range);
  }

  private void collectAlignments(Block rootBlock) {
    if (myCollectAlignmentsInsideFormattingRange && rootBlock.getAlignment() != null
        && isAffectedByFormatting(rootBlock) && !isDisabled(rootBlock.getTextRange()))
    {
      myAlignmentsInsideRangeToModify.add(rootBlock.getAlignment());
    }

    if (rootBlock.getAlignment() != null) {
      myBlocksToAlign.putValue(rootBlock.getAlignment(), rootBlock);
    }
  }

  private boolean isInsideExtendedAffectedRange(TextRange range) {
    if (myExtendedAffectedRanges == null) return false;

    for (TextRange affectedRange : myExtendedAffectedRanges) {
      if (affectedRange.intersects(range)) return true;
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

    if (myPositionOfInterest != -1) {
      myResult.put(wrappedRootBlock, rootBlock);
    }

    InitialInfoBuilderState state = new InitialInfoBuilderState(rootBlock, wrappedRootBlock, currentWrapParent);

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
    TextRange textRange = rootBlock.getTextRange();
    LeafBlockWrapper info = new LeafBlockWrapper(rootBlock, parent, myCurrentWhiteSpace, myModel, myOptions, myPreviousBlock, readOnly, textRange);
    if (index == 0) {
      info.arrangeParentTextRange();
    }

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
    if (isDisabled(myCurrentWhiteSpace.getTextRange()))
      myCurrentWhiteSpace.setReadOnly(true);
    myPreviousBlock = info;

    if (myPositionOfInterest != -1 && (textRange.contains(myPositionOfInterest) || textRange.getEndOffset() == myPositionOfInterest)) {
      myResult.put(info, rootBlock);
      if (parent != null) myResult.put(parent, parentBlock);
    }
    return info;
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

  private boolean isDisabled(@NotNull TextRange range) {
    return myAffectedRanges != null && myAffectedRanges.isInDisabledRange(range);
  }

  private boolean isInsideFormattingRanges(TextRange range) {
    return myAffectedRanges == null || !myAffectedRanges.isReadOnly(range);
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


