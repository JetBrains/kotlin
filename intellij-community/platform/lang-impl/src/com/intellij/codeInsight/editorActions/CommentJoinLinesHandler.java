// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

public class CommentJoinLinesHandler implements JoinRawLinesHandlerDelegate {

  @Override
  public int tryJoinLines(@NotNull Document document, @NotNull PsiFile file, int start, int end) {
    return CANNOT_JOIN;
  }

  @Override
  public int tryJoinRawLines(@NotNull Document document, @NotNull PsiFile file, int start, int end) {
    CharSequence text = document.getText();
    PsiComment prevComment = PsiTreeUtil.getNonStrictParentOfType(file.findElementAt(start - 1), PsiComment.class);
    PsiComment nextComment = PsiTreeUtil.getNonStrictParentOfType(file.findElementAt(end), PsiComment.class);
    if (prevComment == null || nextComment == null) return CANNOT_JOIN;
    boolean sameComment = prevComment == nextComment;
    boolean adjacentLineComments = false;
    Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(file.getLanguage());
    if (commenter == null) return CANNOT_JOIN;

    String blockCommentSuffix = commenter.getBlockCommentSuffix();

    if ("*/".equals(blockCommentSuffix) && text.charAt(end) == '*' && end < text.length() && text.charAt(end + 1) != '/') {
      /* remove leading asterisk
       * <-- like this
       */
      end = StringUtil.skipWhitespaceForward(text, end + 1);
    }
    else if (!sameComment && !(blockCommentSuffix != null && CharArrayUtil.regionMatches(text, start - 2, blockCommentSuffix))) {
      for (String lineCommentPrefix : commenter.getLineCommentPrefixes()) {
        if (CharArrayUtil.regionMatches(text, end, lineCommentPrefix)) {
          // merge two line comments
          // like this
          adjacentLineComments = true;
          end = StringUtil.skipWhitespaceForward(text, end + lineCommentPrefix.length());
          int lineNumber = document.getLineNumber(start);
          int lineStart = document.getLineStartOffset(lineNumber);
          int nextEnd = document.getLineEndOffset(lineNumber + 1);
          int margin = CodeStyle.getSettings(file).getRightMargin(file.getLanguage());
          int lineLength = start - lineStart;
          if (lineLength <= margin && lineLength + (nextEnd - end) + 1 > margin) {
            // Respect right margin
            int allowedEnd = end + margin - lineLength - 1;
            assert allowedEnd < nextEnd;
            while (allowedEnd > end && !Character.isWhitespace(text.charAt(allowedEnd))) {
              allowedEnd--;
            }
            if (allowedEnd <= end) {
              // do nothing, just move the caret
              return end;
            }
            int endOfMovedPart = StringUtil.skipWhitespaceBackward(text, allowedEnd);
            CharSequence toMove = text.subSequence(end, endOfMovedPart);
            int lineBreakPos = CharArrayUtil.indexOf(text, "\n", start);
            document.deleteString(end, allowedEnd + 1);
            document.replaceString(start, lineBreakPos, " " + toMove);
            return start + 1 + toMove.length() + (end - lineBreakPos);
          }
        }
      }
    }

    document.replaceString(start, end, adjacentLineComments || sameComment ? " " : "");
    return start;
  }
}
