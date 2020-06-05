// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Hides .idea directory in Project View.
 *
 * @author yole
 */
public final class ProjectConfigurationDirectoryConcealer implements TreeStructureProvider, DumbAware {
  private final Project myProject;

  public ProjectConfigurationDirectoryConcealer(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent, @NotNull Collection<AbstractTreeNode<?>> children, ViewSettings settings) {
    if (parent instanceof PsiDirectoryNode &&
        ProjectViewDirectoryHelper.getInstance(myProject).shouldHideProjectConfigurationFilesDirectory()) {
      final VirtualFile vFile = ((PsiDirectoryNode)parent).getVirtualFile();
      if (vFile != null && Comparing.equal(ProjectFileIndex.SERVICE.getInstance(myProject).getContentRootForFile(vFile), vFile)) {
        final Collection<? extends AbstractTreeNode<?>> moduleChildren = parent.getChildren();
        Collection<AbstractTreeNode<?>> result = new ArrayList<>();
        for (AbstractTreeNode<?> moduleChild : moduleChildren) {
          if (moduleChild instanceof PsiDirectoryNode) {
            PsiDirectory directory = ((PsiDirectoryNode)moduleChild).getValue();
            if (directory != null && directory.getName().equals(Project.DIRECTORY_STORE_FOLDER) && Registry.is("projectView.hide.dot.idea")) {
              continue;
            }
          }
          result.add(moduleChild);
        }
        return result;
      }
    }
    return children;
  }
}
