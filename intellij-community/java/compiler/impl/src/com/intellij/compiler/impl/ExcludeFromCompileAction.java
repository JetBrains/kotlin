// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.ide.errorTreeView.ErrorTreeElement;
import com.intellij.ide.errorTreeView.ErrorTreeNodeDescriptor;
import com.intellij.ide.errorTreeView.GroupingElement;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.options.ExcludeEntryDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
* @author Eugene Zhuravlev
*/
class ExcludeFromCompileAction extends AnAction {
  private final Project myProject;
  private final NewErrorTreeViewPanel myErrorTreeView;

  ExcludeFromCompileAction(Project project, NewErrorTreeViewPanel errorTreeView) {
    super(CompilerBundle.message("actions.exclude.from.compile.text"));
    myProject = project;
    myErrorTreeView = errorTreeView;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile file = getSelectedFile();

    if (file != null && file.isValid()) {
      ExcludeEntryDescription description = new ExcludeEntryDescription(file, false, true, myProject);
      CompilerConfiguration.getInstance(myProject).getExcludedEntriesConfiguration().addExcludeEntryDescription(description);
      FileStatusManager.getInstance(myProject).fileStatusesChanged();
    }
  }

  @Nullable
  private VirtualFile getSelectedFile() {
    final ErrorTreeNodeDescriptor descriptor = myErrorTreeView.getSelectedNodeDescriptor();
    ErrorTreeElement element = descriptor != null? descriptor.getElement() : null;
    if (element != null && !(element instanceof GroupingElement)) {
      NodeDescriptor parent = descriptor.getParentDescriptor();
      if (parent instanceof ErrorTreeNodeDescriptor) {
        element = ((ErrorTreeNodeDescriptor)parent).getElement();
      }
    }
    return element instanceof GroupingElement? ((GroupingElement)element).getFile() : null;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final boolean isApplicable = getSelectedFile() != null;
    presentation.setEnabledAndVisible(isApplicable);
  }
}
