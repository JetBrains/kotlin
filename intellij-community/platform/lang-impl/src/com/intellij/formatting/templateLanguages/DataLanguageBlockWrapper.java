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
package com.intellij.formatting.templateLanguages;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Alexey Chmutov
 */
public class DataLanguageBlockWrapper implements ASTBlock, BlockEx, BlockWithParent {
  private final Block myOriginal;
  @Nullable private final Language myLanguage;
  private List<Block> myBlocks;
  private List<TemplateLanguageBlock> myTlBlocks;
  private BlockWithParent myParent;
  private DataLanguageBlockWrapper myRightHandWrapper;
  private Spacing mySpacing;
  private Map<Pair<Block, Block>, Spacing> myChildDataBorderSpacings;

  private DataLanguageBlockWrapper(@NotNull final Block original) {
    assert !(original instanceof DataLanguageBlockWrapper) && !(original instanceof TemplateLanguageBlock);
    myOriginal = original;

    final ASTNode node = getNode();
    Language language = null;
    if (node != null) {
      final PsiElement psi = node.getPsi();
      if (psi != null) {
        language = psi.getContainingFile().getLanguage();
      }
    }
    myLanguage = language;
  }

  @Override
  @NotNull
  public TextRange getTextRange() {
    return myOriginal.getTextRange();
  }

  @Override
  @NotNull
  public List<Block> getSubBlocks() {
    if (myBlocks == null) {
      myBlocks = buildBlocks();
      initSpacings();
    }
    return myBlocks;
  }

  private void initSpacings() {
    for (int i = 0; i < myBlocks.size(); i++) {
      Block block1 = i == 0 ? null : myBlocks.get(i - 1);
      Block block2 = myBlocks.get(i);
      Spacing spacing = calcChildSpacing(i, block1, block2);
      if (spacing != null) {
        registerChildSpacing(block1, block2, spacing);
      }
    }
  }

  @Nullable
  private Spacing calcChildSpacing(int index2, @Nullable Block block1, @NotNull Block block2) {
    if (block1 instanceof TemplateLanguageBlock) {
      Spacing spacing = ((TemplateLanguageBlock)block1).getRightNeighborSpacing(block2, this, index2 - 1);
      if (spacing != null) return spacing;
    }
    if (block2 instanceof TemplateLanguageBlock) {
      return ((TemplateLanguageBlock)block2).getLeftNeighborSpacing(block1, this, index2);
    }
    return null;
  }

  private void registerChildSpacing(@Nullable Block block1, @NotNull Block block2, Spacing spacing) {
    if (myChildDataBorderSpacings == null) {
      myChildDataBorderSpacings = ContainerUtil.newTroveMap();
    }
    myChildDataBorderSpacings.put(Pair.create(block1, block2), spacing);
  }

  @Nullable
  @Override
  public Language getLanguage() {
    // Use base language code style settings for the template blocks.
    return myLanguage;
  }

  private List<Block> buildBlocks() {
    assert myBlocks == null;
    if (isLeaf()) {
      return AbstractBlock.EMPTY;
    }
    final List<DataLanguageBlockWrapper> subWrappers = BlockUtil.buildChildWrappers(myOriginal);
    final List<Block> children;
    if (myTlBlocks == null) {
      children = new ArrayList<>(subWrappers);
    }
    else if (subWrappers.size() == 0) {
      //noinspection unchecked
      children = (List<Block>)(subWrappers.size() > 0 ? myTlBlocks : BlockUtil.splitBlockIntoFragments(myOriginal, myTlBlocks));
    }
    else {
      children = BlockUtil.mergeBlocks(myTlBlocks, subWrappers);
    }
    //BlockUtil.printBlocks(getTextRange(), children);
    return BlockUtil.setParent(children, this);
  }

  @Override
  public Wrap getWrap() {
    BlockWithParent parent = getParent();
    if (parent instanceof TemplateLanguageBlock) {
      return ((TemplateLanguageBlock)parent).substituteTemplateChildWrap(this, myOriginal.getWrap());
    }
    return myOriginal.getWrap();
  }

  @Override
  @NotNull
  public ChildAttributes getChildAttributes(final int newChildIndex) {
    return myOriginal.getChildAttributes(newChildIndex);
  }

  @Override
  public Indent getIndent() {
    return myOriginal.getIndent();
  }

  @Override
  public Alignment getAlignment() {
    return myOriginal.getAlignment();
  }

  @Override
  @Nullable
  public Spacing getSpacing(Block child1, @NotNull Block child2) {
    if (child1 instanceof DataLanguageBlockWrapper && child2 instanceof DataLanguageBlockWrapper) {
      return myOriginal.getSpacing(((DataLanguageBlockWrapper)child1).myOriginal, ((DataLanguageBlockWrapper)child2).myOriginal);
    }
    if ((child1 instanceof TemplateLanguageBlock || child2 instanceof TemplateLanguageBlock) && myChildDataBorderSpacings != null) {
      return myChildDataBorderSpacings.get(Pair.create(child1, child2));
    }
   return null;
  }

  @Override
  public boolean isIncomplete() {
    return myOriginal.isIncomplete();
  }

  @Override
  public boolean isLeaf() {
    return myTlBlocks == null && myOriginal.isLeaf();
  }

  void addTlChild(TemplateLanguageBlock tlBlock) {
    assert myBlocks == null;
    if (myTlBlocks == null) {
      myTlBlocks = new ArrayList<>(5);
    }
    myTlBlocks.add(tlBlock);
    tlBlock.setParent(this);
  }

  public Block getOriginal() {
    return myOriginal;
  }

  @Override
  public String toString() {
    String tlBlocksInfo = " TlBlocks " + (myTlBlocks == null ? "0" : myTlBlocks.size()) + "|" + getTextRange() + "|";
    return tlBlocksInfo + myOriginal.toString();
  }

  @Nullable
  public static DataLanguageBlockWrapper create(@NotNull final Block original, @Nullable final Indent indent) {
    final boolean doesntNeedWrapper = original instanceof ASTBlock && ((ASTBlock)original).getNode() instanceof OuterLanguageElement;
    return doesntNeedWrapper ? null : new DataLanguageBlockWrapper(original);
  }

  @Override
  @Nullable
  public ASTNode getNode() {
    return myOriginal instanceof ASTBlock ? ((ASTBlock)myOriginal).getNode() : null;
  }

  @Override
  public BlockWithParent getParent() {
    return myParent;
  }

  @Override
  public void setParent(BlockWithParent parent) {
    myParent = parent;
  }

  public void setRightHandSpacing(DataLanguageBlockWrapper rightHandWrapper, Spacing spacing) {
    myRightHandWrapper = rightHandWrapper;
    mySpacing = spacing;
  }

  @Nullable
  public Spacing getRightHandSpacing(DataLanguageBlockWrapper rightHandWrapper) {
    return myRightHandWrapper == rightHandWrapper ? mySpacing : null;
  }
}
