// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.file.exclude.EnforcedPlainTextFileTypeFactory;
import com.intellij.openapi.file.exclude.EnforcedPlainTextFileTypeManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.file.exclude.EnforcedPlainTextFileTypeManager.isApplicableFor;

/**
 * @author Rustam Vishnyakov
 */
public class MarkAsPlainTextAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    EnforcedPlainTextFileTypeManager typeManager = EnforcedPlainTextFileTypeManager.getInstance();
    if (project == null || typeManager == null) return;
    JBIterable<VirtualFile> selectedFiles =
      JBIterable.of(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY))
              .filter(file -> isApplicableFor(file) && !typeManager.isMarkedAsPlainText(file));
    typeManager.markAsPlainText(project, VfsUtilCore.toVirtualFileArray(selectedFiles.toList()));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    EnforcedPlainTextFileTypeManager typeManager = EnforcedPlainTextFileTypeManager.getInstance();
    JBIterable<VirtualFile> selectedFiles =
      typeManager == null ? JBIterable.empty() :
      JBIterable.of(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY))
        .filter(file -> isApplicableFor(file) && !typeManager.isMarkedAsPlainText(file));
    boolean enabled = e.getProject() != null && !selectedFiles.isEmpty();
    e.getPresentation().setEnabledAndVisible(enabled);
    e.getPresentation().setIcon(EnforcedPlainTextFileTypeFactory.getEnforcedPlainTextIcon());
  }
    
}
