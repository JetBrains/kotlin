/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class StructureViewModuleNode extends AbstractModuleNode {
  public StructureViewModuleNode(Project project, @NotNull Module value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildren() {
    final Module module = getValue();
    if (module == null || module.isDisposed()) {
      // just deleted a module from project view
      return Collections.emptyList();
    }
    final List<AbstractTreeNode> children = new ArrayList<>(2);
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
