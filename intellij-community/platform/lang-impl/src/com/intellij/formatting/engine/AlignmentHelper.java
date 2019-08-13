// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.engine;

import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.util.containers.MultiMap;

import java.util.*;

public class AlignmentHelper {
  private static final Logger LOG = Logger.getInstance(AlignmentHelper.class);

  private static final Map<Alignment.Anchor, BlockAlignmentProcessor> ALIGNMENT_PROCESSORS = new EnumMap<>(Alignment.Anchor.class);
  static {
    ALIGNMENT_PROCESSORS.put(Alignment.Anchor.LEFT, new LeftEdgeAlignmentProcessor());
    ALIGNMENT_PROCESSORS.put(Alignment.Anchor.RIGHT, new RightEdgeAlignmentProcessor());
  }

  private final Set<Alignment> myAlignmentsToSkip = new HashSet<>();
  private final Document myDocument;
  private final BlockIndentOptions myBlockIndentOptions;

  private final AlignmentCyclesDetector myCyclesDetector;

  private final Map<LeafBlockWrapper, Set<LeafBlockWrapper>> myBackwardShiftedAlignedBlocks = new HashMap<>();
  private final Map<AbstractBlockWrapper, Set<AbstractBlockWrapper>> myAlignmentMappings = new HashMap<>();

  public AlignmentHelper(Document document, MultiMap<Alignment, Block> blocksToAlign, BlockIndentOptions options) {
    myDocument = document;
    myBlockIndentOptions = options;
    int totalBlocks = blocksToAlign.values().size();
    myCyclesDetector = new AlignmentCyclesDetector(totalBlocks);
  }

  private static void reportAlignmentProcessingError(BlockAlignmentProcessor.Context context) {
    ASTNode node = context.targetBlock.getNode();
    Language language = node != null ? node.getPsi().getLanguage() : null;
    String message = (language != null ? language.getDisplayName() + ": " : "") + "Can't align block " + context.targetBlock;
    LOG.error(message, new Throwable(), AttachmentFactory.createAttachment(context.document));
  }

  LeafBlockWrapper applyAlignment(final AlignmentImpl alignment, final LeafBlockWrapper currentBlock) {
    BlockAlignmentProcessor alignmentProcessor = ALIGNMENT_PROCESSORS.get(alignment.getAnchor());
    if (alignmentProcessor == null) {
      LOG.error(String.format("Can't find alignment processor for alignment anchor %s", alignment.getAnchor()));
      return null;
    }

    BlockAlignmentProcessor.Context context = new BlockAlignmentProcessor.Context(
      myDocument, alignment, currentBlock, myAlignmentMappings, myBackwardShiftedAlignedBlocks,
      myBlockIndentOptions.getIndentOptions(currentBlock));
    final LeafBlockWrapper offsetResponsibleBlock = alignment.getOffsetRespBlockBefore(currentBlock);
    if (offsetResponsibleBlock != null) {
      myCyclesDetector.registerOffsetResponsibleBlock(offsetResponsibleBlock);
    }
    BlockAlignmentProcessor.Result result = alignmentProcessor.applyAlignment(context);
    switch (result) {
      case TARGET_BLOCK_PROCESSED_NOT_ALIGNED:
        return null;
      case TARGET_BLOCK_ALIGNED:
        storeAlignmentMapping(currentBlock);
        return null;
      case BACKWARD_BLOCK_ALIGNED:
        if (offsetResponsibleBlock == null) {
          return null;
        }
        Set<LeafBlockWrapper> blocksCausedRealignment = new HashSet<>();
        myBackwardShiftedAlignedBlocks.clear();
        myBackwardShiftedAlignedBlocks.put(offsetResponsibleBlock, blocksCausedRealignment);
        blocksCausedRealignment.add(currentBlock);
        storeAlignmentMapping(currentBlock, offsetResponsibleBlock);
        if (myCyclesDetector.isCycleDetected()) {
          reportAlignmentProcessingError(context);
          return null;
        }
        myCyclesDetector.registerBlockRollback(currentBlock);
        return offsetResponsibleBlock.getNextBlock();
      case RECURSION_DETECTED:
        myAlignmentsToSkip.add(alignment);
        return offsetResponsibleBlock; // Fall through to the 'register alignment to skip'.
      case UNABLE_TO_ALIGN_BACKWARD_BLOCK:
        myAlignmentsToSkip.add(alignment);
        return null;
      default:
        return null;
    }
  }

  boolean shouldSkip(AlignmentImpl alignment) {
    return myAlignmentsToSkip.contains(alignment);
  }

  private void storeAlignmentMapping(AbstractBlockWrapper block1, AbstractBlockWrapper block2) {
    doStoreAlignmentMapping(block1, block2);
    doStoreAlignmentMapping(block2, block1);
  }

  private void doStoreAlignmentMapping(AbstractBlockWrapper key, AbstractBlockWrapper value) {
    Set<AbstractBlockWrapper> wrappers = myAlignmentMappings.get(key);
    if (wrappers == null) {
      myAlignmentMappings.put(key, wrappers = new HashSet<>());
    }
    wrappers.add(value);
  }

  private void storeAlignmentMapping(LeafBlockWrapper currentBlock) {
    AlignmentImpl alignment = null;
    AbstractBlockWrapper block = currentBlock;
    while (alignment == null && block != null) {
      alignment = block.getAlignment();
      block = block.getParent();
    }
    if (alignment != null) {
      block = alignment.getOffsetRespBlockBefore(currentBlock);
      if (block != null) {
        storeAlignmentMapping(currentBlock, block);
      }
    }
  }

}
