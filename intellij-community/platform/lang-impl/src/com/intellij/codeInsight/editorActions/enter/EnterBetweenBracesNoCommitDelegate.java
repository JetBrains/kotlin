// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.enter;

import com.intellij.codeInsight.editorActions.EnterHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public abstract class EnterBetweenBracesNoCommitDelegate extends EnterBetweenBracesDelegate {
  @Override
  public boolean bracesAreInTheSameElement(@NotNull PsiFile file, @NotNull Editor editor, int lBraceOffset, int rBraceOffset) {
    final HighlighterIterator it = createBeforeIterator((EditorEx)editor, lBraceOffset + 1);
    while(!it.atEnd() && it.getStart() < rBraceOffset) {
      it.advance();
      if (it.getStart() == rBraceOffset) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isInComment(@NotNull PsiFile file, @NotNull Editor editor, int offset) {
    final HighlighterIterator it = createBeforeIterator((EditorEx)editor, offset);
    return !it.atEnd() && isCommentType(it.getTokenType());
  }

  public abstract boolean isCommentType(IElementType type);

  @Override
  protected void formatAtOffset(@NotNull PsiFile file, @NotNull Editor editor, int offset, Language language) {
    EnterHandler.adjustLineIndentNoCommit(language,
                                          editor.getDocument(),
                                          editor,
                                          offset);
  }

  @NotNull
  public static HighlighterIterator createBeforeIterator(@NotNull EditorEx editor, int caretOffset) {
    return editor.getHighlighter().createIterator(caretOffset == 0 ? 0 : caretOffset - 1);
  }
}