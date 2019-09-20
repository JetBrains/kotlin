/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.lang;

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.TreeTraversal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author gregsh
 */
public abstract class ReadOnlyASTNode extends UserDataHolderBase implements ASTNode {

  private final ReadOnlyASTNode myParent;
  private final int myIndex;

  public ReadOnlyASTNode(@Nullable ReadOnlyASTNode parent, int index) {
    myParent = parent;
    myIndex = index;
  }

  @NotNull
  public List<ASTNode> getChildList() {
    return ContainerUtil.immutableList(getChildArray());
  }

  protected abstract ASTNode[] getChildArray();

  @Override
  public ReadOnlyASTNode getTreeParent() {
    return myParent;
  }

  @Override
  public <T extends PsiElement> T getPsi(@NotNull Class<T> clazz) {
    return ObjectUtils.tryCast(getPsi(), clazz);
  }

  @Override
  public ASTNode getFirstChildNode() {
    ASTNode[] kids = getChildArray();
    return kids.length > 0 ? kids[0] : null;
  }

  @Override
  public ASTNode getLastChildNode() {
    ASTNode[] kids = getChildArray();
    return kids.length > 0 ? kids[kids.length - 1] : null;
  }

  @Override
  public ASTNode getTreeNext() {
    ASTNode[] kids = getTreeParent().getChildArray();
    return kids.length > myIndex + 1 ? kids[myIndex + 1] : null;
  }

  @Override
  public ASTNode getTreePrev() {
    return myIndex > 0 ? getTreeParent().getChildArray()[myIndex - 1] : null;
  }

  @NotNull
  @Override
  public ASTNode[] getChildren(@Nullable final TokenSet filter) {
    ASTNode[] kids = getChildArray();
    return kids.length == 0 ? EMPTY_ARRAY : kids.clone();
  }

  @NotNull
  @Override
  public String getText() {
    return getChars().toString();
  }

  @Override
  public boolean textContains(char c) {
    return StringUtil.indexOf(getChars(), c) >= 0;
  }

  @Override
  public int getStartOffset() {
    return getTextRange().getStartOffset();
  }

  @Override
  public int getTextLength() {
    return getTextRange().getLength();
  }

  @NotNull
  @Override
  public ReadOnlyASTNode clone() {
    return this;
  }

  @Override
  public void addChild(@NotNull ASTNode child) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addChild(@NotNull ASTNode child, @Nullable ASTNode anchorBefore) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addLeaf(@NotNull IElementType leafType, @NotNull CharSequence leafText, @Nullable ASTNode anchorBefore) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeChild(@NotNull ASTNode child) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeRange(@NotNull ASTNode firstNodeToRemove, ASTNode firstNodeToKeep) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceChild(@NotNull ASTNode oldChild, @NotNull ASTNode newChild) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceAllChildrenToChildrenOf(@NotNull ASTNode anotherParent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addChildren(@NotNull ASTNode firstChild, ASTNode firstChildToNotAdd, ASTNode anchorBefore) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ASTNode copyElement() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public ASTNode findLeafElementAt(int offset) {
    if (!getTextRange().contains(offset)) return null;
    return SyntaxTraverser.astTraverser(this)
      .expandAndFilter(n -> n.getTextRange().contains(offset))
      .traverse(TreeTraversal.LEAVES_DFS)
      .first();
  }

  @Nullable
  @Override
  public ASTNode findChildByType(@NotNull IElementType type) {
    return findChildByType(type, null);
  }

  @Nullable
  @Override
  public ASTNode findChildByType(@NotNull IElementType type, @Nullable ASTNode anchor) {
    boolean flag = anchor == null;
    for (ASTNode node : getChildren(null)) {
      if (!flag && node.equals(anchor)) flag = true;
      if (flag && node.getElementType() == type) return node;
    }
    return null;
  }

  @Nullable
  @Override
  public ASTNode findChildByType(@NotNull TokenSet typesSet) {
    return findChildByType(typesSet, null);
  }

  @Nullable
  @Override
  public ASTNode findChildByType(@NotNull TokenSet typesSet, @Nullable ASTNode anchor) {
    boolean flag = anchor == null;
    for (ASTNode node : getChildren(null)) {
      if (!flag && node.equals(anchor)) flag = true;
      if (flag && typesSet.contains(node.getElementType())) return node;
    }
    return null;
  }
}
