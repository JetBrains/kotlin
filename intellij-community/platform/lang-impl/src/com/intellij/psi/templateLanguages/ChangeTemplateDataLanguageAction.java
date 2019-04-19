// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.templateLanguages;

import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class ChangeTemplateDataLanguageAction extends AnAction {
  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setVisible(false);

    VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (files != null && files.length > 1) {
      virtualFile = null;
    }
    if (virtualFile == null || virtualFile.isDirectory()) return;

    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    final FileViewProvider provider = PsiManager.getInstance(project).findViewProvider(virtualFile);
    if (provider instanceof ConfigurableTemplateLanguageFileViewProvider) {
      final TemplateLanguageFileViewProvider viewProvider = (TemplateLanguageFileViewProvider)provider;

      e.getPresentation().setText(LangBundle.message("quickfix.change.template.data.language.text", viewProvider.getTemplateDataLanguage().getDisplayName()));
      e.getPresentation().setEnabledAndVisible(true);
    }

  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    editSettings(project, e.getData(CommonDataKeys.VIRTUAL_FILE));
  }

  public static void editSettings(@NotNull Project project, @Nullable final VirtualFile virtualFile) {
    final TemplateDataLanguageConfigurable configurable = new TemplateDataLanguageConfigurable(project);
    ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
      if (virtualFile != null) {
        configurable.selectFile(virtualFile);
      }
    });
  }
}
