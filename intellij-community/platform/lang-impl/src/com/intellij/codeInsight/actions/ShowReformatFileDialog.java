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
package com.intellij.codeInsight.actions;

import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ShowReformatFileDialog extends AnAction implements DumbAware {
  private static final @NonNls String HELP_ID = "editing.codeReformatting";

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (project == null || editor == null) {
      presentation.setEnabled(false);
      return;
    }

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null || file.getVirtualFile() == null) {
      presentation.setEnabled(false);
      return;
    }

    if (LanguageFormatting.INSTANCE.forContext(file) != null) {
      presentation.setEnabled(true);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (project == null || editor == null) {
      presentation.setEnabled(false);
      return;
    }

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null || file.getVirtualFile() == null) {
      presentation.setEnabled(false);
      return;
    }

    boolean hasSelection = editor.getSelectionModel().hasSelection();
    LayoutCodeDialog dialog = new LayoutCodeDialog(project, file, hasSelection, HELP_ID);
    dialog.show();

    if (dialog.isOK()) {
      new FileInEditorProcessor(file, editor, dialog.getRunOptions()).processCode();
    }
  }
}
