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
package com.intellij.execution.dashboard.tree;

import com.intellij.execution.dashboard.RunDashboardGroup;
import com.intellij.execution.dashboard.RunDashboardNode;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author konstantin.aleev
 */
public class GroupingNode extends AbstractTreeNode<Pair<Object, RunDashboardGroup>> implements RunDashboardNode {
  private final List<AbstractTreeNode> myChildren = new ArrayList<>();

  public GroupingNode(Project project, Object parent, RunDashboardGroup group) {
    super(project, Pair.create(parent, group));
  }

  public RunDashboardGroup getGroup() {
    //noinspection ConstantConditions ???
    return getValue().getSecond();
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return myChildren;
  }

  public void addChildren(Collection<? extends AbstractTreeNode> children) {
    myChildren.addAll(children);
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(getGroup().getName());
    presentation.setIcon(getGroup().getIcon());
  }
}
