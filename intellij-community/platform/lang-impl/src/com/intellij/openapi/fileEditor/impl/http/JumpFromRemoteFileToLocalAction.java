// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor.impl.http;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.FileAppearanceService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.openapi.vfs.impl.http.RemoteFileState;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

class JumpFromRemoteFileToLocalAction extends AnAction {
  private final HttpVirtualFile myFile;
  private final Project myProject;

  JumpFromRemoteFileToLocalAction(HttpVirtualFile file, Project project) {
    super("Find Local File", "", AllIcons.General.AutoscrollToSource);

    myFile = file;
    myProject = project;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(myFile.getFileInfo().getState() == RemoteFileState.DOWNLOADED);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Collection<VirtualFile> files = findLocalFiles(myProject, Urls.newFromVirtualFile(myFile), myFile.getName());
    if (files.isEmpty()) {
      Messages.showErrorDialog(myProject, "Cannot find local file for '" + myFile.getUrl() + "'", CommonBundle.getErrorTitle());
      return;
    }

    if (files.size() == 1) {
      navigateToFile(myProject, ContainerUtil.getFirstItem(files, null));
    }
    else {
      JBPopupFactory.getInstance()
        .createPopupChooserBuilder(new ArrayList<>(files))
        .setRenderer(new ColoredListCellRenderer<VirtualFile>() {
          @Override
          protected void customizeCellRenderer(@NotNull JList<? extends VirtualFile> list,
                                               VirtualFile value,
                                               int index,
                                               boolean selected,
                                               boolean hasFocus) {
            FileAppearanceService.getInstance().forVirtualFile(value).customize(this);
          }
        })
       .setTitle("Select Target File")
       .setMovable(true)
       .setItemsChosenCallback((selectedValues) -> {
         for (VirtualFile value : selectedValues) {
           navigateToFile(myProject, value);
         }
       }).createPopup().showUnderneathOf(e.getInputEvent().getComponent());
    }
  }

  private static Collection<VirtualFile> findLocalFiles(Project project, Url url, String fileName) {
    for (LocalFileFinder finder : LocalFileFinder.EP_NAME.getExtensions()) {
      final VirtualFile file = finder.findLocalFile(url, project);
      if (file != null) {
        return Collections.singletonList(file);
      }
    }

    return FilenameIndex.getVirtualFilesByName(project, fileName, GlobalSearchScope.allScope(project));
  }

  private static void navigateToFile(Project project, @NotNull VirtualFile file) {
    PsiNavigationSupport.getInstance().createNavigatable(project, file, -1).navigate(true);
  }
}
