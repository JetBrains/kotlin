// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting.actions;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class HighlightUsagesAction extends AnAction implements DumbAware {
  public HighlightUsagesAction() {
    setInjectedContext(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabled(e.getProject() != null && e.getData(CommonDataKeys.EDITOR) != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    final Project project = e.getProject();
    if (editor == null || project == null) return;

    String commandName = getTemplatePresentation().getText();
    if (commandName == null) commandName = "";

    CommandProcessor.getInstance().executeCommand(
      project,
      () -> {
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        try {
          HighlightUsagesHandler.invoke(project, editor, psiFile);
        }
        catch (IndexNotReadyException ex) {
          DumbService.getInstance(project).showDumbModeNotification(ActionsBundle.message("action.HighlightUsagesInFile.not.ready"));
        }
      },
      commandName,
      null
    );
  }
}
