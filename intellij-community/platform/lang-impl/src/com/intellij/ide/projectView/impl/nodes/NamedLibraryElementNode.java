// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NamedLibraryElementNode extends ProjectViewNode<NamedLibraryElement> implements NavigatableWithText {
  public NamedLibraryElementNode(Project project, @NotNull NamedLibraryElement value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    List<AbstractTreeNode<?>> children = new ArrayList<>();
    NamedLibraryElement libraryElement = getValue();
    if (libraryElement != null) {
      LibraryGroupNode.addLibraryChildren(libraryElement.getOrderEntry(), children, getProject(), this);
    }
    return children;
  }

  private static Icon getJdkIcon(JdkOrderEntry entry) {
    final Sdk sdk = entry.getJdk();
    if (sdk == null) {
      return AllIcons.Nodes.UnknownJdk;
    }
    final SdkType sdkType = (SdkType) sdk.getSdkType();
    return sdkType.getIcon();
  }

  @Override
  public String getName() {
    NamedLibraryElement library = getValue();
    return library != null ? library.getName() : "";
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    NamedLibraryElement library = getValue();
    if (library == null) return false;

    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      LibraryOrSdkOrderEntry orderEntry = library.getOrderEntry();
      if (orderEntry.isValid()) {
        for (VirtualFile virtualFile : orderEntry.getRootFiles(rootType)) {
          if (VfsUtilCore.isAncestor(virtualFile, file, false)) return true;
        }
      }
    }

    return false;
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    NamedLibraryElement library = getValue();
    if (library == null) return;

    OrderEntry orderEntry = library.getOrderEntry();
    presentation.setPresentableText(library.getName());
    Icon icon = AllIcons.Nodes.PpLibFolder;
    String tooltip = null;
    String location = null;
    if (orderEntry instanceof JdkOrderEntry) {
      JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)orderEntry;
      icon = getJdkIcon(jdkOrderEntry);
      Sdk projectJdk = jdkOrderEntry.getJdk();
      if (projectJdk != null) { //jdk not specified
        String path = projectJdk.getHomePath();
        if (path != null) {
          path = projectJdk.getSdkType().isLocalSdk(projectJdk) ?
                 FileUtil.toSystemDependentName(path) :
                 FileUtil.toSystemIndependentName(path);
          if (getSettings().isShowURL()) {
            location = path;
          }
          else {
            tooltip = path;
          }
        }
      }
    }
    else if (orderEntry instanceof LibraryOrderEntry) {
      LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
      tooltip = StringUtil.capitalize(IdeBundle.message("node.projectview.library", libraryOrderEntry.getLibraryLevel()));
    }
    presentation.setIcon(icon);
    presentation.setTooltip(tooltip);
    presentation.setLocationString(location);
  }

  @Override
  public void navigate(final boolean requestFocus) {
    NamedLibraryElement library = getValue();
    if (library != null) {
      ProjectSettingsService.getInstance(myProject).openLibraryOrSdkSettings(library.getOrderEntry());
    }
  }

  @Override
  public boolean canNavigate() {
    NamedLibraryElement library = getValue();
    return library != null && ProjectSettingsService.getInstance(myProject).canOpenLibraryOrSdkSettings(library.getOrderEntry());
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    return ActionsBundle.message("action.LibrarySettings.navigate");
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getTestPresentation() {
    NamedLibraryElement library = getValue();
    return "Library: " + (library != null ? library.getName() : "(null)");
  }
}
