/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.codeInsight.generation;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim.Mossienko
 */
public interface SelfManagingCommenter<T extends CommenterDataHolder> {
  @Nullable T createLineCommentingState(int startLine, int endLine, @NotNull Document document, @NotNull PsiFile file);
  @Nullable T createBlockCommentingState(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull PsiFile file);
  
  void commentLine(int line, int offset, @NotNull Document document, @NotNull T data);

  void uncommentLine(int line, int offset, @NotNull Document document, @NotNull T data);

  boolean isLineCommented(int line, int offset, @NotNull Document document, @NotNull T data);

  @Nullable
  String getCommentPrefix(int line, @NotNull Document document, @NotNull T data);

  @Nullable TextRange getBlockCommentRange(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull T data);
  @Nullable String getBlockCommentPrefix(int selectionStart, @NotNull Document document, @NotNull T data);

  @Nullable String getBlockCommentSuffix(int selectionEnd, @NotNull Document document, @NotNull T data);

  void uncommentBlockComment(int startOffset,
                             int endOffset,
                             Document document,
                             T data);

  @NotNull TextRange insertBlockComment(int startOffset,
                          int endOffset,
                          Document document,
                          T data);

  CommenterDataHolder EMPTY_STATE = new CommenterDataHolder() {};  
}
