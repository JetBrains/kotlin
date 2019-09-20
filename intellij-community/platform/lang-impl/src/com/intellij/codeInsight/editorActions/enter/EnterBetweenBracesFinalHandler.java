// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.enter;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.CodeDocumentationUtil;
import com.intellij.codeInsight.editorActions.EnterHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Please, don't extend the class.
 * Use the {@code EnterBetweenBracesDelegate} language-specific implementation instead.
 */
public class EnterBetweenBracesFinalHandler extends EnterHandlerDelegateAdapter {
  @Override
  public Result preprocessEnter(@NotNull final PsiFile file,
                                @NotNull final Editor editor,
                                @NotNull final Ref<Integer> caretOffsetRef,
                                @NotNull final Ref<Integer> caretAdvance,
                                @NotNull final DataContext dataContext,
                                final EditorActionHandler originalHandler) {
    if (!CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
      return Result.Continue;
    }

    Document document = editor.getDocument();
    CharSequence text = document.getCharsSequence();
    int caretOffset = caretOffsetRef.get().intValue();

    final EnterBetweenBracesDelegate helper = getLanguageImplementation(EnterHandler.getLanguage(dataContext));
    if (!isApplicable(file, editor, text, caretOffset, helper)) {
      return Result.Continue;
    }

    final int line = document.getLineNumber(caretOffset);
    final int start = document.getLineStartOffset(line);
    final CodeDocumentationUtil.CommentContext commentContext =
      CodeDocumentationUtil.tryParseCommentContext(file, text, caretOffset, start);

    // special case: enter inside "()" or "{}"
    String indentInsideJavadoc = helper.isInComment(file, editor, caretOffset) && commentContext.docAsterisk
                                 ? CodeDocumentationUtil.getIndentInsideJavadoc(document, caretOffset)
                                 : null;

    originalHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);

    Project project = editor.getProject();
    if (indentInsideJavadoc != null &&
        project != null &&
        CodeStyleManager.getInstance(project).getDocCommentSettings(file).isLeadingAsteriskEnabled()) {
      document.insertString(editor.getCaretModel().getOffset(), "*" + indentInsideJavadoc);
    }

    helper.formatAtOffset(file, editor, editor.getCaretModel().getOffset(), EnterHandler.getLanguage(dataContext));
    return indentInsideJavadoc == null ? Result.Continue : Result.DefaultForceIndent;
  }

  protected boolean isApplicable(@NotNull PsiFile file,
                                 @NotNull Editor editor,
                                 CharSequence documentText,
                                 int caretOffset,
                                 EnterBetweenBracesDelegate helper) {
    int prevCharOffset = CharArrayUtil.shiftBackward(documentText, caretOffset - 1, " \t");
    int nextCharOffset = CharArrayUtil.shiftForward(documentText, caretOffset, " \t");
    return isValidOffset(prevCharOffset, documentText) &&
           isValidOffset(nextCharOffset, documentText) &&
           helper.isBracePair(documentText.charAt(prevCharOffset), documentText.charAt(nextCharOffset)) &&
           !helper.bracesAreInTheSameElement(file, editor, prevCharOffset, nextCharOffset);
  }

  @NotNull
  protected EnterBetweenBracesDelegate getLanguageImplementation(@Nullable Language language) {
    if (language != null) {
      final EnterBetweenBracesDelegate helper = EnterBetweenBracesDelegate.EP_NAME.forLanguage(language);
      if (helper != null) {
        return helper;
      }
    }
    return ourDefaultBetweenDelegate;
  }

  protected static EnterBetweenBracesDelegate ourDefaultBetweenDelegate = new EnterBetweenBracesDelegate();

  protected static boolean isValidOffset(int offset, CharSequence text) {
    return offset >= 0 && offset < text.length();
  }
}
