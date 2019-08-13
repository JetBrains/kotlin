/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.ide.todo.nodes;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.todo.TodoFileDirAndModuleComparator;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TodoTreeHelper {
  private final Project myProject;
  
  public static TodoTreeHelper getInstance(Project project) {
    return ServiceManager.getService(project, TodoTreeHelper.class);
  }

  public TodoTreeHelper(final Project project) {
    myProject = project;
  }

  public void addPackagesToChildren(ArrayList<? super AbstractTreeNode> children,
                                    Module module,
                                    TodoTreeBuilder builder) {
    addDirsToChildren(collectContentRoots(module), children, builder);
  }

  protected List<VirtualFile> collectContentRoots(Module module) {
    final List<VirtualFile> roots = new ArrayList<>();
    if (module == null) {
      ContainerUtil.addAll(roots, ProjectRootManager.getInstance(myProject).getContentRoots());
    } else {
      ContainerUtil.addAll(roots, ModuleRootManager.getInstance(module).getContentRoots());
    }
    return roots;
  }

  protected void addDirsToChildren(List<? extends VirtualFile> roots,
                                   ArrayList<? super AbstractTreeNode> children,
                                   TodoTreeBuilder builder) {
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    for (VirtualFile dir : roots) {
      final PsiDirectory directory = psiManager.findDirectory(dir);
      if (directory == null) {
        continue;
      }
      final Iterator<PsiFile> files = builder.getFiles(directory);
      if (!files.hasNext()) continue;
      TodoDirNode dirNode = new TodoDirNode(myProject, directory, builder);
      if (!children.contains(dirNode)){
        children.add(dirNode);
      }
    }
  }

  public Collection<AbstractTreeNode> getDirectoryChildren(PsiDirectory psiDirectory, TodoTreeBuilder builder, boolean isFlatten) {
    ArrayList<AbstractTreeNode> children = new ArrayList<>();
    if (!isFlatten || !skipDirectory(psiDirectory)) {
      final Iterator<PsiFile> iterator = builder.getFiles(psiDirectory);
      while (iterator.hasNext()) {
        final PsiFile psiFile = iterator.next();
        // Add files
        final PsiDirectory containingDirectory = psiFile.getContainingDirectory();
        TodoFileNode todoFileNode = new TodoFileNode(getProject(), psiFile, builder, false);
        if (psiDirectory.equals(containingDirectory) && !children.contains(todoFileNode)) {
          children.add(todoFileNode);
          continue;
        }
        // Add directories (find first ancestor directory that is in our psiDirectory)
        PsiDirectory _dir = psiFile.getContainingDirectory();
        while (_dir != null) {
          if (skipDirectory(_dir)){
            break;
          }
          final PsiDirectory parentDirectory = _dir.getParentDirectory();
          TodoDirNode todoDirNode = new TodoDirNode(getProject(), _dir, builder);
          if (parentDirectory != null && psiDirectory.equals(parentDirectory) && !children.contains(todoDirNode)) {
            children.add(todoDirNode);
            break;
          }
          _dir = parentDirectory;
        }
      }
    }
    else { // flatten packages
      final PsiDirectory parentDirectory = psiDirectory.getParentDirectory();
      if (
        parentDirectory == null ||
        !skipDirectory(parentDirectory) ||
        !ProjectRootManager.getInstance(getProject()).getFileIndex().isInContent(parentDirectory.getVirtualFile())
      ) {
        final Iterator<PsiFile> iterator = builder.getFiles(psiDirectory);
        while (iterator.hasNext()) {
          final PsiFile psiFile = iterator.next();
          // Add files
          TodoFileNode todoFileNode = new TodoFileNode(getProject(), psiFile, builder, false);
          if (psiDirectory.equals(psiFile.getContainingDirectory()) && !children.contains(todoFileNode)) {
            children.add(todoFileNode);
            continue;
          }
          // Add directories
          final PsiDirectory _dir = psiFile.getContainingDirectory();
          if (_dir == null || skipDirectory(_dir)){
            continue;
          }
          TodoDirNode todoDirNode = new TodoDirNode(getProject(), _dir, builder);
          if (PsiTreeUtil.isAncestor(psiDirectory, _dir, true) && !children.contains(todoDirNode) && !builder.isDirectoryEmpty(_dir)) {
            children.add(todoDirNode);
          }
        }
      }
      else {
        final Iterator<PsiFile> iterator = builder.getFiles(psiDirectory);
        while (iterator.hasNext()) {
          final PsiFile psiFile = iterator.next();
          final PsiDirectory containingDirectory = psiFile.getContainingDirectory();
          TodoFileNode todoFileNode = new TodoFileNode(getProject(), psiFile, builder, false);
          if (psiDirectory.equals(containingDirectory) && !children.contains(todoFileNode)) {
            children.add(todoFileNode);
          }
        }
      }
    }
   Collections.sort(children, TodoFileDirAndModuleComparator.INSTANCE);
   return children;
  }

  public boolean skipDirectory(PsiDirectory directory) {
    return false;
  }

  @Nullable
  public PsiElement getSelectedElement(Object userObject) {
    if (userObject instanceof TodoDirNode) {
      TodoDirNode descriptor = (TodoDirNode)userObject;
      return descriptor.getValue();
    }

    else if (userObject instanceof TodoFileNode) {
      TodoFileNode descriptor = (TodoFileNode)userObject;
      return descriptor.getValue();
    }
    return null;
  }
  
  public boolean contains(ProjectViewNode node, Object element) {
    return false;
  }


  public Project getProject() {
    return myProject;
  }
}
