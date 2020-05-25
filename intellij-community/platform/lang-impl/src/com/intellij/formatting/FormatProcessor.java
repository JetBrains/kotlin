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

package com.intellij.formatting;

import com.intellij.formatting.engine.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.intellij.formatting.InitialInfoBuilder.prepareToBuildBlocksSequentially;

public class FormatProcessor {
  private static final Logger LOG = Logger.getInstance(FormatProcessor.class);
  
  private final WrapBlocksState myWrapState;
  private final boolean myReformatContext;
  private final Document myDocument;
  
  @NotNull
  private final FormattingProgressCallback myProgressCallback;

  @NotNull
  private final StateProcessor myStateProcessor;

  public FormatProcessor(final FormattingDocumentModel docModel,
                         Block rootBlock,
                         CodeStyleSettings settings,
                         CommonCodeStyleSettings.IndentOptions indentOptions,
                         @Nullable FormatTextRanges affectedRanges,
                         @NotNull FormattingProgressCallback progressCallback)
  {
    this(docModel, rootBlock, new FormatOptions(settings, indentOptions, affectedRanges), progressCallback);
  }

  public FormatProcessor(final FormattingDocumentModel model,
                         Block block,
                         FormatOptions options,
                         @NotNull FormattingProgressCallback callback)
  {
    myProgressCallback = callback;
    
    CommonCodeStyleSettings.IndentOptions defaultIndentOption = options.myIndentOptions;
    CodeStyleSettings settings = options.mySettings;
    BlockIndentOptions blockIndentOptions = new BlockIndentOptions(settings, defaultIndentOption, block);
    
    myDocument = model.getDocument();
    myReformatContext = options.isReformatWithContext();
    
    final InitialInfoBuilder builder = prepareToBuildBlocksSequentially(block, model, options, defaultIndentOption, myProgressCallback);
    myWrapState = new WrapBlocksState(builder, blockIndentOptions);
    
    FormatTextRanges ranges = options.myAffectedRanges;
    
    if (ranges != null && myReformatContext) {
      AdjustFormatRangesState adjustRangesState = new AdjustFormatRangesState(block, ranges, model);
      myStateProcessor = new StateProcessor(adjustRangesState);
      myStateProcessor.setNextState(myWrapState);
    }
    else {
      myStateProcessor = new StateProcessor(myWrapState); 
    }
  }
  
  public BlockRangesMap getBlockRangesMap() {
    return myWrapState.getBlockRangesMap();
  }

  public void format(FormattingModel model) {
    format(model, false);
  }

  public void format(FormattingModel model, boolean sequentially) {
    if (sequentially) {
      myStateProcessor.setNextState(new AdjustWhiteSpacesState(myWrapState, myProgressCallback, myReformatContext));
      myStateProcessor.setNextState(new ExpandChildrenIndentState(myDocument, myWrapState));
      myStateProcessor.setNextState(new ApplyChangesState(model, myWrapState, myProgressCallback));
    }
    else {
      formatWithoutRealModifications(false);
      performModifications(model, false);
    }
  }

  public boolean iteration() {
    if (myStateProcessor.isDone()) {
      return true;
    }
    myStateProcessor.iteration();
    return myStateProcessor.isDone();
  }

  /**
   * Asks current processor to stop any active sequential processing if any.
   */
  public void stopSequentialProcessing() {
    myStateProcessor.stop();
  }

  public void formatWithoutRealModifications() {
    formatWithoutRealModifications(false);
  }

  public void formatWithoutRealModifications(boolean sequentially) {
    myStateProcessor.setNextState(new AdjustWhiteSpacesState(myWrapState, myProgressCallback, myReformatContext));
    myStateProcessor.setNextState(new ExpandChildrenIndentState(myDocument, myWrapState));
    if (sequentially) {
      return;
    }
    doIterationsSynchronously();
  }

  public void performModifications(FormattingModel model) {
    performModifications(model, false);
  }

  public void performModifications(FormattingModel model, boolean sequentially) {
    myStateProcessor.setNextState(new ApplyChangesState(model, myWrapState, myProgressCallback));

    if (sequentially) {
      return;
    }

    doIterationsSynchronously();
  }

  private void doIterationsSynchronously() {
    while (!myStateProcessor.isDone()) {
      myStateProcessor.iteration();
    }
  }

  public void setAllWhiteSpacesAreReadOnly() {
    LeafBlockWrapper current = myWrapState.getFirstBlock();
    while (current != null) {
      current.getWhiteSpace().setReadOnly(true);
      current = current.getNextBlock();
    }
  }

  public static class ChildAttributesInfo {
    public final AbstractBlockWrapper parent;
    public final ChildAttributes attributes;
    public final int index;

    public ChildAttributesInfo(final AbstractBlockWrapper parent, final ChildAttributes attributes, final int index) {
      this.parent = parent;
      this.attributes = attributes;
      this.index = index;
    }
  }

  public IndentInfo getIndentAt(final int offset) {
    LeafBlockWrapper current = adjustAtLanguageBorder(processBlocksBefore(offset), offset);
    AbstractBlockWrapper parent = getParentFor(offset, current);
    if (parent == null) {
      final LeafBlockWrapper previousBlock = current.getPreviousBlock();
      if (previousBlock != null) parent = getParentFor(offset, previousBlock);
      if (parent == null) return new IndentInfo(0, 0, 0);
    }
    int index = getNewChildPosition(parent, offset);
    final Block block = myWrapState.getBlockToInfoMap().get(parent);

    if (block == null) {
      return new IndentInfo(0, 0, 0);
    }

    ChildAttributesInfo info = getChildAttributesInfo(block, index, parent);
    if (info == null) {
      return new IndentInfo(0, 0, 0);
    }

    IndentAdjuster adjuster = myWrapState.getIndentAdjuster();
    return adjuster.adjustLineIndent(info, current);
  }

  private static @NotNull LeafBlockWrapper adjustAtLanguageBorder(@NotNull LeafBlockWrapper current, final int offset) {
    if (!current.contains(offset)) {
      final LeafBlockWrapper previousBlock = current.getPreviousBlock();
      if (previousBlock != null && !previousBlock.contains(offset) &&
          !Objects.equals(previousBlock.getLanguage(), current.getLanguage())) {
        AbstractBlockWrapper prevParent = getParentFor(offset, (AbstractBlockWrapper)previousBlock);
        if (prevParent != null && prevParent.getEndOffset() <= current.getStartOffset()) {
          return previousBlock;
        }
      }
    }
    return current;
  }

  @Nullable
  private static ChildAttributesInfo getChildAttributesInfo(@NotNull final Block block,
                                                            final int index,
                                                            @Nullable AbstractBlockWrapper parent) {
    if (parent == null) {
      return null;
    }
    ChildAttributes childAttributes = block.getChildAttributes(index);

    if (childAttributes == ChildAttributes.DELEGATE_TO_PREV_CHILD) {
      final Block newBlock = block.getSubBlocks().get(index - 1);
      AbstractBlockWrapper prevWrappedBlock;
      if (parent instanceof CompositeBlockWrapper) {
        prevWrappedBlock = ((CompositeBlockWrapper)parent).getChildren().get(index - 1);
      }
      else {
        prevWrappedBlock = parent.getPreviousBlock();
      }
      return getChildAttributesInfo(newBlock, newBlock.getSubBlocks().size(), prevWrappedBlock);
    }

    else if (childAttributes == ChildAttributes.DELEGATE_TO_NEXT_CHILD) {
      AbstractBlockWrapper nextWrappedBlock;
      if (parent instanceof CompositeBlockWrapper) {
        List<AbstractBlockWrapper> children = ((CompositeBlockWrapper)parent).getChildren();
        if (children != null && index < children.size()) {
          nextWrappedBlock = children.get(index);
        }
        else {
          return null;
        }
      }
      else {
        nextWrappedBlock = ((LeafBlockWrapper)parent).getNextBlock();
      }
      return getChildAttributesInfo(block.getSubBlocks().get(index), 0, nextWrappedBlock);
    }

    else {
      return new ChildAttributesInfo(parent, childAttributes, index);
    }
  }
  
  private static int getNewChildPosition(final AbstractBlockWrapper parent, final int offset) {
    AbstractBlockWrapper parentBlockToUse = getLastNestedCompositeBlockForSameRange(parent);
    if (!(parentBlockToUse instanceof CompositeBlockWrapper)) return 0;
    final List<AbstractBlockWrapper> subBlocks = ((CompositeBlockWrapper)parentBlockToUse).getChildren();
    if (subBlocks != null) {
      for (int i = 0; i < subBlocks.size(); i++) {
        AbstractBlockWrapper block = subBlocks.get(i);
        if (block.getStartOffset() >= offset) return i;
      }
      return subBlocks.size();
    }
    else {
      return 0;
    }
  }

  @Nullable
  private static AbstractBlockWrapper getParentFor(final int offset, AbstractBlockWrapper block) {
    AbstractBlockWrapper current = block;
    while (current != null) {
      if (current.getStartOffset() < offset && current.getEndOffset() >= offset) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  @Nullable
  private AbstractBlockWrapper getParentFor(final int offset, LeafBlockWrapper block) {
    AbstractBlockWrapper previous = getPreviousIncompleteBlock(block, offset);
    if (previous != null) {
      return getLastNestedCompositeBlockForSameRange(previous);
    }
    else {
      return getParentFor(offset, (AbstractBlockWrapper)block);
    }
  }

  @Nullable
  private AbstractBlockWrapper getPreviousIncompleteBlock(final LeafBlockWrapper block, final int offset) {
    if (block == null) {
      LeafBlockWrapper lastTokenBlock = myWrapState.getLastBlock();
      if (lastTokenBlock.isIncomplete()) {
        return lastTokenBlock;
      }
      else {
        return null;
      }
    }

    AbstractBlockWrapper current = block;
    while (current.getParent() != null && current.getParent().getStartOffset() > offset) {
      current = current.getParent();
    }

    if (current.getParent() == null) return null;

    if (current.getEndOffset() <= offset) {
      while (!current.isIncomplete() &&
             current.getParent() != null &&
             current.getParent().getEndOffset() <= offset) {
        current = current.getParent();
      }
      if (current.isIncomplete()) return current;
    }

    if (current.getParent() == null) return null;

    final List<AbstractBlockWrapper> subBlocks = current.getParent().getChildren();
    final int index = subBlocks.indexOf(current);
    if (index < 0) {
      LOG.assertTrue(false);
    }
    if (index == 0) return null;

    AbstractBlockWrapper currentResult = subBlocks.get(index - 1);
    if (!currentResult.isIncomplete()) return null;

    AbstractBlockWrapper lastChild = getLastChildOf(currentResult);
    while (lastChild != null && lastChild.isIncomplete()) {
      currentResult = lastChild;
      lastChild = getLastChildOf(currentResult);
    }
    return currentResult;
  }

  @Nullable
  private static AbstractBlockWrapper getLastChildOf(final AbstractBlockWrapper currentResult) {
    AbstractBlockWrapper parentBlockToUse = getLastNestedCompositeBlockForSameRange(currentResult);
    if (!(parentBlockToUse instanceof CompositeBlockWrapper)) return null;
    final List<AbstractBlockWrapper> subBlocks = ((CompositeBlockWrapper)parentBlockToUse).getChildren();
    if (subBlocks.isEmpty()) return null;
    return subBlocks.get(subBlocks.size() - 1);
  }

  /**
   * There is a possible case that particular block is a composite block that contains number of nested composite blocks
   * that all target the same text range. This method allows to derive the most nested block that shares the same range (if any).
   *
   * @param block   block to check
   * @return        the most nested block of the given one that shares the same text range if any; given block otherwise
   */
  @NotNull
  private static AbstractBlockWrapper getLastNestedCompositeBlockForSameRange(@NotNull final AbstractBlockWrapper block) {
    if (!(block instanceof CompositeBlockWrapper)) {
      return block;
    }

    AbstractBlockWrapper result = block;
    AbstractBlockWrapper candidate = block;
    while (true) {
      List<AbstractBlockWrapper> subBlocks = ((CompositeBlockWrapper)candidate).getChildren();
      if (subBlocks == null || subBlocks.size() != 1) {
        break;
      }

      candidate = subBlocks.get(0);
      if (candidate.getStartOffset() == block.getStartOffset() && candidate.getEndOffset() == block.getEndOffset()
          && candidate instanceof CompositeBlockWrapper)
      {
        result = candidate;
      }
      else {
        break;
      }
    }
    return result;
  }
  
  private LeafBlockWrapper processBlocksBefore(final int offset) {
    AdjustWhiteSpacesState state = new AdjustWhiteSpacesState(myWrapState, myProgressCallback, myReformatContext);
    state.prepare();
    
    LeafBlockWrapper last = null;
    while (!state.isDone() && state.getCurrentBlock().getStartOffset() < offset) {
      last = state.getCurrentBlock();
      state.doIteration();
    }

    return state.getCurrentBlock() != null ? state.getCurrentBlock() : last;
  }

  public LeafBlockWrapper getFirstTokenBlock() {
    return myWrapState.getFirstBlock();
  }

  public WhiteSpace getLastWhiteSpace() {
    return myWrapState.getLastWhiteSpace();
  }
  
  public static class FormatOptions {
    public CodeStyleSettings mySettings;
    public CommonCodeStyleSettings.IndentOptions myIndentOptions;

    public FormatTextRanges myAffectedRanges;

    public int myInterestingOffset;

    public FormatOptions(CodeStyleSettings settings,
                         CommonCodeStyleSettings.IndentOptions options,
                         FormatTextRanges ranges) {
      this(settings, options, ranges,  -1);
    }

    public FormatOptions(CodeStyleSettings settings,
                         CommonCodeStyleSettings.IndentOptions options,
                         FormatTextRanges ranges,
                         int interestingOffset) {
      mySettings = settings;
      myIndentOptions = options;
      myAffectedRanges = ranges;
      myInterestingOffset = interestingOffset;
    }

    public boolean isReformatWithContext() {
      return myAffectedRanges != null && myAffectedRanges.isExtendToContext();
    }
  }
}
