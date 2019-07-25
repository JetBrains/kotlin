// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This implementation collects specific direct children of top level elements and specific direct children of specific composite elements.
 * This covers all of the ruby PSI structure
 */
public abstract class AbstractCodeBlockSupportHandler implements CodeBlockSupportHandler {

  /**
   * @return all element types which don't have a parent, e.g. IF_STATEMENT <br>
   * <br>
   * {@link #getDirectChildrenElementTypes} should never return these element types
   */
  @NotNull
  protected abstract TokenSet getTopLevelElementTypes();

  /**
   * @return elements which should be highlighted. These elements don't have children<br>
   * <br>
   * {@link #getDirectChildrenElementTypes} should return an empty TokenSet for an element type if it's a keyword
   */
  @NotNull
  protected abstract TokenSet getKeywordElementTypes();

  /**
   * @return block element types which should be used for navigation
   */
  @NotNull
  protected abstract TokenSet getBlockElementTypes();

  /**
   *  The method defines a highlighting tree. <br>
   *  Highlighting tree nodes are PSI elements. <br>
   *  A highlighting tree has an edge from A to B if and only if <br>
   *    - A is a parent for B in the PSI tree <br>
   *    - <code>#getDirectChildrenElementTypes(getElementType(A)).contains(getElementType(B))</code> <br>
   *  <br>
   *  If two keyword elements have a common ancestor in a highlighting tree they will be highlighted together <br>
   *  The method should be consistent with {@link #getTopLevelElementTypes()} and {@link #getKeywordElementTypes()}<br>
   *  <br>
   *  For example, for the given if statement<br>
   *  <br>
   *  <code>
   *    if expr  <br>
   *       <br>
   *    elsif expr <br>
   *       <br>
   *    else      <br>
   *      <br>
   *    end       <br>
   *  </code>   <br>
   *
   *  the children should be defined as follows:<br>
   *  <br>
   *  IF_STATEMENT => {kIF, ELSIF_BLOCK, ELSE_BLOCK, kEND} <br>
   *  ELSIF_BLOCK => {kELSEIF} <br>
   *  ELSE_BLOCK => {kELSE} <br>
   */
  @NotNull
  protected abstract TokenSet getDirectChildrenElementTypes(@Nullable IElementType parentElementType);

  @NotNull
  @Override
  public List<TextRange> getCodeBlockMarkerRanges(@NotNull PsiElement elementAtCursor) {
    TokenSet keywordElementTypes = getKeywordElementTypes();
    if (!keywordElementTypes.contains(PsiUtilCore.getElementType(elementAtCursor))) {
      return Collections.emptyList();
    }
    final PsiElement rootElement = getParentByTokenSet(elementAtCursor, getTopLevelElementTypes());
    if (rootElement == null) {
      return Collections.emptyList();
    }
    return computeMarkersRanges(rootElement, keywordElementTypes);
  }

  @NotNull
  @Override
  public TextRange getCodeBlockRange(@NotNull PsiElement elementAtCursor) {
    return ObjectUtils.notNull(
      ObjectUtils.doIfNotNull(getParentByTokenSet(elementAtCursor, getBlockElementTypes()), PsiElement::getTextRange),
      TextRange.EMPTY_RANGE);
  }

  @NotNull
  private List<TextRange> computeMarkersRanges(@NotNull PsiElement rootElement, @NotNull TokenSet keywordsElementTypes) {
    final IElementType type = PsiUtilCore.getElementType(rootElement);
    if (keywordsElementTypes.contains(type)) {
      return Collections.singletonList(rootElement.getTextRange());
    }
    TokenSet directChildrenTypes = getDirectChildrenElementTypes(type);
    List<TextRange> result = new ArrayList<>();
    PsiElement currentElement = rootElement.getFirstChild();
    while (currentElement != null) {
      IElementType currentElementType = PsiUtilCore.getElementType(currentElement);
      if (directChildrenTypes.contains(currentElementType)) {
        result.addAll(computeMarkersRanges(currentElement, keywordsElementTypes));
      }
      currentElement = currentElement.getNextSibling();
    }
    return result;
  }

  @Nullable
  private static PsiElement getParentByTokenSet(@NotNull PsiElement element, @NotNull TokenSet tokenSet) {
    PsiElement run = element;
    while (run != null && !tokenSet.contains(PsiUtilCore.getElementType(run))) {
      run = run.getParent();
    }
    return run;
  }
}
