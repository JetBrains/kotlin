// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProblemsViewPanel extends NewErrorTreeViewPanel {
  public ProblemsViewPanel(@NotNull Project project) {
    super(project, null, false, true, null);

    myTree.getEmptyText().setText(JavaCompilerBundle.message("no.compilation.problems.found"));
    setProgress("", 0.0f); // hack: this will pre-initialize progress UI
  }

  @Override
  protected void fillRightToolbarGroup(DefaultActionGroup group) {
    super.fillRightToolbarGroup(group);
    group.addSeparator();
    group.add(new CompilerPropertiesAction());
  }

  @Override
  protected void addExtraPopupMenuActions(DefaultActionGroup group) {
    group.add(new ExcludeFromCompileAction(myProject) {
      @Override
      protected @Nullable VirtualFile getFile() {
        return getSelectedFile();
      }
    });
    // todo: do we need compiler's popup actions here?
    //ActionGroup popupGroup = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP);
    //if (popupGroup != null) {
    //  for (AnAction action : popupGroup.getChildren(null)) {
    //    group.add(action);
    //  }
    //}
  }

  @Override
  protected boolean canHideWarnings() {
    return false;
  }
}