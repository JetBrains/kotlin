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

package com.intellij.codeInsight.editorActions.enter;

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
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

public class EnterInLineCommentHandler extends EnterHandlerDelegateAdapter {
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
    int lineCommentStartOffset = getLineCommentStartOffset(editor, caretOffset, commenter);
    if (lineCommentStartOffset < 0) return Result.Continue;

    Document document = editor.getDocument();
    CharSequence text = document.getImmutableCharSequence();
    final int offset = CharArrayUtil.shiftForward(text, caretOffset, WHITESPACE);
    if (offset >= document.getTextLength() || text.charAt(offset) == '\n') return Result.Continue;

    String prefix = commenter.getLineCommentPrefix();
    assert prefix != null : "Line Comment type is set but Line Comment Prefix is null!";
    String prefixTrimmed = prefix.trim();

    int beforeCommentOffset = CharArrayUtil.shiftBackward(text, lineCommentStartOffset - 1, WHITESPACE);
    boolean onlyCommentInCaretLine = beforeCommentOffset < 0 || text.charAt(beforeCommentOffset) == '\n';

    CharSequence spacing = " ";
    if (StringUtil.startsWith(text, offset, prefix)) {
      int afterPrefix = offset + prefixTrimmed.length();
      if (afterPrefix < document.getTextLength() && text.charAt(afterPrefix) != ' ') {
        document.insertString(afterPrefix, spacing);
      }
      caretOffsetRef.set(offset);
    }
    else {
      if (onlyCommentInCaretLine) {
        int indentStart = lineCommentStartOffset + prefix.trim().length();
        int indentEnd = CharArrayUtil.shiftForward(text, indentStart, WHITESPACE);
        CharSequence currentLineSpacing = text.subSequence(indentStart, indentEnd);
        if (TodoConfiguration.getInstance().isMultiLine() &&
            EnterInCommentUtil.isTodoText(text, lineCommentStartOffset, caretOffset) &&
            EnterInCommentUtil.isTodoText(text, lineCommentStartOffset, DocumentUtil.getLineEndOffset(lineCommentStartOffset, document))) {
          spacing = currentLineSpacing + " ";
        }
        else if (currentLineSpacing.length() > 0) {
          spacing = currentLineSpacing;
        }
        int textStart = CharArrayUtil.shiftForward(text, caretOffset, WHITESPACE);
        document.deleteString(caretOffset, textStart);
      }
      else {
        if (text.charAt(caretOffset) == ' ') spacing = "";
      }
      document.insertString(caretOffset, prefixTrimmed + spacing);
    }

    if (onlyCommentInCaretLine) {
      caretAdvance.set(prefixTrimmed.length() + spacing.length());
    }
    return Result.DefaultForceIndent;
  }

  private static int getLineCommentStartOffset(@NotNull Editor editor, int offset, @NotNull CodeDocumentationAwareCommenter commenter) {
    if (offset < 1) return -1;
    EditorHighlighter highlighter = ((EditorEx)editor).getHighlighter();
    HighlighterIterator iterator = highlighter.createIterator(offset - 1);
    String prefix = commenter.getLineCommentPrefix();
    return iterator.getTokenType() == commenter.getLineCommentTokenType() &&
           (iterator.getStart() + (prefix == null ? 0 : prefix.length())) <= offset ? iterator.getStart() : -1;
  }
}
