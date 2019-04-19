/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexey Chmutov
 */
public abstract class TemplateLanguageBlock extends AbstractBlock implements BlockWithParent {
  private final TemplateLanguageBlockFactory myBlockFactory;
  private final CodeStyleSettings mySettings;
  private List<DataLanguageBlockWrapper> myForeignChildren;
  private boolean myChildrenBuilt = false;
  private BlockWithParent myParent;

  protected TemplateLanguageBlock(@NotNull TemplateLanguageBlockFactory blockFactory, @NotNull CodeStyleSettings settings,
                                  @NotNull ASTNode node, @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    this(node, null, null, blockFactory, settings, foreignChildren);
  }

  protected TemplateLanguageBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                                  @NotNull TemplateLanguageBlockFactory blockFactory,
                                  @NotNull CodeStyleSettings settings,
                                  @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    super(node, wrap, alignment);
    myBlockFactory = blockFactory;
    myForeignChildren = foreignChildren;
    mySettings = settings;
  }

  @Override
  protected List<Block> buildChildren() {
    myChildrenBuilt = true;
    if (isLeaf()) {
      return EMPTY;
    }
    final ArrayList<TemplateLanguageBlock> tlChildren = new ArrayList<>(5);
    for (ASTNode childNode = getNode().getFirstChildNode(); childNode != null; childNode = childNode.getTreeNext()) {
      if (FormatterUtil.containsWhiteSpacesOnly(childNode)) continue;
      if (shouldBuildBlockFor(childNode)) {
        final TemplateLanguageBlock childBlock = myBlockFactory
          .createTemplateLanguageBlock(childNode, createChildWrap(childNode), createChildAlignment(childNode), null, mySettings);
        childBlock.setParent(this);
        tlChildren.add(childBlock);
      }
    }
    final List<Block> children = (List<Block>)(myForeignChildren == null ? tlChildren : BlockUtil.mergeBlocks(tlChildren, myForeignChildren));
    //BlockUtil.printBlocks(getTextRange(), children);
    return BlockUtil.setParent(children, this);
  }

  protected boolean shouldBuildBlockFor(ASTNode childNode) {
    return childNode.getElementType() != getTemplateTextElementType() || noForeignChildren();
  }

  private boolean noForeignChildren() {
    return (myForeignChildren == null || myForeignChildren.isEmpty());
  }

  void addForeignChild(@NotNull DataLanguageBlockWrapper foreignChild) {
    initForeignChildren();
    myForeignChildren.add(foreignChild);
  }

  void addForeignChildren(List<DataLanguageBlockWrapper> foreignChildren) {
    initForeignChildren();
    myForeignChildren.addAll(foreignChildren);
  }

  private void initForeignChildren() {
    assert !myChildrenBuilt;
    if (myForeignChildren == null) {
      myForeignChildren = new ArrayList<>(5);
    }
  }

  @Override
  @Nullable
  public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    if (child1 instanceof DataLanguageBlockWrapper && child2 instanceof DataLanguageBlockWrapper) {
      return ((DataLanguageBlockWrapper)child1).getRightHandSpacing((DataLanguageBlockWrapper)child2);
    }
    return null;
  }

  /**
   * Invoked when the current base language block is located inside a template data language block to determine the spacing after the current block.
   * @param rightNeighbor the block to the right of the current one
   * @param parent the parent block
   * @param thisBlockIndex the index of the current block in the parent block sub-blocks
   * @return the spacing between the current block and its right neighbor
   */
  @Nullable
  public Spacing getRightNeighborSpacing(@NotNull Block rightNeighbor, @NotNull DataLanguageBlockWrapper parent, int thisBlockIndex) {
    return null;
  }

  /**
   * Invoked when the current base language block is located inside a template data language block to determine the spacing before the current block.
   * @param leftNeighbor the block to the left of the current one, or null if the current block is first child
   * @param parent the parent block
   * @param thisBlockIndex the index of the current block in the parent block sub-blocks
   * @return the spacing between the current block and its left neighbor
   */
  @Nullable
  public Spacing getLeftNeighborSpacing(@Nullable Block leftNeighbor, @NotNull DataLanguageBlockWrapper parent, int thisBlockIndex) {
    return null;
  }

  @Override
  public boolean isLeaf() {
    return noForeignChildren() && getNode().getFirstChildNode() == null;
  }

  protected abstract IElementType getTemplateTextElementType();

  @Override
  public BlockWithParent getParent() {
    return myParent;
  }

  @Override
  public void setParent(BlockWithParent newParent) {
    myParent = newParent;
  }

  /**
   * Checks if DataLanguageBlockFragmentWrapper must be created for the given text range.
   * @param range The range to check.
   * @return True by default.
   */
  public boolean isRequiredRange(TextRange range) {
    return true;
  }

  protected Wrap createChildWrap(ASTNode child) {
    return Wrap.createWrap(WrapType.NONE, false);
  }

  protected Alignment createChildAlignment(ASTNode child) {
    return null;
  }

  public CodeStyleSettings getSettings() {
    return mySettings;
  }

  public List<DataLanguageBlockWrapper> getForeignChildren() {
    return myForeignChildren;
  }

  @Nullable
  public Wrap substituteTemplateChildWrap(@NotNull DataLanguageBlockWrapper child, @Nullable Wrap childWrap) {
    return childWrap;
  }
}

