// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NavigatableWithText;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SyntheticLibraryElementNode extends ProjectViewNode<SyntheticLibrary> implements NavigatableWithText {
  @NotNull private final ItemPresentation myItemPresentation;

  public SyntheticLibraryElementNode(@NotNull Project project, @NotNull SyntheticLibrary library,
                                     @NotNull ItemPresentation itemPresentation, ViewSettings settings) {
    super(project, library, settings);
    myItemPresentation = itemPresentation;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return getLibrary().contains(file);
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode<?>> getChildren() {
    SyntheticLibrary library = getLibrary();
    Project project = Objects.requireNonNull(getProject());
    Set<VirtualFile> excludedRoots = library.getExcludedRoots();
    List<VirtualFile> children = ContainerUtil.filter(library.getAllRoots(), file -> file.isValid() && !excludedRoots.contains(file));
    return ProjectViewDirectoryHelper.getInstance(project).createFileAndDirectoryNodes(children, getSettings());
  }

  @Override
  public String getName() {
    return myItemPresentation.getPresentableText();
  }

  @NotNull
  private SyntheticLibrary getLibrary() {
    return Objects.requireNonNull(getValue());
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.updateFrom(myItemPresentation);
  }

  @Nullable
  @Override
  public String getNavigateActionText(boolean focusEditor) {
    NavigatableWithText navigatable = ObjectUtils.tryCast(getLibrary(), NavigatableWithText.class);
    return navigatable != null ? navigatable.getNavigateActionText(focusEditor) : null;
  }

  @Override
  public boolean canNavigate() {
    Navigatable navigatable = ObjectUtils.tryCast(getLibrary(), Navigatable.class);
    return navigatable != null && navigatable.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    Navigatable navigatable = ObjectUtils.tryCast(getLibrary(), Navigatable.class);
    return navigatable != null && navigatable.canNavigateToSource();
  }

  @Override
  public void navigate(boolean requestFocus) {
    Navigatable navigatable = ObjectUtils.tryCast(getLibrary(), Navigatable.class);
    if (navigatable != null) {
      navigatable.navigate(requestFocus);
    }
  }
}
