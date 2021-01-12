// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.generation;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Commenter that may provide a complex commenting logic. For simple wrapping/prefixing cases may be enough to use {@link Commenter}
 *
 * @see Commenter
 * @see EscapingCommenter
 * @see CodeDocumentationAwareCommenter
 * @see IndentedCommenter
 * @see CommenterWithLineSuffix
 * @see SelfManagingCommenterUtil
 */
public interface SelfManagingCommenter<T extends CommenterDataHolder> {

  @Nullable T createLineCommentingState(int startLine, int endLine, @NotNull Document document, @NotNull PsiFile file);

  @Nullable T createBlockCommentingState(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull PsiFile file);

  void commentLine(int line, int offset, @NotNull Document document, @NotNull T data);

  void uncommentLine(int line, int offset, @NotNull Document document, @NotNull T data);

  boolean isLineCommented(int line, int offset, @NotNull Document document, @NotNull T data);

  /**
   * @see Commenter#getLineCommentPrefix()
   */
  @Nullable String getCommentPrefix(int line, @NotNull Document document, @NotNull T data);

  /**
   * @see SelfManagingCommenterUtil#getBlockCommentRange(int, int, Document, String, String)
   */
  @Nullable TextRange getBlockCommentRange(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull T data);

  /**
   * @see Commenter#getBlockCommentPrefix()
   */
  @Nullable String getBlockCommentPrefix(int selectionStart, @NotNull Document document, @NotNull T data);

  /**
   * @see Commenter#getBlockCommentSuffix()
   */
  @Nullable String getBlockCommentSuffix(int selectionEnd, @NotNull Document document, @NotNull T data);

  /**
   * @see SelfManagingCommenterUtil#uncommentBlockComment(int, int, Document, String, String)
   */
  void uncommentBlockComment(int startOffset,
                             int endOffset,
                             Document document,
                             T data);

  /**
   * @return text range from opener start till the closer end or null if wrapping failed for some reason
   * @see SelfManagingCommenterUtil#insertBlockComment(int, int, Document, String, String)
   */
  @Nullable TextRange insertBlockComment(int startOffset,
                                         int endOffset,
                                         Document document,
                                         T data);

  CommenterDataHolder EMPTY_STATE = new CommenterDataHolder() {
  };
}
