// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.editor.actions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class ToggleShowImportPopupsAction extends ToggleAction {
  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    PsiFile file = getFile(e);
    return file != null && DaemonCodeAnalyzer.getInstance(file.getProject()).isImportHintsEnabled(file);
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    PsiFile file = getFile(e);
    if (file != null) {
      DaemonCodeAnalyzer.getInstance(file.getProject()).setImportHintsEnabled(file, state);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean works = getFile(e) != null;
    e.getPresentation().setEnabledAndVisible(works);
    super.update(e);
  }

  @Nullable
  private static PsiFile getFile(@NotNull AnActionEvent e) {
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    return editor == null ? null : e.getData(CommonDataKeys.PSI_FILE);
  }
}
