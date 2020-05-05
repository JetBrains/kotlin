/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.slicer;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SliceLanguageSupportProvider {
  @NotNull
  SliceUsage createRootUsage(@NotNull PsiElement element, @NotNull SliceAnalysisParams params) ;

  @Nullable
  PsiElement getExpressionAtCaret(@NotNull PsiElement atCaret, boolean dataFlowToThis);

  @NotNull
  PsiElement getElementForDescription(@NotNull PsiElement element);

  /**
   * @param expression expression (previously returned from {@link #getExpressionAtCaret(PsiElement, boolean)}.
   * @return true if value filters are supported
   */
  default boolean supportValueFilters(@NotNull PsiElement expression) {
    return false;
  }

  /**
   * @param expression expression (previously returned from {@link #getExpressionAtCaret(PsiElement, boolean)}.
   * @param filter user-entered filter string
   * @return parsed {@link SliceValueFilter}
   * @throws SliceFilterParseException if string cannot be parsed or filtering is not supported
   */
  default @NotNull SliceValueFilter parseFilter(@NotNull PsiElement expression, @NotNull String filter)
    throws SliceFilterParseException {
    throw new SliceFilterParseException(LangBundle.message("slice.filter.not.supported"));
  }

  @NotNull
  SliceUsageCellRendererBase getRenderer();

  void startAnalyzeLeafValues(@NotNull AbstractTreeStructure structure, @NotNull Runnable finalRunnable);

  void startAnalyzeNullness(@NotNull AbstractTreeStructure structure, @NotNull Runnable finalRunnable);

  void registerExtraPanelActions(@NotNull DefaultActionGroup group, @NotNull SliceTreeBuilder builder);
}
