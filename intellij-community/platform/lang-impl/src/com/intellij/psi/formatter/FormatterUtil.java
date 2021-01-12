// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.formatter;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.CharTable;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;

public final class FormatterUtil {

  /**
   * @deprecated Use {@link #getReformatBeforeCommitCommandName()} instead
   */
  @Deprecated
  public static final String REFORMAT_BEFORE_COMMIT_COMMAND_NAME = "Reformat Code Before Commit";

  public static final Collection<String> FORMATTER_ACTION_NAMES = Collections.unmodifiableCollection(ContainerUtil.newHashSet(
    ReformatCodeProcessor.getCommandName(), getReformatBeforeCommitCommandName()
  ));

  private FormatterUtil() {
  }

  public static boolean isWhitespaceOrEmpty(@Nullable ASTNode node) {
    if (node == null) return false;
    IElementType type = node.getElementType();
    return type == TokenType.WHITE_SPACE || (type != TokenType.ERROR_ELEMENT && node.getTextLength() == 0);
  }

  public static boolean isOneOf(@Nullable ASTNode node, IElementType @NotNull ... types) {
    if (node == null) return false;
    IElementType elementType = node.getElementType();
    for (IElementType each : types) {
      if (elementType == each) return true;
    }
    return false;
  }

  @Nullable
  public static ASTNode getPrevious(@Nullable ASTNode node, IElementType @NotNull ... typesToIgnore) {
    return getNextOrPrevious(node, false, typesToIgnore);
  }

  @Nullable
  public static ASTNode getNext(@Nullable ASTNode node, IElementType @NotNull ... typesToIgnore) {
    return getNextOrPrevious(node, true, typesToIgnore);
  }

  @Nullable
  private static ASTNode getNextOrPrevious(@Nullable ASTNode node, boolean isNext, IElementType @NotNull ... typesToIgnore) {
    if (node == null) return null;

    ASTNode each = isNext ? node.getTreeNext() : node.getTreePrev();
    ASTNode parent = node.getTreeParent();
    while (each == null && parent != null) {
      each = isNext ? parent.getTreeNext() : parent.getTreePrev();
      parent = parent.getTreeParent();
    }

    if (each == null) {
      return null;
    }

    for (IElementType type : typesToIgnore) {
      if (each.getElementType() == type) {
        return getNextOrPrevious(each, isNext, typesToIgnore);
      }
    }

    return each;
  }

  @Nullable
  public static ASTNode getPreviousLeaf(@Nullable ASTNode node, IElementType @NotNull ... typesToIgnore) {
    ASTNode prev = getPrevious(node, typesToIgnore);
    if (prev == null) {
      return null;
    }

    ASTNode result = prev;
    ASTNode lastChild = prev.getLastChildNode();
    while (lastChild != null) {
      result = lastChild;
      lastChild = lastChild.getLastChildNode();
    }

    for (IElementType type : typesToIgnore) {
      if (result.getElementType() == type) {
        return getPreviousLeaf(result, typesToIgnore);
      }
    }
    return result;
  }

  @Nullable
  public static ASTNode getPreviousNonWhitespaceLeaf(@Nullable ASTNode node) {
    if (node == null) return null;
    ASTNode treePrev = node.getTreePrev();
    if (treePrev != null) {
      ASTNode candidate = TreeUtil.getLastChild(treePrev);
      if (candidate != null && !isWhitespaceOrEmpty(candidate)) {
        return candidate;
      }
      else {
        return getPreviousNonWhitespaceLeaf(candidate);
      }
    }
    final ASTNode treeParent = node.getTreeParent();

    if (treeParent == null || treeParent.getTreeParent() == null) {
      return null;
    }
    else {
      return getPreviousNonWhitespaceLeaf(treeParent);
    }
  }

  @Nullable
  public static ASTNode getNextNonWhitespaceLeaf(@Nullable ASTNode node) {
    if (node == null) return null;
    ASTNode treeNext = node.getTreeNext();
    if (treeNext != null) {
      ASTNode candidate = TreeUtil.findFirstLeaf(treeNext);
      if (candidate != null && !isWhitespaceOrEmpty(candidate)) {
        return candidate;
      }
      else {
        return getNextNonWhitespaceLeaf(candidate);
      }
    }
    final ASTNode treeParent = node.getTreeParent();

    if (treeParent == null || treeParent.getTreeParent() == null) {
      return null;
    }
    else {
      return getNextNonWhitespaceLeaf(treeParent);
    }
  }

  @Nullable
  public static ASTNode getPreviousNonWhitespaceSibling(@Nullable ASTNode node) {
    ASTNode prevNode = node == null ? null : node.getTreePrev();
    while (prevNode != null && isWhitespaceOrEmpty(prevNode)) {
      prevNode = prevNode.getTreePrev();
    }
    return prevNode;
  }

  @Nullable
  public static ASTNode getNextNonWhitespaceSibling(@Nullable ASTNode node) {
    ASTNode next = node == null ? null : node.getTreeNext();
    while (next != null && isWhitespaceOrEmpty(next)) {
      next = next.getTreeNext();
    }
    return next;
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, IElementType expectedType) {
    return isPrecededBy(node, expectedType, IElementType.EMPTY_ARRAY);
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, IElementType expectedType, TokenSet skipTypes) {
    return isPrecededBy(node, expectedType, skipTypes.getTypes());
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, IElementType expectedType, IElementType... skipTypes) {
    ASTNode prevNode = node == null ? null : node.getTreePrev();
    while (prevNode != null && (isWhitespaceOrEmpty(prevNode) || isOneOf(prevNode, skipTypes))) {
      prevNode = prevNode.getTreePrev();
    }
    if (prevNode == null) return false;
    return prevNode.getElementType() == expectedType;
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, TokenSet expectedTypes) {
    return isPrecededBy(node, expectedTypes, IElementType.EMPTY_ARRAY);
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, TokenSet expectedTypes, TokenSet skipTypes) {
    return isPrecededBy(node, expectedTypes, skipTypes.getTypes());
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, TokenSet expectedTypes, IElementType... skipTypes) {
    ASTNode prevNode = node == null ? null : node.getTreePrev();
    while (prevNode != null && (isWhitespaceOrEmpty(prevNode) || isOneOf(prevNode, skipTypes))) {
      prevNode = prevNode.getTreePrev();
    }
    if (prevNode == null) return false;
    return expectedTypes.contains(prevNode.getElementType());
  }

  public static boolean hasPrecedingSiblingOfType(@Nullable ASTNode node, IElementType expectedSiblingType, IElementType... skipTypes) {
    for (ASTNode prevNode = node == null ? null : node.getTreePrev(); prevNode != null; prevNode = prevNode.getTreePrev()) {
      if (isWhitespaceOrEmpty(prevNode) || isOneOf(prevNode, skipTypes)) continue;
      if (prevNode.getElementType() == expectedSiblingType) return true;
    }
    return false;
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, IElementType expectedType) {
    return isFollowedBy(node, expectedType, IElementType.EMPTY_ARRAY);
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, IElementType expectedType, TokenSet skipTypes) {
    return isFollowedBy(node, expectedType, skipTypes.getTypes());
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, IElementType expectedType, IElementType... skipTypes) {
    ASTNode nextNode = node == null ? null : node.getTreeNext();
    while (nextNode != null && (isWhitespaceOrEmpty(nextNode) || isOneOf(nextNode, skipTypes))) {
      nextNode = nextNode.getTreeNext();
    }
    if (nextNode == null) return false;
    return nextNode.getElementType() == expectedType;
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, @NotNull TokenSet expectedTypes, TokenSet skipTypes) {
    return isFollowedBy(node, expectedTypes, skipTypes.getTypes());
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, @NotNull TokenSet expectedTypes, IElementType... skipTypes) {
    ASTNode nextNode = node == null ? null : node.getTreeNext();
    while (nextNode != null && (isWhitespaceOrEmpty(nextNode) || isOneOf(nextNode, skipTypes))) {
      nextNode = nextNode.getTreeNext();
    }
    if (nextNode == null) return false;
    return expectedTypes.contains(nextNode.getElementType());
  }

  public static boolean isIncomplete(@Nullable ASTNode node) {
    ASTNode lastChild = node == null ? null : node.getLastChildNode();
    while (lastChild != null && lastChild.getElementType() == TokenType.WHITE_SPACE) {
      lastChild = lastChild.getTreePrev();
    }
    if (lastChild == null) return false;
    if (lastChild.getElementType() == TokenType.ERROR_ELEMENT) return true;
    return isIncomplete(lastChild);
  }

  public static boolean containsWhiteSpacesOnly(@Nullable ASTNode node) {
    if (node == null) return false;

    ArrayDeque<ASTNode> queue = new ArrayDeque<>();
    queue.offer(node);
    while (!queue.isEmpty()) {
      TreeElement each = (TreeElement)queue.poll();
      if (each instanceof CompositeElement && spacesOnly(each)) {
        continue;
      }

      if (each instanceof LeafElement && !spacesOnly(each)) {
        return false;
      }

      Collections.addAll(queue, each.getChildren(null));
    }
    return true;
  }

  private static boolean spacesOnly(@Nullable TreeElement node) {
    if (node == null) return false;

    if (isWhitespaceOrEmpty(node)) return true;
    PsiElement psi = node.getPsi();
    if (psi == null) {
      return false;
    }
    Language language = psi.getLanguage();
    return WhiteSpaceFormattingStrategyFactory.getStrategy(language).containsWhitespacesOnly(node);
  }

  /**
   * There is a possible case that we want to adjust white space which is not represented at the AST/PSI tree, e.g.
   * we might have a multiline comment which uses tabs for inner lines indents and want to replace them by spaces.
   * There is no white space element then, the only leaf is the comment itself.
   * <p/>
   * This method allows such 'inner element modifications', i.e. it receives information on what new text should be used
   * at the target inner element range and performs corresponding replacement by generating new leaf with adjusted text
   * and replacing the old one by it.
   *
   * @param newWhiteSpaceText  new text to use at the target inner element range
   * @param holder             target range holder
   * @param whiteSpaceRange    target range which text should be replaced by the given one
   */
  public static void replaceInnerWhiteSpace(@NotNull final String newWhiteSpaceText,
                                            @NotNull final ASTNode holder,
                                            @NotNull final TextRange whiteSpaceRange)
  {
    final CharTable charTable = SharedImplUtil.findCharTableByTree(holder);
    StringBuilder newText = createNewLeafChars(holder, whiteSpaceRange, newWhiteSpaceText);
    LeafElement newElement =
      Factory.createSingleLeafElement(holder.getElementType(), newText, charTable, holder.getPsi().getManager());

    holder.getTreeParent().replaceChild(holder, newElement);
  }

  public static void replaceWhiteSpace(final String whiteSpace,
                                       final ASTNode leafElement,
                                       final IElementType whiteSpaceToken,
                                       @Nullable final TextRange textRange) {
    final CharTable charTable = SharedImplUtil.findCharTableByTree(leafElement);

    if (textRange != null && textRange.getStartOffset() > leafElement.getTextRange().getStartOffset() &&
        textRange.getEndOffset() < leafElement.getTextRange().getEndOffset()) {
      replaceInnerWhiteSpace(whiteSpace, leafElement, textRange);
      return;
    }

    ASTNode treePrev = findPreviousWhiteSpace(leafElement, whiteSpaceToken);
    if (treePrev == null) {
      treePrev = getWsCandidate(leafElement);
    }

    if (treePrev != null &&
        treePrev.getText().trim().isEmpty() &&
        treePrev.getElementType() != whiteSpaceToken &&
        treePrev.getTextLength() > 0 &&
        !whiteSpace.isEmpty()) {
      LeafElement whiteSpaceElement =
        Factory.createSingleLeafElement(treePrev.getElementType(), whiteSpace, charTable, SharedImplUtil.getManagerByTree(leafElement));

      ASTNode treeParent = treePrev.getTreeParent();
      treeParent.replaceChild(treePrev, whiteSpaceElement);
    }
    else {
      LeafElement whiteSpaceElement =
        Factory.createSingleLeafElement(whiteSpaceToken, whiteSpace, charTable, SharedImplUtil.getManagerByTree(leafElement));

      if (treePrev == null) {
        if (!whiteSpace.isEmpty()) {
          addWhiteSpace(leafElement, whiteSpaceElement);
        }
      }
      else {
        if (!(treePrev.getElementType() == whiteSpaceToken)) {
          if (!whiteSpace.isEmpty()) {
            addWhiteSpace(treePrev, whiteSpaceElement);
          }
        }
        else {
          if (treePrev.getElementType() == whiteSpaceToken) {
            final CompositeElement treeParent = (CompositeElement)treePrev.getTreeParent();
            if (!whiteSpace.isEmpty()) {
              //          LOG.assertTrue(textRange == null || treeParent.getTextRange().equals(textRange));
              treeParent.replaceChild(treePrev, whiteSpaceElement);
            }
            else {
              treeParent.removeChild(treePrev);
            }

            // There is a possible case that more than one PSI element is matched by the target text range.
            // That is the case, for example, for Python's multi-line expression. It may looks like below:
            //     import contextlib,\
            //       math, decimal
            // Here single range contains two blocks: '\' & '\n  '. So, we may want to replace that range to another text, hence,
            // we replace last element located there with it ('\n  ') and want to remove any remaining elements ('\').
            ASTNode removeCandidate = findPreviousWhiteSpace(whiteSpaceElement, whiteSpaceToken);
            while (textRange != null && removeCandidate != null && removeCandidate.getStartOffset() >= textRange.getStartOffset()) {
              treePrev = findPreviousWhiteSpace(removeCandidate, whiteSpaceToken);
              removeCandidate.getTreeParent().removeChild(removeCandidate);
              removeCandidate = treePrev;
            }
            //treeParent.subtreeChanged();
          }
        }
      }
    }
  }

  @Nullable
  private static ASTNode findPreviousWhiteSpace(final ASTNode leafElement, final IElementType whiteSpaceTokenType) {
    final int offset = leafElement.getTextRange().getStartOffset() - 1;
    if (offset < 0) return null;
    final PsiElement psiElement = SourceTreeToPsiMap.treeElementToPsi(leafElement);
    if (psiElement == null) {
      return null;
    }
    final PsiElement found = psiElement.getContainingFile().findElementAt(offset);
    if (found == null) return null;
    final ASTNode treeElement = found.getNode();
    if (treeElement != null && treeElement.getElementType() == whiteSpaceTokenType) return treeElement;
    return null;
  }

  @Nullable
  private static ASTNode getWsCandidate(@Nullable ASTNode node) {
    if (node == null) return null;
    ASTNode treePrev = node.getTreePrev();
    if (treePrev != null) {
      if (treePrev.getElementType() == TokenType.WHITE_SPACE) {
        return treePrev;
      }
      else if (treePrev.getTextLength() == 0 && isSpaceBeforeEmptyElement(treePrev)) {
        return getWsCandidate(treePrev);
      }
      else {
        return node;
      }
    }
    final ASTNode treeParent = node.getTreeParent();

    if (treeParent == null || treeParent.getTreeParent() == null) {
      return node;
    }
    else {
      return getWsCandidate(treeParent);
    }
  }

  private static boolean isSpaceBeforeEmptyElement(ASTNode node) {
    if (node.getElementType().isLeftBound()) {
      final ASTNode parent = node.getTreeParent();
      return parent != null && parent.getFirstChildNode() == node;
    }
    return true;
  }

  private static StringBuilder createNewLeafChars(final ASTNode leafElement, final TextRange textRange, final String whiteSpace) {
    final TextRange elementRange = leafElement.getTextRange();
    final String elementText = leafElement.getText();

    final StringBuilder result = new StringBuilder();

    if (elementRange.getStartOffset() < textRange.getStartOffset()) {
      result.append(elementText, 0, textRange.getStartOffset() - elementRange.getStartOffset());
    }

    result.append(whiteSpace);

    if (elementRange.getEndOffset() > textRange.getEndOffset()) {
      result.append(elementText.substring(textRange.getEndOffset() - elementRange.getStartOffset()));
    }

    return result;
  }

  private static void addWhiteSpace(final ASTNode treePrev, final LeafElement whiteSpaceElement) {
    for (WhiteSpaceFormattingStrategy strategy : WhiteSpaceFormattingStrategyFactory.getAllStrategies()) {
      if (strategy.addWhitespace(treePrev, whiteSpaceElement)) {
        return;
      }
    }

    final ASTNode treeParent = treePrev.getTreeParent();
    treeParent.addChild(whiteSpaceElement, treePrev);
  }


  public static void replaceLastWhiteSpace(final ASTNode astNode, final String whiteSpace, final TextRange textRange) {
    ASTNode lastWS = TreeUtil.findLastLeaf(astNode);
    if (lastWS == null) {
      return;
    }
    if (lastWS.getElementType() != TokenType.WHITE_SPACE) {
      lastWS = null;
    }
    if (lastWS != null && !lastWS.getTextRange().equals(textRange)) {
      return;
    }
    if (whiteSpace.isEmpty() && lastWS == null) {
      return;
    }
    if (lastWS != null && whiteSpace.isEmpty()) {
      lastWS.getTreeParent().removeRange(lastWS, null);
      return;
    }

    LeafElement whiteSpaceElement = ASTFactory.whitespace(whiteSpace);

    if (lastWS == null) {
      astNode.addChild(whiteSpaceElement, null);
    }
    else {
      ASTNode treeParent = lastWS.getTreeParent();
      treeParent.replaceChild(lastWS, whiteSpaceElement);
    }
  }

  /**
   * @return    {@code true} explicitly called 'reformat' is in  progress at the moment; {@code false} otherwise
   */
  public static boolean isFormatterCalledExplicitly() {
    return FORMATTER_ACTION_NAMES.contains(CommandProcessor.getInstance().getCurrentCommandName());
  }

  public static String getReformatBeforeCommitCommandName() {
    return CodeInsightBundle.message("process.reformat.code.before.commit");
  }
}
