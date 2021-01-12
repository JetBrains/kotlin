// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.codeInsight.highlighting.CodeBlockSupportHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public final class CodeBlockUtil {
  private CodeBlockUtil() {
  }

  private static Language getBraceType(HighlighterIterator iterator) {
    final IElementType type = iterator.getTokenType();
    return type.getLanguage();
  }

  public static void moveCaretToCodeBlockEnd(Project project, Editor editor, boolean isWithSelection) {
    Document document = editor.getDocument();
    int selectionStart = editor.getSelectionModel().getLeadSelectionOffset();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return;

    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
    final CodeBlockProvider provider = CodeBlockProviders.INSTANCE.forLanguage(file.getLanguage());
    if (provider != null) {
      final TextRange range = provider.getCodeBlockRange(editor, file);
      if (range != null) {
        editor.getCaretModel().moveToOffset(range.getEndOffset());
      }
    }
    else {
      final IndentGuideDescriptor guide = editor.getIndentsModel().getCaretIndentGuide();
      if (guide != null) {
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(guide.endLine, guide.indentLevel));
      }
      else {
        int endOffset = calcBlockEndOffset(editor, file);
        if (endOffset != -1) {
          editor.getCaretModel().moveToOffset(endOffset);
        }
      }
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

    if (isWithSelection) {
      editor.getSelectionModel().setSelection(selectionStart, editor.getCaretModel().getOffset());
    }
    else {
      editor.getSelectionModel().removeSelection();
    }
  }

  public static void moveCaretToCodeBlockStart(Project project, Editor editor, boolean isWithSelection) {
    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    int selectionStart = editor.getSelectionModel().getLeadSelectionOffset();
    if (file == null) return;

    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();

    final CodeBlockProvider provider = CodeBlockProviders.INSTANCE.forLanguage(file.getLanguage());
    if (provider != null) {
      final TextRange range = provider.getCodeBlockRange(editor, file);
      if (range != null) {
        editor.getCaretModel().moveToOffset(range.getStartOffset());
      }
    }
    else {
      final IndentGuideDescriptor guide = editor.getIndentsModel().getCaretIndentGuide();
      if (guide != null && guide.startLine != editor.getCaretModel().getLogicalPosition().line) {
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(guide.startLine, guide.indentLevel));
      }
      else {
        int start = calcBlockStartOffset(editor, file);
        if (start < 0) return;
        editor.getCaretModel().moveToOffset(start);
      }
    }


    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

    if (isWithSelection) {
      editor.getSelectionModel().setSelection(selectionStart, editor.getCaretModel().getOffset());
    }
    else {
      editor.getSelectionModel().removeSelection();
    }
  }

  private static int calcBlockEndOffset(@NotNull Editor editor, @NotNull PsiFile file) {
    int offsetFromBraceMatcher = calcBlockEndOffsetFromBraceMatcher(editor, file);
    TextRange rangeFromStructuralSupport = CodeBlockSupportHandler.findCodeBlockRange(editor, file);
    if (rangeFromStructuralSupport.isEmpty()) {
      return offsetFromBraceMatcher;
    }
    else if (offsetFromBraceMatcher == -1) {
      return rangeFromStructuralSupport.getEndOffset();
    }
    else {
      return Math.min(rangeFromStructuralSupport.getEndOffset(), offsetFromBraceMatcher);
    }
  }

  private static int calcBlockEndOffsetFromBraceMatcher(@NotNull Editor editor, @NotNull PsiFile file) {
    Document document = editor.getDocument();
    int offset = editor.getCaretModel().getOffset();
    final FileType fileType = getFileType(file, offset);
    HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
    if (iterator.atEnd()) return -1;

    int depth = 0;
    Language braceType;
    boolean isBeforeLBrace = false;
    if (isLStructuralBrace(fileType, iterator, document.getCharsSequence())) {
      isBeforeLBrace = true;
      depth = -1;
      braceType = getBraceType(iterator);
    } else {
      braceType = null;
    }

    boolean moved = false;
    while (true) {
      if (iterator.atEnd()) return -1;

      if (isRStructuralBrace(fileType, iterator, document.getCharsSequence()) &&
          (braceType == getBraceType(iterator) ||
           braceType == null
          )
      ) {
        if (moved) {
          if (depth == 0) break;
          depth--;
        }

        if (braceType == null) {
          braceType = getBraceType(iterator);
        }
      }
      else if (isLStructuralBrace(fileType, iterator, document.getCharsSequence()) &&
               (braceType == getBraceType(iterator) ||
                braceType == null
               )
      ) {
        if (braceType == null) {
          braceType = getBraceType(iterator);
        }
        depth++;
      }

      moved = true;
      iterator.advance();
    }

    return isBeforeLBrace ? iterator.getEnd() : iterator.getStart();
  }

  private static int calcBlockStartOffset(@NotNull Editor editor, @NotNull PsiFile file) {
    int offsetFromBraceMatcher = calcBlockStartOffsetFromBraceMatcher(editor, file);
    TextRange rangeFromStructuralSupport = CodeBlockSupportHandler.findCodeBlockRange(editor, file);
    if (rangeFromStructuralSupport.isEmpty()) {
      return offsetFromBraceMatcher;
    }
    else if (offsetFromBraceMatcher == -1) {
      return rangeFromStructuralSupport.getStartOffset();
    }
    else {
      return Math.max(rangeFromStructuralSupport.getStartOffset(), offsetFromBraceMatcher);
    }
  }

  private static int calcBlockStartOffsetFromBraceMatcher(Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset() - 1;
    if (offset < 0) return -1;

    Document document = editor.getDocument();
    final FileType fileType = getFileType(file, offset);
    HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);

    int depth = 0;
    Language braceType;
    boolean isAfterRBrace = false;
    if (isRStructuralBrace(fileType, iterator, document.getCharsSequence())) {
      isAfterRBrace = true;
      depth = -1;
      braceType = getBraceType(iterator);
    }
    else {
      braceType = null;
    }

    boolean moved = false;
    while (true) {
      if (iterator.atEnd()) return -1;

      if (isLStructuralBrace(fileType, iterator, document.getCharsSequence()) &&
          (braceType == getBraceType(iterator) || braceType == null)) {
        if (braceType == null) {
          braceType = getBraceType(iterator);
        }

        if (moved) {
          if (depth == 0) break;
          depth--;
        }
      }
      else if (isRStructuralBrace(fileType, iterator, document.getCharsSequence()) &&
               (braceType == getBraceType(iterator) || braceType == null)) {
        if (braceType == null) {
          braceType = getBraceType(iterator);
        }
        depth++;
      }

      moved = true;
      iterator.retreat();
    }

    return isAfterRBrace ? iterator.getStart() : iterator.getEnd();
  }

  @NotNull
  private static FileType getFileType(PsiFile file, int offset) {
    PsiElement psiElement = file.findElementAt(offset);
    if (psiElement != null) {
      return psiElement.getContainingFile().getFileType();
    }
    else {
      return file.getFileType();
    }
  }

  private static boolean isLStructuralBrace(final FileType fileType, HighlighterIterator iterator, CharSequence fileText) {
    return BraceMatchingUtil.isLBraceToken(iterator, fileText, fileType) && BraceMatchingUtil.isStructuralBraceToken(fileType, iterator,fileText);
  }

  private static boolean isRStructuralBrace(final FileType fileType, HighlighterIterator iterator, CharSequence fileText) {
    return BraceMatchingUtil.isRBraceToken(iterator, fileText, fileType) && BraceMatchingUtil.isStructuralBraceToken(fileType, iterator,fileText);
  }
}
