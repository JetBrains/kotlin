// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProjectViewUnloadedModuleNode extends ProjectViewNode<UnloadedModuleDescription> {

  public ProjectViewUnloadedModuleNode(Project project, @NotNull UnloadedModuleDescription value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildren() {
    UnloadedModuleDescription module = getValue();
    if (module == null) return Collections.emptyList();

    final List<VirtualFile> contentRoots = ProjectViewDirectoryHelper.getInstance(myProject).getTopLevelUnloadedModuleRoots(module, getSettings());
    return ProjectViewDirectoryHelper.getInstance(myProject).createFileAndDirectoryNodes(contentRoots, getSettings());
  }

  @Override
  public int getWeight() {
    return 10;
  }

  @Override
  public int getTypeSortWeight(final boolean sortByType) {
    return 2;
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    UnloadedModuleDescription module = getValue();
    if (module == null) {
      setValue(null);
      return;
    }

    presentation.setPresentableText(module.getName());
    presentation.addText(module.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    presentation.setIcon(AllIcons.Modules.UnloadedModule);
    presentation.setTooltip("Unloaded module");
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getRoots() {
    UnloadedModuleDescription module = getValue();
    return module != null ? module.getContentRoots().stream().map(VirtualFilePointer::getFile).filter(Objects::nonNull).collect(Collectors.toList())
                          : Collections.emptyList();
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    UnloadedModuleDescription module = getValue();
    return module != null
           && module.getContentRoots().stream()
                    .map(VirtualFilePointer::getFile)
                    .anyMatch(root -> root != null && VfsUtilCore.isAncestor(root, file, false));
  }
}
