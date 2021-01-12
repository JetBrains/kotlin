// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeInsight.actions.VcsFacade;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SynchronizeCurrentFileAction extends AnAction implements DumbAware {
  @Override
  public void update(@NotNull AnActionEvent e) {
    List<VirtualFile> files = getFiles(e).take(2).toList();
    Project project = e.getProject();
    if (project == null || files.isEmpty()) {
      e.getPresentation().setEnabledAndVisible(false);
    }
    else {
      e.getPresentation().setEnabledAndVisible(true);
    }
  }

  @NotNull
  private static String getMessage(@NotNull Project project, @NotNull List<? extends VirtualFile> files) {
    VirtualFile single = files.size() == 1 ? files.get(0) : null;
    return single != null ?
           IdeBundle.message("action.synchronize.file", VfsPresentationUtil.getPresentableNameForAction(project, single)) :
           IdeBundle.message("action.synchronize.selected.files");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = getEventProject(e);
    List<VirtualFile> files = getFiles(e).toList();
    if (project == null || files.isEmpty()) return;

    for (VirtualFile file : files) {
      if (file.isDirectory()) file.getChildren();
      if (file instanceof NewVirtualFile) {
        ((NewVirtualFile)file).markClean();
        ((NewVirtualFile)file).markDirtyRecursively();
      }
    }

    RefreshQueue.getInstance().refresh(true, true, () -> postRefresh(project, files), files);
  }

  private static void postRefresh(Project project, List<? extends VirtualFile> files) {
    VcsFacade.getInstance().markFilesDirty(project, files);
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
    if (statusBar != null) {
      statusBar.setInfo(IdeBundle.message("action.sync.completed.successfully", getMessage(project, files)));
    }
  }

  @NotNull
  private static JBIterable<VirtualFile> getFiles(@NotNull AnActionEvent e) {
    return JBIterable.of(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY))
      .filter(o -> o.isInLocalFileSystem());
  }
}