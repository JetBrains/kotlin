// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.renameProject;

import com.intellij.ide.TitledHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author lene
 */
public final class ProjectFolderRenameHandler extends PsiElementRenameHandler implements TitledHandler {
  @Override
  public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
    return RenameProjectHandler.isAvailable(dataContext) && super.isAvailableOnDataContext(dataContext);
  }

  @Override
  public String getActionTitle() {
    return RefactoringBundle.message("rename.directory.title");
  }
}
