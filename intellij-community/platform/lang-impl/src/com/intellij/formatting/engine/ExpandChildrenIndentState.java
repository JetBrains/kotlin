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
import com.intellij.openapi.editor.Document;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ExpandChildrenIndentState extends State {
  private final Document myDocument;
  private final WrapBlocksState myWrapState;
  private IndentAdjuster myIndentAdjuster;
  private MultiMap<ExpandableIndent, AbstractBlockWrapper> myExpandableIndents;
  private LeafBlockWrapper myCurrentBlock;

  private Iterator<ExpandableIndent> myIterator;
  private final MultiMap<Alignment, LeafBlockWrapper> myBlocksToRealign = new MultiMap<>();

  public ExpandChildrenIndentState(Document document, WrapBlocksState state) {
    myDocument = document;
    myWrapState = state;
  }

  @Override
  public void prepare() {
    myExpandableIndents = myWrapState.getExpandableIndent();
    myIndentAdjuster = myWrapState.getIndentAdjuster();
    myIterator = myExpandableIndents.keySet().iterator();
  }

  @Override
  protected void doIteration() {
    if (!myIterator.hasNext()) {
      setDone(true);
      return;
    }

    final ExpandableIndent indent = myIterator.next();
    Collection<AbstractBlockWrapper> blocksToExpandIndent = myExpandableIndents.get(indent);
    if (shouldExpand(blocksToExpandIndent)) {
      for (AbstractBlockWrapper block : blocksToExpandIndent) {
        indent.setEnforceIndent(true);
        reindentNewLineChildren(block);
        indent.setEnforceIndent(false);
      }
    }

    restoreAlignments(myBlocksToRealign);
    myBlocksToRealign.clear();
  }

  private void restoreAlignments(MultiMap<Alignment, LeafBlockWrapper> blocks) {
    for (Alignment alignment : blocks.keySet()) {
      AlignmentImpl alignmentImpl = (AlignmentImpl)alignment;
      if (!alignmentImpl.isAllowBackwardShift()) continue;

      Set<LeafBlockWrapper> toRealign = alignmentImpl.getOffsetResponsibleBlocks();
      arrangeSpaces(toRealign);

      LeafBlockWrapper rightMostBlock = getRightMostBlock(toRealign);
      int maxSpacesBeforeBlock = rightMostBlock.getNumberOfSymbolsBeforeBlock().getTotalSpaces();
      int rightMostBlockLine = myDocument.getLineNumber(rightMostBlock.getStartOffset());

      for (LeafBlockWrapper block : toRealign) {
        int currentBlockLine = myDocument.getLineNumber(block.getStartOffset());
        if (currentBlockLine == rightMostBlockLine) continue;

        int blockIndent = block.getNumberOfSymbolsBeforeBlock().getTotalSpaces();
        int delta = maxSpacesBeforeBlock - blockIndent;
        if (delta > 0) {
          int newSpaces = block.getWhiteSpace().getTotalSpaces() + delta;
          adjustSpacingToKeepAligned(block, newSpaces);
        }
      }
    }
  }

  private static void adjustSpacingToKeepAligned(LeafBlockWrapper block, int newSpaces) {
    WhiteSpace space = block.getWhiteSpace();
    SpacingImpl property = block.getSpaceProperty();
    if (property == null) return;
    space.arrangeSpaces(new SpacingImpl(newSpaces, newSpaces,
                                        property.getMinLineFeeds(),
                                        property.isReadOnly(),
                                        property.isSafe(),
                                        property.shouldKeepLineFeeds(),
                                        property.getKeepBlankLines(),
                                        property.shouldKeepFirstColumn(),
                                        property.getPrefLineFeeds()));
  }

  private static LeafBlockWrapper getRightMostBlock(Collection<? extends LeafBlockWrapper> toRealign) {
    int maxSpacesBeforeBlock = -1;
    LeafBlockWrapper rightMostBlock = null;

    for (LeafBlockWrapper block : toRealign) {
      int spaces = block.getNumberOfSymbolsBeforeBlock().getTotalSpaces();
      if (spaces > maxSpacesBeforeBlock) {
        maxSpacesBeforeBlock = spaces;
        rightMostBlock = block;
      }
    }

    return rightMostBlock;
  }

  private static void arrangeSpaces(Collection<? extends LeafBlockWrapper> toRealign) {
    for (LeafBlockWrapper block : toRealign) {
      WhiteSpace whiteSpace = block.getWhiteSpace();
      SpacingImpl spacing = block.getSpaceProperty();
      whiteSpace.arrangeSpaces(spacing);
    }
  }

  private static boolean shouldExpand(Collection<? extends AbstractBlockWrapper> blocksToExpandIndent) {
    AbstractBlockWrapper last = null;
    for (AbstractBlockWrapper block : blocksToExpandIndent) {
      if (block.getWhiteSpace().containsLineFeeds()) {
        return true;
      }
      last = block;
    }

    if (last != null) {
      AbstractBlockWrapper next = getNextBlock(last);
      if (next != null && next.getWhiteSpace().containsLineFeeds()) {
        int nextNewLineBlockIndent = next.getNumberOfSymbolsBeforeBlock().getTotalSpaces();
        if (nextNewLineBlockIndent >= finMinNewLineIndent(blocksToExpandIndent)) {
          return true;
        }
      }
    }

    return false;
  }

  private static int finMinNewLineIndent(@NotNull Collection<? extends AbstractBlockWrapper> wrappers) {
    int totalMinimum = Integer.MAX_VALUE;
    for (AbstractBlockWrapper wrapper : wrappers) {
      int minNewLineIndent = findMinNewLineIndent(wrapper);
      if (minNewLineIndent < totalMinimum) {
        totalMinimum = minNewLineIndent;
      }
    }
    return totalMinimum;
  }

  private static int findMinNewLineIndent(@NotNull AbstractBlockWrapper block) {
    if (block instanceof LeafBlockWrapper && block.getWhiteSpace().containsLineFeeds()) {
      return block.getNumberOfSymbolsBeforeBlock().getTotalSpaces();
    }
    else if (block instanceof CompositeBlockWrapper) {
      List<AbstractBlockWrapper> children = ((CompositeBlockWrapper)block).getChildren();
      int currentMin = Integer.MAX_VALUE;
      for (AbstractBlockWrapper child : children) {
        int childIndent = findMinNewLineIndent(child);
        if (childIndent < currentMin) {
          currentMin = childIndent;
        }
      }
      return currentMin;
    }
    return Integer.MAX_VALUE;
  }

  private static AbstractBlockWrapper getNextBlock(AbstractBlockWrapper block) {
    List<AbstractBlockWrapper> children = block.getParent().getChildren();
    int nextBlockIndex = children.indexOf(block) + 1;
    if (nextBlockIndex < children.size()) {
      return children.get(nextBlockIndex);
    }
    return null;
  }

  private void reindentNewLineChildren(final @NotNull AbstractBlockWrapper block) {
    if (block instanceof LeafBlockWrapper) {
      WhiteSpace space = block.getWhiteSpace();

      if (space.containsLineFeeds()) {
        myCurrentBlock = (LeafBlockWrapper)block;
        myIndentAdjuster.adjustIndent(myCurrentBlock); //since aligned block starts new line, it should not touch any other block
        storeAlignmentsAfterCurrentBlock();
      }
    }
    else if (block instanceof CompositeBlockWrapper) {
      List<AbstractBlockWrapper> children = ((CompositeBlockWrapper)block).getChildren();
      for (AbstractBlockWrapper childBlock : children) {
        reindentNewLineChildren(childBlock);
      }
    }
  }

  private void storeAlignmentsAfterCurrentBlock() {
    LeafBlockWrapper current = myCurrentBlock.getNextBlock();
    while (current != null && !current.getWhiteSpace().containsLineFeeds()) {
      if (current.getAlignment() != null) {
        myBlocksToRealign.putValue(current.getAlignment(), current);
      }
      current = current.getNextBlock();
    }
  }
}