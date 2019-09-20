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

package com.intellij.ide.todo.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.todo.CurrentFileTodosTreeBuilder;
import com.intellij.ide.todo.ToDoSummary;
import com.intellij.ide.todo.TodoFileDirAndModuleComparator;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class SummaryNode extends BaseToDoNode<ToDoSummary> {
  public SummaryNode(Project project, @NotNull ToDoSummary value, TodoTreeBuilder builder) {
    super(project, value, builder);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildren() {
    ArrayList<AbstractTreeNode> children = new ArrayList<>();

    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    if (myToDoSettings.isModulesShown()) {
      for (Iterator i = myBuilder.getAllFiles(); i.hasNext();) {
        final PsiFile psiFile = (PsiFile)i.next();
        if (psiFile == null) { // skip invalid PSI files
          continue;
        }
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        createModuleTodoNodeForFile(children, projectFileIndex, virtualFile);
      }
    }
    else {
      if (myToDoSettings.getIsPackagesShown()) {
        if (myBuilder instanceof CurrentFileTodosTreeBuilder){
          final Iterator allFiles = myBuilder.getAllFiles();
          if(allFiles.hasNext()){
            children.add(new TodoFileNode(myProject, (PsiFile)allFiles.next(), myBuilder, false));
          }
        } else {
          TodoTreeHelper.getInstance(getProject()).addPackagesToChildren(children, null, myBuilder);
        }
      }
      else {
        for (Iterator i = myBuilder.getAllFiles(); i.hasNext();) {
          final PsiFile psiFile = (PsiFile)i.next();
          if (psiFile == null) { // skip invalid PSI files
            continue;
          }
          TodoFileNode fileNode = new TodoFileNode(getProject(), psiFile, myBuilder, false);
          if (getTreeStructure().accept(psiFile) && !children.contains(fileNode)) {
            children.add(fileNode);
          }
        }
      }
    }
    Collections.sort(children, TodoFileDirAndModuleComparator.INSTANCE);
    return children;

  }

  protected void createModuleTodoNodeForFile(ArrayList<? super AbstractTreeNode> children, ProjectFileIndex projectFileIndex, VirtualFile virtualFile) {
    Module module = projectFileIndex.getModuleForFile(virtualFile);
    if (module != null) {
      ModuleToDoNode moduleToDoNode = new ModuleToDoNode(getProject(), module, myBuilder);
      if (!children.contains(moduleToDoNode)) {
        children.add(moduleToDoNode);
      }
    }
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    if (DumbService.getInstance(getProject()).isDumb()) return;
    int todoItemCount = getTodoItemCount(getValue());
    int fileCount = getFileCount(getValue());
    presentation.setPresentableText(IdeBundle.message("node.todo.summary", todoItemCount, fileCount));
  }

  @Override
  public String getTestPresentation() {
    return "Summary";
  }

  @Override
  public int getFileCount(ToDoSummary summary) {
    int count = 0;
    for (Iterator i = myBuilder.getAllFiles(); i.hasNext();) {
      PsiFile psiFile = (PsiFile)i.next();
      if (psiFile == null) { // skip invalid PSI files
        continue;
      }
      if (getTreeStructure().accept(psiFile)) {
        count++;
      }
    }
    return count;
  }

  @Override
  public int getTodoItemCount(final ToDoSummary val) {
    int count = 0;
    for(final Iterator<PsiFile> i=myBuilder.getAllFiles();i.hasNext();){
      count += ReadAction.compute(() -> getTreeStructure().getTodoItemCount(i.next()));
      }
    return count;
  }

  @Override
  public int getWeight() {
    return 0;
  }
}
