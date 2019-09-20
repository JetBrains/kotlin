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
package com.intellij.psi.templateLanguages;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.ErrorQuickFixProvider;
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.LangBundle;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class TemplateLanguageErrorQuickFixProvider implements ErrorQuickFixProvider{

  @Override
  public void registerErrorQuickFix(@NotNull final PsiErrorElement errorElement, @NotNull final HighlightInfo highlightInfo) {
    final PsiFile psiFile = errorElement.getContainingFile();
    final FileViewProvider provider = psiFile.getViewProvider();
    if (!(provider instanceof TemplateLanguageFileViewProvider)) return;
    if (psiFile.getLanguage() != ((TemplateLanguageFileViewProvider) provider).getTemplateDataLanguage()) return;

    QuickFixAction.registerQuickFixAction(highlightInfo, createChangeTemplateDataLanguageFix(errorElement));

  }

  public static IntentionAction createChangeTemplateDataLanguageFix(final PsiElement errorElement) {
    final PsiFile containingFile = errorElement.getContainingFile();
    final VirtualFile virtualFile = containingFile.getVirtualFile();
    final Language language = ((TemplateLanguageFileViewProvider) containingFile.getViewProvider()).getTemplateDataLanguage();
    return new IntentionAction() {

      @Override
      @NotNull
      public String getText() {
        return LangBundle.message("quickfix.change.template.data.language.text", language.getDisplayName());
      }

      @Override
      @NotNull
      public String getFamilyName() {
        return getText();
      }

      @Override
      public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
        return true;
      }

      @Override
      public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        final TemplateDataLanguageConfigurable configurable = new TemplateDataLanguageConfigurable(project);
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
          if (virtualFile != null) {
            configurable.selectFile(virtualFile);
          }
        });
      }

      @Override
      public boolean startInWriteAction() {
        return false;
      }
    };
  }

}
