// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.generation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageSurrounders;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

public class SurroundWithAction extends BaseCodeInsightAction{
  public SurroundWithAction() {
    setEnabledInModalContext(true);
  }

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler(){
    return new SurroundWithHandler();
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull final PsiFile file) {
    final Language language = file.getLanguage();
    if (!LanguageSurrounders.INSTANCE.allForLanguage(language).isEmpty()) {
      return true;
    }
    final PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
    if (baseFile != null && baseFile != file && !LanguageSurrounders.INSTANCE.allForLanguage(baseFile.getLanguage()).isEmpty()) {
      return true;
    }

    if (!TemplateManagerImpl.listApplicableTemplateWithInsertingDummyIdentifier(
      TemplateActionContext.surrounding(file, editor)).isEmpty()) {
      return true;
    }

    return false;
  }
}