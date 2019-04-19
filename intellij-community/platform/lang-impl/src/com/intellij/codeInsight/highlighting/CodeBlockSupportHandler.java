// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.openapi.util.TextRange.EMPTY_RANGE;

/**
 * Provides information about complex language structures, like multi-block expressions: E.g. if/elisf/else/end
 * Used to:
 * - highlight markers or keywords if one of them is under cursor
 * - navigate to the begin/end of the block
 *
 * @see com.intellij.codeInsight.highlighting.BraceMatcher
 * @see com.intellij.codeInsight.editorActions.CodeBlockProvider
 */
@ApiStatus.Experimental
public interface CodeBlockSupportHandler {
  LanguageExtension<CodeBlockSupportHandler> EP = new LanguageExtension<>("com.intellij.codeBlockSupportHandler");

  /**
   * Checks if code block marker(or keyword) is under cursor and collects related markers: e.g. finds {@code if}/{@code elsif}/{@code else}
   * if cursor is on the {@code else} from this compound statement
   *
   * @return list of ranges of related structural markers or empty list if there is no structural marker under cursor or it should not be
   * handled (highlighted) by any reason.
   * @apiNote platform iterates handlers, until some returns a non-empty list of ranges
   */
  @NotNull
  List<TextRange> getCodeBlockMarkerRanges(@NotNull PsiElement elementAtCursor);

  /**
   * Checks if cursor is inside the block supported by this handler
   *
   * @return range of the smallest code block cursor is in or empty range if we are not in code block
   */
  @NotNull
  TextRange getCodeBlockRange(@NotNull PsiElement elementAtCursor);

  /**
   * Attempts to find code block range using extension points
   */
  @NotNull
  static TextRange findCodeBlockRange(@NotNull Editor editor,
                                      @NotNull PsiFile psiFile) {
    PsiElement contextElement = psiFile.findElementAt(
      TargetElementUtil.adjustOffset(psiFile, editor.getDocument(), editor.getCaretModel().getOffset()));
    if (contextElement == null) {
      return EMPTY_RANGE;
    }

    for (CodeBlockSupportHandler handler : EP.allForLanguage(contextElement.getLanguage())) {
      TextRange codeBlockRange = handler.getCodeBlockRange(contextElement);
      if (!codeBlockRange.isEmpty()) {
        return codeBlockRange;
      }
    }
    return EMPTY_RANGE;
  }
}

