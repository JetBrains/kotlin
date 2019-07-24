/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package com.intellij.formatting;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 */
public interface FormattingModelBuilderEx extends FormattingModelBuilder {
  
  /**
   * Requests building the formatting model for a section of the file containing
   * the specified PSI element and its children.
   *
   * @param element  the top element for which formatting is requested.
   * @param settings the code style settings used for formatting.
   * @param mode     formatting mode
   * @return the formatting model for the file.
   * @see FormattingModelBuilderEx#createModel(PsiElement, TextRange, CodeStyleSettings, FormattingMode)
   */
  @NotNull
  FormattingModel createModel(@NotNull final PsiElement element, @NotNull final CodeStyleSettings settings, @NotNull FormattingMode mode);

  /**
   * Requests building the formatting model for a section of the file containing
   * the specified PSI element and its children.
   *
   * @param element  the top element for which formatting is requested.
   * @param range    the range for which a model should be built.
   * @param settings the code style settings used for formatting.
   * @param mode     formatting mode.
   * @return the formatting model for the file.
   */
  @NotNull
  default FormattingModel createModel(@NotNull final PsiElement element,
                                      @NotNull final TextRange range,
                                      @NotNull final CodeStyleSettings settings,
                                      @NotNull final FormattingMode mode) {
    return createModel(element, settings, mode); // just for compatibility with old implementations
  }

  /**
   * Allows to adjust indent options to used during performing formatting operation on the given ranges of the given file.
   * <p/>
   * Default algorithm is to query given settings for indent options using given file's language as a key.
   * 
   * @param file      target file which content is going to be reformatted
   * @param ranges    given file's ranges to reformat
   * @param settings  code style settings holder
   * @return          indent options to use for the target formatting operation (if any adjustment is required);
   *                  {@code null} to trigger default algorithm usage
   * @deprecated Use {@link com.intellij.psi.codeStyle.FileIndentOptionsProvider} instead.
   * @see com.intellij.psi.codeStyle.FileIndentOptionsProvider
   */
  @Nullable
  @Deprecated
  CommonCodeStyleSettings.IndentOptions getIndentOptionsToUse(@NotNull PsiFile file,
                                                              @NotNull FormatTextRanges ranges,
                                                              @NotNull CodeStyleSettings settings);
}
