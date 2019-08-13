// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.enter;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Please, use the {@code EnterBetweenBracesDelegate} language-specific implementation instead.
 */
@Deprecated
public class EnterBetweenBracesHandler extends EnterBetweenBracesFinalHandler {
  @Override
  protected boolean isApplicable(@NotNull PsiFile file,
                                 @NotNull Editor editor,
                                 CharSequence documentText,
                                 int caretOffset,
                                 EnterBetweenBracesDelegate helper) {
    int prevCharOffset = CharArrayUtil.shiftBackward(documentText, caretOffset - 1, " \t");
    int nextCharOffset = CharArrayUtil.shiftForward(documentText, caretOffset, " \t");
    return isValidOffset(prevCharOffset, documentText) &&
           isValidOffset(nextCharOffset, documentText) &&
           isBracePair(documentText.charAt(prevCharOffset), documentText.charAt(nextCharOffset)) &&
           !ourDefaultBetweenDelegate.bracesAreInTheSameElement(file, editor, prevCharOffset, nextCharOffset);
  }

  protected boolean isBracePair(char lBrace, char rBrace) {
    return ourDefaultBetweenDelegate.isBracePair(lBrace, rBrace);
  }
}
