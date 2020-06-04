/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class SliceBackwardAction extends CodeInsightAction {
  @NotNull
  @Override
  protected SliceHandler getHandler() {
    return SliceHandler.create(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (LanguageSlicing.hasAnyProviders()) super.update(e);
    else e.getPresentation().setEnabledAndVisible(false);
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (LanguageSlicing.getProvider(file) == null) {
      return false;
    }
    PsiElement expression = getHandler().getExpressionAtCaret(editor, file);
    return expression == null || expression.isPhysical();
  }
}
