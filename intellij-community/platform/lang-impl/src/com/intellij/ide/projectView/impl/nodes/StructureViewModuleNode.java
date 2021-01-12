// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class StructureViewModuleNode extends AbstractModuleNode {
  public StructureViewModuleNode(Project project, @NotNull Module value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    final Module module = getValue();
    if (module == null || module.isDisposed()) {
      // just deleted a module from project view
      return Collections.emptyList();

    }
    List<AbstractTreeNode<?>> children = new ArrayList<>(2);
    children.add(new LibraryGroupNode(getProject(), new LibraryGroupElement(module), getSettings()) {
      @Override
      public boolean isAlwaysExpand() {
        return true;
      }
    });

    children.add(new ModuleListNode(getProject(), module, getSettings()));
    return children;
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
  public boolean contains(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  public boolean someChildContainsFile(VirtualFile file) {
    return true;
  }
}
