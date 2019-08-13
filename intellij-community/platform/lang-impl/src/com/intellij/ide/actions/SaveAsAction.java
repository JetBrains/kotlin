package com.intellij.ide.actions;

import com.intellij.notebook.editor.BackedVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.copy.CopyHandler;
import org.jetbrains.annotations.NotNull;

public class SaveAsAction extends DumbAwareAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    e.getPresentation().setEnabled(project != null && virtualFile != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (virtualFile instanceof BackedVirtualFile) {
      virtualFile = ((BackedVirtualFile)virtualFile).getOriginFile();
    }
    if (project == null || virtualFile == null) return;
    PsiElement element = PsiManager.getInstance(project).findFile(virtualFile);
    if (element == null) return;
    CopyHandler.doCopy(new PsiElement[]{element.getContainingFile()}, null);
  }
}
