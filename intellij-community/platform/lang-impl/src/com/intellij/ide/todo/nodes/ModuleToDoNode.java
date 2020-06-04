// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.todo.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.todo.TodoTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.TodoItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ModuleToDoNode extends BaseToDoNode<Module> {

  public ModuleToDoNode(Project project, @NotNull Module value, TodoTreeBuilder builder) {
    super(project, value, builder);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
    if (myToDoSettings.getIsPackagesShown()) {
      TodoTreeHelper.getInstance(getProject()).addPackagesToChildren(children, getValue(), myBuilder);
    }
    else {
      for (Iterator i = myBuilder.getAllFiles(); i.hasNext();) {
        final PsiFile psiFile = (PsiFile)i.next();
        if (psiFile == null) { // skip invalid PSI files
          continue;
        }
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        final boolean isInContent = ModuleRootManager.getInstance(getValue()).getFileIndex().isInContent(virtualFile);
        if (!isInContent) continue;
        TodoFileNode fileNode = new TodoFileNode(getProject(), psiFile, myBuilder, false);
        if (getTreeStructure().accept(psiFile) && !children.contains(fileNode)) {
          children.add(fileNode);
        }
      }
    }
    return children;

  }

  @Override
  public boolean contains(Object element) {
    if (element instanceof TodoItem) {
      Module module = ModuleUtilCore.findModuleForFile(((TodoItem)element).getFile());
      return super.canRepresent(module);
    }

    if (element instanceof PsiElement) {
      Module module = ModuleUtilCore.findModuleForPsiElement((PsiElement)element);
      return super.canRepresent(module);
    }
    return super.canRepresent(element);
  }

  private TodoTreeStructure getStructure() {
    return myBuilder.getTodoTreeStructure();
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    String newName = getValue().getName();
    int todoItemCount = getTodoItemCount(getValue());
    presentation.setLocationString(IdeBundle.message("node.todo.group", todoItemCount));
    presentation.setIcon(ModuleType.get(getValue()).getIcon());
    presentation.setPresentableText(newName);
  }

  @Override
  public String getTestPresentation() {
    return "Module";
  }

  @Override
  public int getFileCount(Module module) {
    Iterator<PsiFile> iterator = myBuilder.getFiles(module);
    int count = 0;
    while (iterator.hasNext()) {
      PsiFile psiFile = iterator.next();
      if (getStructure().accept(psiFile)) {
        count++;
      }
    }
    return count;
  }

  @Override
  public int getTodoItemCount(final Module val) {
    Iterator<PsiFile> iterator = myBuilder.getFiles(val);
    int count = 0;
    while (iterator.hasNext()) {
      final PsiFile psiFile = iterator.next();
      count += ReadAction.compute(() -> getTreeStructure().getTodoItemCount(psiFile));
    }
    return count;
  }

  @Override
  public int getWeight() {
    return 1;
  }
}
