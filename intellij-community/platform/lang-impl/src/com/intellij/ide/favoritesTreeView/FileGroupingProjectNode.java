// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FileGroupingProjectNode extends ProjectViewNodeWithChildrenList<File> {
  private VirtualFile myVirtualFile;

  public FileGroupingProjectNode(Project project, @NotNull File file, ViewSettings viewSettings) {
    super(project, file, viewSettings);
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    myVirtualFile = lfs.findFileByIoFile(file);
    if (myVirtualFile == null) {
      myVirtualFile = lfs.refreshAndFindFileByIoFile(file);
    }
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return file.equals(myVirtualFile);
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    if (myVirtualFile != null && myVirtualFile.isDirectory()) {
      presentation.setIcon(AllIcons.Nodes.Folder);
    }
    else if (myVirtualFile != null) {
      presentation.setIcon(myVirtualFile.getFileType().getIcon());
    }
    else {
      presentation.setIcon(AllIcons.FileTypes.Unknown);
    }
    presentation.setPresentableText(getValue().getName());
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  public void navigate(boolean requestFocus) {
    if (myVirtualFile != null) {
      PsiNavigationSupport.getInstance().createNavigatable(myProject, myVirtualFile, -1).navigate(requestFocus);
    }
  }

  // todo possibly we need file
  @Override
  public boolean canNavigate() {
    return myVirtualFile != null && myVirtualFile.isValid();
  }

  @Override
  public boolean canNavigateToSource() {
    return myVirtualFile != null && myVirtualFile.isValid();
  }
}
