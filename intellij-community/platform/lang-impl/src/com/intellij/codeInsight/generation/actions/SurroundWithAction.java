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

package com.intellij.codeInsight.generation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
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

    if (!TemplateManagerImpl.listApplicableTemplateWithInsertingDummyIdentifier(editor, file, true).isEmpty()) {
      return true;
    }

    return false;
  }
}