// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.lang.LangBundle;
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
  public Collection<AbstractTreeNode<?>> getChildren() {
    Module module = getValue();

    final Module[] deps = ModuleRootManager.getInstance(module).getDependencies(true);
    final List<AbstractTreeNode<?>> children = new ArrayList<>();
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
    presentation.setPresentableText(LangBundle.message("presentable.text.module.dependencies"));
    presentation.setIcon(PlatformIcons.CLOSED_MODULE_GROUP_ICON);
  }

  @Override
  public boolean isAlwaysExpand() {
    return true;
  }
}
