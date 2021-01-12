/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.templates;


import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface provides method used in {@link PostfixTemplateWithExpressionSelector}
 */
public interface PostfixTemplateExpressionSelector {

  /**
   * Check that we can select not-null expression(PsiElement) in current context
   */
  boolean hasExpression(@NotNull PsiElement context,
                        @NotNull Document copyDocument,
                        int newOffset);

  /**
   * Return list of all possible expressions in the current position.
   * Postfix template implementation shows popup chooser (if size > 1)
   */
  @NotNull
  List<PsiElement> getExpressions(@NotNull PsiElement context,
                                  @NotNull Document document,
                                  int offset);

  /**
   * returns renderer for expressions from  {@link #getExpressions}.
   * Renderer is used for showing popup chooser
   */
  @NotNull
  Function<PsiElement, String> getRenderer();
}
