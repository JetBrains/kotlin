/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.hints;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class ToggleCompletionHintsAction extends ToggleAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (psiFile == null || !"Java".equals(psiFile.getLanguage().getDisplayName())) {
      e.getPresentation().setVisible(false);
    }
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION = state;
  }
}
