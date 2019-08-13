// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 */
public class RenameFileAction extends AnAction {
  public static final String RENAME_FILE = "Rename File...";

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    assert file != null;
    final VirtualFile virtualFile = file.getVirtualFile();
    assert virtualFile != null;
    final Project project = e.getData(CommonDataKeys.PROJECT);
    assert project != null;
    PsiElementRenameHandler.invoke(file, project, file, null);
  }

  @Override
  public boolean startInTransaction() {
    return true;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    Presentation presentation = e.getPresentation();
    String place = e.getPlace();
    boolean enabled = file != null &&
                      (enabledInProjectView(file) || !ActionPlaces.PROJECT_VIEW_POPUP.equals(place)) &&
                      place != ActionPlaces.EDITOR_POPUP && e.getData(CommonDataKeys.PROJECT) != null;
    presentation.setEnabledAndVisible(enabled);
    if (enabled) {
      presentation.setText(RENAME_FILE);
      presentation.setDescription("Rename selected file");
    }
  }

  protected boolean enabledInProjectView(@NotNull PsiFile file) {
    for (RenameFileActionProvider provider : RenameFileActionProvider.EP_NAME.getExtensionList()) {
      if (provider.enabledInProjectView(file)) return true;
    }

    return false;
  }
}
