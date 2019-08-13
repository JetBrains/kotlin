// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.hint.ShowContainerInfoHandler;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context info action
 */
public class ShowContainerInfoAction extends BaseCodeInsightAction{
  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new ShowContainerInfoHandler();
  }

  @Override
  @Nullable
  protected Editor getBaseEditor(@NotNull final DataContext dataContext, @NotNull final Project project) {
    return CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(dataContext);
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull final PsiFile file) {
    return LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file) instanceof TreeBasedStructureViewBuilder;
  }
}