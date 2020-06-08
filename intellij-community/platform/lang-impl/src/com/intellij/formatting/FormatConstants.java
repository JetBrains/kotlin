// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.util.PsiUtilBase.getLanguageInEditor;

/**
 * Is assumed to be a single place to hold various constants to use during formatting.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public final class FormatConstants {

  /**
   * It may be necessary to wrap a long line sometimes (during formatting, when typing reaches right margin etc). We should take
   * into consideration possibility that additional symbols are inserted during that.
   * <p/>
   * For example consider that we want to wrap long string literal:
   * <p/>
   * <pre>
   *                                |
   *                                | <- right margin
   *     "this is a long string to w|rap"
   *                                |
   * </pre>
   * We can't just wrap before 'w' symbol of 'wrap' word because we can get the following situation then:
   * <p/>
   * <pre>
   *                                |
   *                                | <- right margin
   *     "this is a long string to "| + // Notice that '" +' string is inserted at the wrapped line
   *        "wrap"                  |
   * </pre>
   * Hence, we need to reserve particular number of columns.
   * <p/>
   * This constant is assumed to hold language-agnostic number of columns to reserve on smart line wrapping.
   */
  public static final int RESERVED_LINE_WRAP_WIDTH_IN_COLUMNS = 3; // '3' is for breaking string literal: 'quote symbol',
                                                                   // 'space' and 'plus' operator

  private FormatConstants() {
  }

  public static int getReservedLineWrapWidthInColumns(@NotNull Editor editor) {
    return isPlainTextFile(editor) ? 0 : RESERVED_LINE_WRAP_WIDTH_IN_COLUMNS;
  }

  private static boolean isPlainTextFile(@NotNull Editor editor) {
    return editor.getProject() != null && PlainTextLanguage.INSTANCE.is(getLanguageInEditor(editor, editor.getProject()));
  }
}
