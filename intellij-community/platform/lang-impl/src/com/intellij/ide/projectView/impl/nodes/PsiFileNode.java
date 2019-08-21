// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class PsiFileNode extends BasePsiNode<PsiFile> implements NavigatableWithText {
  public PsiFileNode(Project project, @NotNull PsiFile value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    Project project = getProject();
    VirtualFile jarRoot = getJarRoot();
    if (project != null && jarRoot != null) {
      PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(jarRoot);
      if (psiDirectory != null) {
        return ProjectViewDirectoryHelper.getInstance(project).getDirectoryChildren(psiDirectory, getSettings(), true);
      }
    }

    return ContainerUtil.emptyList();
  }

  private boolean isArchive() {
    VirtualFile file = getVirtualFile();
    return file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    PsiFile value = getValue();
    if (value != null) {
      data.setPresentableText(value.getName());
      data.setIcon(value.getIcon(Iconable.ICON_FLAG_READ_STATUS));

      VirtualFile file = getVirtualFile();
      if (file != null && file.is(VFileProperty.SYMLINK)) {
        String target = file.getCanonicalPath();
        if (target == null) {
          data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
          data.setTooltip(IdeBundle.message("node.project.view.bad.link"));
        }
        else {
          data.setTooltip(FileUtil.toSystemDependentName(target));
        }
      }
    }
  }

  @Override
  public boolean canNavigate() {
    return isNavigatableLibraryRoot() || super.canNavigate();
  }

  private boolean isNavigatableLibraryRoot() {
    VirtualFile jarRoot = getJarRoot();
    final Project project = getProject();
    if (jarRoot != null && project != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
      final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(jarRoot, project);
      return orderEntry != null && ProjectSettingsService.getInstance(project).canOpenLibraryOrSdkSettings(orderEntry);
    }
    return false;
  }

  @Nullable
  private VirtualFile getJarRoot() {
    final VirtualFile file = getVirtualFile();
    if (file == null || !file.isValid() || !(file.getFileType() instanceof ArchiveFileType)) {
      return null;
    }
    return JarFileSystem.getInstance().getJarRootForLocalFile(file);
  }

  @Override
  public void navigate(boolean requestFocus) {
    final VirtualFile jarRoot = getJarRoot();
    final Project project = getProject();
    if (requestFocus && jarRoot != null && project != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
      final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(jarRoot, project);
      if (orderEntry != null) {
        ProjectSettingsService.getInstance(project).openLibraryOrSdkSettings(orderEntry);
        return;
      }
    }

    super.navigate(requestFocus);
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    return isNavigatableLibraryRoot() ? ActionsBundle.message("action.LibrarySettings.navigate") : null;
  }

  @Override
  public int getWeight() {
    return 20;
  }

  @Override
  public String getTitle() {
    VirtualFile file = getVirtualFile();
    return file != null ? FileUtil.getLocationRelativeToUserHome(file.getPresentableUrl()) : super.getTitle();
  }

  @Override
  protected boolean isMarkReadOnly() {
    return true;
  }

  @Override
  public Comparable<ExtensionSortKey> getTypeSortKey() {
    String extension = extension(getValue());
    return extension == null ? null : new ExtensionSortKey(extension);
  }

  @Nullable
  public static String extension(@Nullable PsiFile file) {
    if (file != null) {
      VirtualFile vFile = file.getVirtualFile();
      if (vFile != null) {
        return vFile.getFileType().getDefaultExtension();
      }
    }

    return null;
  }

  public static class ExtensionSortKey implements Comparable<ExtensionSortKey> {
    private final String myExtension;

    public ExtensionSortKey(@NotNull String extension) {
      myExtension = extension;
    }

    @Override
    public int compareTo(ExtensionSortKey o) {
      return o == null ? 0 : myExtension.compareTo(o.myExtension);
    }
  }

  @Override
  public boolean shouldDrillDownOnEmptyElement() {
    final PsiFile file = getValue();
    return file != null && file.getFileType() == StdFileTypes.JAVA;
  }

  @Override
  public boolean canRepresent(final Object element) {
    if (super.canRepresent(element)) return true;

    PsiFile value = getValue();
    return value != null && element != null && element.equals(value.getVirtualFile());
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return super.contains(file) || isArchive() && Comparing.equal(VfsUtil.getLocalFile(file), getVirtualFile());
  }
}