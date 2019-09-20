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
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ProjectViewNodeWithChildrenList<T> extends ProjectViewNode<T> {
  protected final List<AbstractTreeNode> myChildren;

  protected ProjectViewNodeWithChildrenList(Project project, @NotNull T t, ViewSettings viewSettings) {
    super(project, t, viewSettings);
    myChildren = new ArrayList<>();
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return myChildren;
  }

  public void addChild(final AbstractTreeNode node) {
    myChildren.add(node);
    node.setParent(this);
  }
}
