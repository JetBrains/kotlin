// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.todo.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.todo.ToDoSummary;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ToDoRootNode extends BaseToDoNode<Object> {
  private final SummaryNode mySummaryNode;

  public ToDoRootNode(Project project, Object value, TodoTreeBuilder builder, @NotNull ToDoSummary summary) {
    super(project, value, builder);
    mySummaryNode = createSummaryNode(summary);
  }

  protected SummaryNode createSummaryNode(@NotNull ToDoSummary summary) {
    return new SummaryNode(getProject(), summary, myBuilder);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    return new ArrayList<>(Collections.singleton(mySummaryNode));
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
  }

  public Object getSummaryNode() {
    return mySummaryNode;
  }

  @Override
  public String getTestPresentation() {
    return "Root";
  }

  @Override
  public int getFileCount(final Object val) {
    return mySummaryNode.getFileCount(null);
  }

  @Override
  public int getTodoItemCount(final Object val) {
    return mySummaryNode.getTodoItemCount(null);
  }
}
