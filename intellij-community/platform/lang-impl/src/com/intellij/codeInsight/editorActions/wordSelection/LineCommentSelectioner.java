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

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LineCommentSelectioner extends WordSelectioner {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    if (e instanceof PsiComment) {
      final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(e.getLanguage());
      if (!(commenter instanceof CodeDocumentationAwareCommenter)) return true;
      return !((CodeDocumentationAwareCommenter) commenter).isDocumentationComment((PsiComment)e);
    }
    return false;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement element, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    List<TextRange> result = super.select(element, editorText, cursorOffset, editor);


    PsiElement firstComment = element;
    PsiElement e = element;

    while (e.getPrevSibling() != null) {
      if (e instanceof PsiComment) {
        firstComment = e;
      }
      else if (!(e instanceof PsiWhiteSpace)) {
        break;
      }
      e = e.getPrevSibling();
    }

    PsiElement lastComment = element;
    e = element;
    while (e.getNextSibling() != null) {
      if (e instanceof PsiComment) {
        lastComment = e;
      }
      else if (!(e instanceof PsiWhiteSpace)) {
        break;
      }
      e = e.getNextSibling();
    }


    result.addAll(expandToWholeLine(editorText, new TextRange(firstComment.getTextRange().getStartOffset(),
                                                              lastComment.getTextRange().getEndOffset())));

    return result;
  }
}
