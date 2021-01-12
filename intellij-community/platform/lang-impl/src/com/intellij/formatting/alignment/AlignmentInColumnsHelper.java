/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.formatting.alignment;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * This class provides helper methods to use for {@code 'align in columns'} processing.
 * <p/>
 * {@code 'Align in columns'} here means format the code like below:
 * <pre>
 *     class Test {
 *         private int    iii = 1;
 *         private double d   = 2;
 *     }
 * </pre>
 * I.e. components of two lines are aligned to each other in columns.
 * <p/>
 * This class is not singleton but it's thread-safe and provides single-point-of-usage field {@link #INSTANCE}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public class AlignmentInColumnsHelper {

  /**
   * Single-point-of-usage field.
   */
  public static final AlignmentInColumnsHelper INSTANCE = new AlignmentInColumnsHelper();

  /**
   * Allows to answer if given node should be aligned to the previous node of the same type according to the given alignment config
   * assuming that given node is a variable declaration.
   *
   * @param node                         target node which alignment strategy is to be defined
   * @param config                       alignment config to use for processing
   * @param blankLinesToBeKeptOnReformat corresponding KEEP_LINE_IN_* formatting setting
   * @return {@code true} if given node should be aligned to the previous one; {@code false} otherwise
   */
  public boolean useDifferentVarDeclarationAlignment(ASTNode node, AlignmentInColumnsConfig config, int blankLinesToBeKeptOnReformat) {
    ASTNode prev = getPreviousAdjacentNodeOfTargetType(node, config, blankLinesToBeKeptOnReformat);
    if (prev == null) {
      return true;
    }

    ASTNode curr = deriveNodeOfTargetType(node, TokenSet.create(prev.getElementType()));
    if (curr == null) {
      return true;
    }

    // The main idea is to avoid alignment like the one below:
    //     private final int    i;
    //                   double d;
    // I.e. we want to avoid alignment-implied long indents from the start of line.
    // Please note that we do allow alignment like below:
    //     private final int    i;
    //     private       double d;
    ASTNode prevSubNode = getSubNodeThatStartsNewLine(prev.getFirstChildNode(), config);
    ASTNode currSubNode = getSubNodeThatStartsNewLine(curr.getFirstChildNode(), config);
    while (true) {
      boolean prevNodeIsDefined = prevSubNode != null;
      boolean currNodeIsDefined = currSubNode != null;
      // Check if one sub-node starts from new line and another one doesn't start.
      if (prevNodeIsDefined ^ currNodeIsDefined) {
        return true;
      }
      if (prevSubNode == null) {
        break;
      }
      if (prevSubNode.getElementType() != currSubNode.getElementType()
          /*|| StringUtil.countNewLines(prevSubNode.getChars()) != StringUtil.countNewLines(currSubNode.getChars())*/) {
        return true;
      }
      prevSubNode = getSubNodeThatStartsNewLine(prevSubNode.getTreeNext(), config);
      currSubNode = getSubNodeThatStartsNewLine(currSubNode.getTreeNext(), config);
    }

    // There is a possible declaration like the one below
    //     int i1 = 1;
    //     int i2, i3 = 2;
    // Three fields are declared here - 'i1', 'i2' and 'i3'. So, the check if field 'i2' contains assignment should be
    // performed against 'i3'.
    ASTNode currentFieldToUse = curr;
    ASTNode nextNode = curr.getTreeNext();

    for (; nextNode != null && nextNode.getTreeParent() == curr.getTreeParent(); nextNode = nextNode.getTreeNext()) {
      IElementType type = nextNode.getElementType();
      if (config.getWhiteSpaceTokenTypes().contains(type)) {
        ASTNode previous = nextNode.getTreePrev();
        if ((previous != null && previous.getElementType() == curr.getElementType()) || StringUtil.countNewLines(nextNode.getChars()) > 1) {
          break;
        }
        continue;
      }

      if (config.getCommentTokenTypes().contains(type)) {
        continue;
      }

      if (type == curr.getElementType()) {
        currentFieldToUse = nextNode;
      }
    }

    List<IElementType> prevTypes = findSubNodeTypes(prev, config.getDistinguishableTypes());
    List<IElementType> currTypes = findSubNodeTypes(currentFieldToUse, config.getDistinguishableTypes());

    return !prevTypes.equals(currTypes);
  }

  /**
   * Tries to find previous node adjacent to the given node that has the same
   * {@link AlignmentInColumnsConfig#getTargetDeclarationTypes() target type}.
   *
   * @param baseNode                     base node to use
   * @param config                       current processing config
   * @param blankLinesToBeKeptOnReformat
   * @return previous node to the given base node that has that same type and is adjacent to it if possible;
   *         {@code null} otherwise
   */
  @Nullable
  private static ASTNode getPreviousAdjacentNodeOfTargetType(ASTNode baseNode,
                                                             AlignmentInColumnsConfig config,
                                                             final double blankLinesToBeKeptOnReformat) {
    ASTNode nodeOfTargetType = deriveNodeOfTargetType(baseNode, config.getTargetDeclarationTypes());
    if (nodeOfTargetType == null) {
      return null;
    }

    final ASTNode[] prev = new ASTNode[1];
    findPreviousNode(config, baseNode, new NodeProcessor() {
      @Override
      public boolean targetTypeFound(ASTNode node) {
        prev[0] = node;
        return true;
      }

      @Override
      public boolean whitespaceFound(ASTNode node) {
        return blankLinesToBeKeptOnReformat > 0 && StringUtil.countChars(node.getText(), '\n') > 1;
      }
    });
    if (prev[0] == null) return null;

    // ensure there are no non-whitespace, non-comment elements on the top level between baseNode and the found one
    Couple<ASTNode> siblingParents = TreeUtil.findTopmostSiblingParents(prev[0], baseNode);
    if (siblingParents.first != null && siblingParents.second != null) {
      for (ASTNode each = siblingParents.second.getTreePrev(); each != null && each != siblingParents.first; each = each.getTreePrev()) {
        IElementType eachType = each.getElementType();
        if (!config.getCommentTokenTypes().contains(eachType) && !config.getWhiteSpaceTokenTypes().contains(eachType)) return null;
      }
    }

    return deriveNodeOfTargetType(prev[0], TokenSet.create(nodeOfTargetType.getElementType()));
  }

  /**
   * There is a possible case that given base node doesn't have {@link AlignmentInColumnsConfig#getTargetDeclarationTypes() target type}
   * but its first child node or first child node of the first child node etc does.
   * <p/>
   * This method tries to derive node of the target type from the given node.
   *
   * @param baseNode    base node to process
   * @param targetTypes target node types
   * @return base node or its first descendant child that has
   *         {@link AlignmentInColumnsConfig#getTargetDeclarationTypes() target type} target type if the one if found;
   *         {@code null} otherwise
   */
  @Nullable
  private static ASTNode deriveNodeOfTargetType(ASTNode baseNode, TokenSet targetTypes) {
    if (targetTypes.contains(baseNode.getElementType())) {
      return baseNode;
    }
    for (ASTNode node = baseNode; node != null; node = node.getFirstChildNode()) {
      IElementType nodeType = node.getElementType();
      if (targetTypes.contains(nodeType)) {
        return node;
      }
    }
    return null;
  }

  /**
   * Shorthand for calling {@link #findPreviousNode(AlignmentInColumnsConfig, ASTNode, NodeProcessor)} with the type of
   * the given node as a target type.
   *
   * @param config    configuration to use
   * @param from      start node to use
   * @param processor
   * @return true if the processor has returned true for one of the processed nodes, false otherwise
   */
  private static boolean findPreviousNode(AlignmentInColumnsConfig config, ASTNode from, NodeProcessor processor) {
    return findPreviousNode(config, from, from.getElementType(), false, true, processor);
  }

  /**
   * Tries to find node that is direct or indirect previous node of the given node.
   * <p/>
   * E.g. there is a possible use-case:
   * <pre>
   *                     n1
   *                  /    \
   *                n21    n22
   *                 |      |
   *                n31    n32
   * </pre>
   * Let's assume that target node is {@code 'n32'}. 'n31' is assumed to be returned from this method then.
   * <p/>
   * <b>Note:</b> current method avoids going too deep if found node type is the same as start node type
   *
   * @return direct or indirect previous node of the given one having target type if possible; {@code null} otherwise
   */
  private static boolean findPreviousNode(AlignmentInColumnsConfig config,
                                          ASTNode from,
                                          IElementType targetType,
                                          boolean processFrom,
                                          boolean processParent,
                                          NodeProcessor processor) {
    if (from == null) return false;

    for (ASTNode prev = processFrom ? from : from.getTreePrev(); prev != null; prev = prev.getTreePrev()) {
      IElementType prevType = prev.getElementType();
      if (prevType == targetType) {
        if (processor.targetTypeFound(prev)) return true;
      }
      else if (config.getWhiteSpaceTokenTypes().contains(prevType)) {
        if (processor.whitespaceFound(prev)) return true;
      }

      if (findPreviousNode(config, prev.getLastChildNode(), targetType, true, false, processor)) return true;
    }

    if (processParent) {
      for (ASTNode parent = from.getTreeParent(); parent != null; parent = parent.getTreeParent()) {
        if (findPreviousNode(config, parent, targetType, false, false, processor)) return true;
      }
    }
    return false;
  }

  private static abstract class NodeProcessor {
    public boolean targetTypeFound(ASTNode node) {
      return false;
    }

    public boolean whitespaceFound(ASTNode node) {
      return false;
    }
  }

  @Nullable
  private static ASTNode getSubNodeThatStartsNewLine(@Nullable ASTNode startNode, AlignmentInColumnsConfig config) {
    if (startNode == null) {
      return null;
    }

    ASTNode parent = startNode.getTreeParent();
    if (parent == null) {
      // Never expect to be here.
      return null;
    }

    // Check if previous node to the start node is white space that contains line feeds.
    final boolean[] returnFirstNonEmptySubNode = {false};

    findPreviousNode(config, startNode, new NodeProcessor() {
      @Override
      public boolean targetTypeFound(ASTNode node) {
        return true;
      }

      @Override
      public boolean whitespaceFound(ASTNode node) {
        return returnFirstNonEmptySubNode[0] = StringUtil.countNewLines(node.getChars()) > 0;
      }
    });

    boolean stop = false;
    for (ASTNode result = startNode; result != null && result.getTreeParent() == parent; result = result.getTreeNext()) {
      if (config.getStopMultilineCheckElementTypes().contains(result.getElementType())) {
        return null;
      }
      if (result.getTextLength() <= 0) {
        continue;
      }
      if (config.getCommentTokenTypes().contains(result.getElementType())) {
        continue;
      }
      if (config.getWhiteSpaceTokenTypes().contains(result.getElementType()) && StringUtil.countNewLines(result.getChars()) > 0) {
        stop = true;
        continue;
      }
      if (returnFirstNonEmptySubNode[0]) {
        return result;
      }
      if (stop) {
        return result;
      }
    }

    return null;
  }

  private static List<IElementType> findSubNodeTypes(ASTNode node, TokenSet types) {
    List<IElementType> foundTypes = new SmartList<>();
    for (ASTNode child = node.getFirstChildNode(); child != null && child.getTreeParent() == node; child = child.getTreeNext()) {
      IElementType type = child.getElementType();
      if (types.contains(type)) {
        foundTypes.add(type);
      }
    }
    return foundTypes;
  }
}
