/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.lang.folding;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.psi.util.PsiTreeUtil.skipParentsOfType;

/**
 * @author Rustam Vishnyakov
 */
public class CustomFoldingSurroundDescriptor implements SurroundDescriptor {

  public final static CustomFoldingSurroundDescriptor INSTANCE = new CustomFoldingSurroundDescriptor();
  public final static CustomFoldingRegionSurrounder[] SURROUNDERS;

  private final static String DEFAULT_DESC_TEXT = "Description";

  static {
    List<CustomFoldingRegionSurrounder> surrounderList = new ArrayList<>();
    for (CustomFoldingProvider provider : CustomFoldingProvider.getAllProviders()) {
      surrounderList.add(new CustomFoldingRegionSurrounder(provider));
    }
    SURROUNDERS = surrounderList.toArray(new CustomFoldingRegionSurrounder[0]);
  }

  @NotNull
  @Override
  public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
    if (startOffset >= endOffset) return PsiElement.EMPTY_ARRAY;
    Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(file.getLanguage());
    if (commenter == null || commenter.getLineCommentPrefix() == null && 
                             (commenter.getBlockCommentPrefix() == null || commenter.getBlockCommentSuffix() == null)) {
      return PsiElement.EMPTY_ARRAY;
    }
    PsiElement startElement = file.findElementAt(startOffset);
    PsiElement endElement = file.findElementAt(endOffset - 1);
    if (startElement instanceof PsiWhiteSpace) {
      if (startElement == endElement) return PsiElement.EMPTY_ARRAY;
      startElement = startElement.getNextSibling();
    }
    if (endElement instanceof PsiWhiteSpace) endElement = endElement.getPrevSibling();
    if (startElement != null && endElement != null) {
      startElement = findClosestParentAfterLineBreak(startElement);
      if (startElement != null) {
        endElement = findClosestParentBeforeLineBreak(endElement);
        if (endElement != null) {
          return adjustRange(startElement, endElement);
        }
      }
    }
    return PsiElement.EMPTY_ARRAY;
  }

  @NotNull
  private static PsiElement[] adjustRange(@NotNull PsiElement start, @NotNull PsiElement end) {
    PsiElement newStart = lowerStartElementIfNeeded(start, end);
    PsiElement newEnd = lowerEndElementIfNeeded(start, end);
    if (newStart == null || newEnd == null) {
      return PsiElement.EMPTY_ARRAY;
    }
    PsiElement commonParent = findCommonAncestorForWholeRange(newStart, newEnd);
    if (commonParent != null) {
      return new PsiElement[] {commonParent};
    }
    // If either start or end element is the first/last leaf element in its parent, use the parent itself instead
    // to prevent selection of clearly illegal ranges like the following:
    // [
    //   <selection>1
    // ]</selection>
    // E.g. in case shown, because of that adjustment, closing bracket and number literal won't have the same parent
    // and next test will fail.
    PsiElement newStartParent = getParent(newStart);
    if (newStartParent != null && newStartParent.getFirstChild() == newStart && newStart.getFirstChild() == null) {
      newStart = newStartParent;
    }
    PsiElement newEndParent = getParent(newEnd);
    if (newEndParent != null && newEndParent.getLastChild() == newEnd && newEnd.getFirstChild() == null) {
      newEnd = newEndParent;
    }
    if (getParent(newStart) == getParent(newEnd)) {
      return new PsiElement[] {newStart, newEnd};
    }
    return PsiElement.EMPTY_ARRAY;
  }

  @Nullable
  private static PsiElement getParent(@Nullable PsiElement e) {
    return e instanceof PsiFile ? e : skipParentsOfType(e, GeneratedParserUtilBase.DummyBlock.class);
  }

  @Nullable
  private static PsiElement lowerEndElementIfNeeded(@NotNull PsiElement start, @NotNull PsiElement end) {
    if (PsiTreeUtil.isAncestor(end, start, true)) {
      PsiElement o = end.getLastChild();
      while (o != null && o.getParent() != start.getParent()) {
        PsiElement last = o.getLastChild();
        if (last == null) return o;
        o = last;
      }
      return o;
    }
    return end;
  }

  @Nullable
  private static PsiElement lowerStartElementIfNeeded(@NotNull PsiElement start, @NotNull PsiElement end) {
    if (PsiTreeUtil.isAncestor(start, end, true)) {
      PsiElement o = start.getFirstChild();
      while (o != null && o.getParent() != end.getParent()) {
        PsiElement first = o.getFirstChild();
        if (first == null) return o;
        o = first;
      }
      return o;
    }
    return start;
  }

  @Nullable
  private static PsiElement findCommonAncestorForWholeRange(@NotNull PsiElement start, @NotNull PsiElement end) {
    if (start.getContainingFile() != end.getContainingFile()) {
      return null;
    }
    final PsiElement parent = PsiTreeUtil.findCommonParent(start, end);
    if (parent == null) {
      return null;
    }
    final TextRange parentRange = parent.getTextRange();
    if (parentRange.getStartOffset() == start.getTextRange().getStartOffset() &&
        parentRange.getEndOffset() == end.getTextRange().getEndOffset()) {
      return parent;
    }
    return null;
  }

  @Nullable
  private static PsiElement findClosestParentAfterLineBreak(PsiElement element) {
    PsiElement parent = element;
    while (parent != null && !(parent instanceof PsiFileSystemItem)) {
      PsiElement prev = parent.getPrevSibling();
      while (prev != null && prev.getTextLength() <= 0) {
        prev = prev.getPrevSibling();
      }
      if (firstElementInFile(parent)) {
        return parent.getContainingFile();
      }
      else if (isWhiteSpaceWithLineFeed(prev)) {
        return parent;
      }
      parent = parent.getParent();
    }
    return null;
  }

  private static boolean firstElementInFile(@NotNull PsiElement element) {
    return element.getTextOffset() == 0;
  }

  @Nullable
  private static PsiElement findClosestParentBeforeLineBreak(PsiElement element) {
    PsiElement parent = element;
    while (parent != null && !(parent instanceof PsiFileSystemItem)) {
      final PsiElement next = parent.getNextSibling();
      if (lastElementInFile(parent)) {
        return parent.getContainingFile();
      }
      else if (isWhiteSpaceWithLineFeed(next)) {
        return parent;
      }
      parent = parent.getParent();
    }
    return null;
  }

  private static boolean lastElementInFile(@NotNull PsiElement element) {
    return element.getTextRange().getEndOffset() == element.getContainingFile().getTextRange().getEndOffset();
  }

  private static boolean isWhiteSpaceWithLineFeed(@Nullable PsiElement element) {
    if (element == null) {
      return false;
    }
    if (element instanceof PsiWhiteSpace) {
      return element.textContains('\n');
    }
    final ASTNode node = element.getNode();
    if (node == null) {
      return false;
    }
    final CharSequence text = node.getChars();
    boolean lineFeedFound = false;
    for (int i = 0; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (!StringUtil.isWhiteSpace(c)) {
        return false;
      }
      lineFeedFound |= c == '\n';
    }
    return lineFeedFound;
  }

  @NotNull
  @Override
  public Surrounder[] getSurrounders() {
    return SURROUNDERS;
  }

  @Override
  public boolean isExclusive() {
    return false;
  }

  private static class CustomFoldingRegionSurrounder implements Surrounder {

    private final CustomFoldingProvider myProvider;

    CustomFoldingRegionSurrounder(@NotNull CustomFoldingProvider provider) {
      myProvider = provider;
    }

    @Override
    public String getTemplateDescription() {
      return myProvider.getDescription();
    }

    @Override
    public boolean isApplicable(@NotNull PsiElement[] elements) {
      if (elements.length == 0) return false;
      if (elements[0].getContainingFile() instanceof PsiCodeFragment) {
        return false;
      }
      for (FoldingBuilder each : LanguageFolding.INSTANCE.allForLanguage(elements[0].getLanguage())) {
        if (each instanceof CustomFoldingBuilder) return true;
      }
      return false;
    }

    @Override
    public TextRange surroundElements(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement[] elements)
      throws IncorrectOperationException {
      if (elements.length == 0) return null;
      PsiElement firstElement = elements[0];
      PsiElement lastElement = elements[elements.length - 1];
      PsiFile psiFile = firstElement.getContainingFile();
      Language language = psiFile.getLanguage();
      Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
      if (commenter == null) return null;
      String linePrefix = commenter.getLineCommentPrefix();
      String lineSuffix = "";
      if (linePrefix == null) {
        linePrefix = commenter.getBlockCommentPrefix();
        lineSuffix = StringUtil.notNullize(commenter.getBlockCommentSuffix());
      }
      if (linePrefix == null) return null;
      int prefixLength = linePrefix.length();

      int startOffset = firstElement.getTextRange().getStartOffset();
      final Document document = editor.getDocument();
      final int startLineNumber = document.getLineNumber(startOffset);
      final String startIndent = document.getText(new TextRange(document.getLineStartOffset(startLineNumber), startOffset));
      int endOffset = lastElement.getTextRange().getEndOffset();
      int delta = 0;
      TextRange rangeToSelect = TextRange.create(startOffset, startOffset);
      String startText = myProvider.getStartString();
      int descPos = startText.indexOf("?");
      if (descPos >= 0) {
        startText = startText.replace("?", DEFAULT_DESC_TEXT);
        rangeToSelect = TextRange.from(startOffset + descPos, DEFAULT_DESC_TEXT.length());
      }

      String startString = linePrefix + startText + lineSuffix + "\n" + startIndent;
      String endString = "\n" + startIndent + linePrefix + myProvider.getEndString() + lineSuffix;
      document.insertString(endOffset, endString);
      delta += endString.length();
      document.insertString(startOffset, startString);
      delta += startString.length();
      
      RangeMarker rangeMarkerToSelect = document.createRangeMarker(rangeToSelect.shiftRight(prefixLength));
      PsiDocumentManager.getInstance(project).commitDocument(document);
      adjustLineIndent(project, psiFile, language, TextRange.from(endOffset + delta - endString.length(), endString.length()));
      adjustLineIndent(project, psiFile, language, TextRange.from(startOffset, startString.length()));
      rangeToSelect = TextRange.create(rangeMarkerToSelect.getStartOffset(), rangeMarkerToSelect.getEndOffset());
      rangeMarkerToSelect.dispose();
      return rangeToSelect;
    }

    private static void adjustLineIndent(@NotNull Project project, PsiFile file, Language language, TextRange range) {
      CommonCodeStyleSettings formatSettings = CodeStyle.getLanguageSettings(file, language);
      boolean keepAtFirstCol = formatSettings.KEEP_FIRST_COLUMN_COMMENT;
      formatSettings.KEEP_FIRST_COLUMN_COMMENT = false;
      CodeStyleManager.getInstance(project).adjustLineIndent(file, range);
      formatSettings.KEEP_FIRST_COLUMN_COMMENT = keepAtFirstCol;
    }
  }
}
