// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.compiler.options.ExcludeEntryDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 */
public abstract class ExcludeFromCompileAction extends AnAction {
  private final Project myProject;

  public ExcludeFromCompileAction(Project project) {
    super(JavaCompilerBundle.message("actions.exclude.from.compile.text"));
    myProject = project;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile file = getFile();
    if (file != null && file.isValid()) {
      ExcludeEntryDescription description = new ExcludeEntryDescription(file, false, true, myProject);
      CompilerConfiguration.getInstance(myProject).getExcludedEntriesConfiguration().addExcludeEntryDescription(description);
      FileStatusManager.getInstance(myProject).fileStatusesChanged();
    }
  }

  @Nullable
  protected abstract VirtualFile getFile();

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final boolean isApplicable = getFile() != null;
    presentation.setEnabledAndVisible(isApplicable);
  }
}
