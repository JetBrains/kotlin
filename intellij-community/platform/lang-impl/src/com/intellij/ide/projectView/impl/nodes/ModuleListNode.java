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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModuleListNode extends ProjectViewNode<Module> {

  public ModuleListNode(Project project, @NotNull Module value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildren() {
    Module module = getValue();

    final Module[] deps = ModuleRootManager.getInstance(module).getDependencies(true);
    final List<AbstractTreeNode> children = new ArrayList<>();
    for (Module dependency : deps) {
      children.add(new ProjectViewModuleNode(myProject, dependency, getSettings()) {
        @Override
        protected boolean showModuleNameInBold() {
          return false;
        }
      });
    }

    return children;
  }


  @Override
  public String getTestPresentation() {
    return "Modules";
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return someChildContainsFile(file);
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText("Module Dependencies");
    presentation.setIcon(PlatformIcons.CLOSED_MODULE_GROUP_ICON);
  }

  @Override
  public boolean isAlwaysExpand() {
    return true;
  }
}
