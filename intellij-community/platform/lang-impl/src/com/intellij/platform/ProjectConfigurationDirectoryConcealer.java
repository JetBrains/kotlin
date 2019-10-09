/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
public class ProjectConfigurationDirectoryConcealer implements TreeStructureProvider, DumbAware {
  private final Project myProject;

  public ProjectConfigurationDirectoryConcealer(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(@NotNull final AbstractTreeNode parent, @NotNull final Collection<AbstractTreeNode> children, final ViewSettings settings) {
    if (parent instanceof PsiDirectoryNode &&
        ProjectViewDirectoryHelper.getInstance(myProject).shouldHideProjectConfigurationFilesDirectory()) {
      final VirtualFile vFile = ((PsiDirectoryNode)parent).getVirtualFile();
      if (vFile != null && Comparing.equal(ProjectFileIndex.SERVICE.getInstance(myProject).getContentRootForFile(vFile), vFile)) {
        final Collection<? extends AbstractTreeNode> moduleChildren = ((PsiDirectoryNode) parent).getChildren();
        Collection<AbstractTreeNode> result = new ArrayList<>();
        for (AbstractTreeNode moduleChild : moduleChildren) {
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
