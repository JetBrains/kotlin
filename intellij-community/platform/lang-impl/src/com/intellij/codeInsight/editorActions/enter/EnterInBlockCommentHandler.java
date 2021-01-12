// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.enter;

import com.intellij.codeInsight.editorActions.EnterHandler;
import com.intellij.ide.todo.TodoConfiguration;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

public class EnterInBlockCommentHandler extends EnterHandlerDelegateAdapter {
  private static final String WHITESPACE = " \t";

  @Override
  public Result preprocessEnter(@NotNull final PsiFile file,
                                @NotNull final Editor editor,
                                @NotNull final Ref<Integer> caretOffsetRef,
                                @NotNull final Ref<Integer> caretAdvance,
                                @NotNull final DataContext dataContext,
                                final EditorActionHandler originalHandler) {
    CodeDocumentationAwareCommenter commenter = EnterInCommentUtil.getDocumentationAwareCommenter(dataContext);
    if (commenter == null) return Result.Continue;

    int caretOffset = caretOffsetRef.get();
    int blockCommentStartOffset = getBlockCommentStartOffset(editor, caretOffset, commenter);
    if (blockCommentStartOffset < 0) return Result.Continue;

    Document document = editor.getDocument();
    CharSequence text = document.getImmutableCharSequence();

    String docCommentPrefix = commenter.getDocumentationCommentPrefix();
    if (docCommentPrefix != null && StringUtil.startsWith(text.subSequence(blockCommentStartOffset, caretOffset), docCommentPrefix)) {
      return Result.Continue;
    }

    int beforeWhitespace = CharArrayUtil.shiftBackward(text, blockCommentStartOffset - 1, WHITESPACE);
    if (beforeWhitespace > 0 && text.charAt(beforeWhitespace) != '\n') return Result.Continue;

    PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
    PsiElement element = file.findElementAt(blockCommentStartOffset);
    if (!(element instanceof PsiComment) ||
        ((PsiComment)element).getTokenType() != commenter.getBlockCommentTokenType()) {
      return Result.Continue;
    }
    if (!EnterHandler.isCommentComplete((PsiComment)element, commenter, editor)) {
      int currentEndOfLine = CharArrayUtil.shiftForwardUntil(text, caretOffset, "\n");
      document.insertString(currentEndOfLine,
                            "\n" +
                            text.subSequence(beforeWhitespace + 1, blockCommentStartOffset) + " " + commenter.getBlockCommentSuffix());
      return Result.Default;
    }

    int additionalIndent = 0;
    if (TodoConfiguration.getInstance().isMultiLine()) {
      int lineStartOffset = DocumentUtil.getLineStartOffset(caretOffset, document);
      int todoOffset = EnterInCommentUtil.getTodoTextOffset(text, lineStartOffset, caretOffset);
      if (todoOffset >= 0) {
        int lineEndOffset = DocumentUtil.getLineEndOffset(caretOffset, document);
        if (todoOffset == EnterInCommentUtil.getTodoTextOffset(text, lineStartOffset, lineEndOffset)) {
          int nonWsLineStart = CharArrayUtil.shiftForward(text, lineStartOffset, WHITESPACE);
          if (todoOffset >= nonWsLineStart) {
            additionalIndent = todoOffset - nonWsLineStart + 1;
            document.insertString(caretOffset, StringUtil.repeat(" ", additionalIndent));
          }
        }
      }
    }

    String linePrefix = commenter.getDocumentationCommentLinePrefix();
    if (linePrefix == null) return Result.Continue;

    int refOffset = document.getLineNumber(caretOffset) > document.getLineNumber(blockCommentStartOffset)
                    ? document.getLineStartOffset(document.getLineNumber(blockCommentStartOffset) + 1) : caretOffset;
    if (StringUtil.startsWith(text, CharArrayUtil.shiftForward(text, refOffset, WHITESPACE), linePrefix)) {
      int endOffset = CharArrayUtil.shiftForward(text, caretOffset, WHITESPACE);
      if (endOffset < text.length() && text.charAt(endOffset) != '\n') endOffset = caretOffset;
      int valueLength = linePrefix.length() + 1;
      int endOffsetToReplace = endOffset > caretOffset ? endOffset : caretOffset + Math.min(additionalIndent, valueLength);
      document.replaceString(caretOffset, endOffsetToReplace, linePrefix + " ");
      caretAdvance.set(Math.max(valueLength, additionalIndent));
      return Result.DefaultForceIndent;
    }
    return Result.Continue;
  }

  private static int getBlockCommentStartOffset(@NotNull Editor editor, int offset, @NotNull CodeDocumentationAwareCommenter commenter) {
    EditorHighlighter highlighter = ((EditorEx)editor).getHighlighter();
    HighlighterIterator iterator = highlighter.createIterator(offset);
    if (iterator.atEnd() || iterator.getTokenType() != commenter.getBlockCommentTokenType()) return -1;

    String prefix = ObjectUtils.notNull(commenter.getBlockCommentPrefix(), "");
    int tokenStart = iterator.getStart();
    int tokenEnd = iterator.getEnd();
    int prefixEnd = tokenStart + prefix.length();
    if (prefixEnd > tokenEnd || prefixEnd > offset) return -1;

    String suffix = ObjectUtils.notNull(commenter.getBlockCommentSuffix(), "");
    if (StringUtil.endsWith(editor.getDocument().getImmutableCharSequence().subSequence(prefixEnd, tokenEnd), suffix) &&
        offset > tokenEnd - suffix.length()) {
      return -1;
    }
    return tokenStart;
  }
}
