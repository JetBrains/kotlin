// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.todo.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.TodoItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SingleFileToDoNode extends BaseToDoNode<PsiFile>{
  private final TodoFileNode myFileNode;

  public SingleFileToDoNode(Project project, @NotNull PsiFile value, TodoTreeBuilder builder) {
    super(project, value, builder);
    myFileNode = new TodoFileNode(getProject(), value, myBuilder, true);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    return new ArrayList<>(Collections.singleton(myFileNode));
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
  }

  @Override
  public boolean canRepresent(Object element) {
    return false;
  }

  @Override
  public boolean contains(Object element) {
    if (element instanceof TodoItem) {
      return super.canRepresent(((TodoItem)element).getFile());
    }
    return super.canRepresent(element);
  }

  public Object getFileNode() {
    return myFileNode;
  }

  @Override
  public int getFileCount(final PsiFile val) {
    return 1;
  }

  @Override
  public int getTodoItemCount(final PsiFile val) {
    return getTreeStructure().getTodoItemCount(val);
  }
}
