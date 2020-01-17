// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("SameParameterValue")
class FormattingRangesExtender {
  private final static Logger LOG = Logger.getInstance(FormattingRangesExtender.class);

  private final static int MAX_EXTENSION_LINES = 10;

  private final Document myDocument;
  private final PsiFile  myFile;

  FormattingRangesExtender(@NotNull Document document, PsiFile file) {
    myDocument = document;
    myFile = file;
  }

  public List<TextRange> getExtendedRanges(@NotNull List<TextRange> ranges) {
    return ContainerUtil.map(ranges, range -> processRange(range));
  }

  private TextRange processRange(@NotNull TextRange originalRange) {
    TextRange validRange = ensureRangeIsValid(originalRange);
    ASTNode containingNode = CodeFormatterFacade.findContainingNode(myFile, expandToLine(validRange));
    if (containingNode != null && !validRange.isEmpty()) {
      return narrowToMaxExtensionLines(validRange, getRangeWithSiblings(containingNode));
    }
    return validRange;
  }

  private TextRange narrowToMaxExtensionLines(@NotNull TextRange original, @NotNull TextRange result) {
    int startLine = Math.max(myDocument.getLineNumber(result.getStartOffset()),
                             myDocument.getLineNumber(original.getStartOffset()) - MAX_EXTENSION_LINES);
    int endLine = Math.min(myDocument.getLineNumber(result.getEndOffset() - 1),
                           myDocument.getLineNumber(original.getEndOffset() - 1) + MAX_EXTENSION_LINES);
    int rangeStart = Math.max(result.getStartOffset(), myDocument.getLineStartOffset(startLine));
    int rangeEnd = Math.min(result.getEndOffset(), myDocument.getLineEndOffset(endLine));
    return new TextRange(rangeStart, rangeEnd);
  }

  private TextRange ensureRangeIsValid(@NotNull TextRange range) {
    int startOffset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    final int docLength = myDocument.getTextLength();
    if (endOffset > docLength) {
      LOG.warn("The given range " + endOffset + " exceeds the document length " + docLength);
      return new TextRange(Math.min(startOffset, docLength), docLength);
    }
    return range;
  }

  @Nullable
  private TextRange trimSpaces(@NotNull TextRange range) {
    int startOffset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    startOffset = CharArrayUtil.shiftForward(myDocument.getCharsSequence(), startOffset, endOffset, " /t");
    if (startOffset == endOffset) return null;
    endOffset = CharArrayUtil.shiftBackward(myDocument.getCharsSequence(), startOffset, endOffset, " /t" );
    return new TextRange(startOffset, endOffset);
  }

  private TextRange expandToLine(@NotNull TextRange range) {
    int line = myDocument.getLineNumber(range.getStartOffset());
    if (line == myDocument.getLineNumber(Math.min(range.getEndOffset(), myDocument.getTextLength()))) {
      int lineStart = myDocument.getLineStartOffset(line);
      int lineEnd = myDocument.getLineEndOffset(line);
      TextRange lineRange = trimSpaces(new TextRange(lineStart, lineEnd));
      if (lineRange != null) {
        return lineRange;
      }
    }
    return range;
  }

  private static TextRange getRangeWithSiblings(@NotNull ASTNode astNode) {
    Ref<TextRange> result = Ref.create(astNode.getTextRange());
    IElementType elementType = astNode.getElementType();
    ASTNode sibling = astNode.getTreePrev();
    while (sibling != null && processSibling(sibling, result, elementType)) {
      sibling = sibling.getTreePrev();
    }
    sibling = astNode.getTreeNext();
    while (sibling != null && processSibling(sibling, result, elementType)) {
      sibling = sibling.getTreeNext();
    }
    return result.get();
  }

  private static boolean processSibling(@NotNull ASTNode node,
                                        @NotNull Ref<TextRange> rangeRef,
                                        @NotNull IElementType siblingType) {
    if (node.getPsi() instanceof PsiWhiteSpace)  {
      return !hasMinLineBreaks(node, 2);
    }
    else if (node.getElementType() == siblingType) {
      rangeRef.set(rangeRef.get().union(node.getTextRange()));
    }
    return false;
  }

  private static boolean hasMinLineBreaks(@NotNull ASTNode node, int lineBreaks) {
    int count = 0;
    for (int i = 0; i < node.getChars().length(); i ++) {
      if (node.getChars().charAt(i) == '\n') count ++;
      if (count >= lineBreaks) return true;
    }
    return false;
  }

}
