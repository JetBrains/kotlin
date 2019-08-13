/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.CommonActionsPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractFavoritesListProvider<T> implements FavoritesListProvider {
  public static final int BOOKMARKS_WEIGHT = 100;
  public static final int BREAKPOINTS_WEIGHT = 200;
  public static final int TASKS_WEIGHT = 300;
  protected final Project myProject;
  private final String myListName;
  protected final List<AbstractTreeNode<T>> myChildren = new ArrayList<>();
  protected final FavoritesListNode myNode;

  protected AbstractFavoritesListProvider(@NotNull Project project, @NotNull String listName) {
    this(project, listName, null);
  }

  protected AbstractFavoritesListProvider(@NotNull Project project, @NotNull String listName, @Nullable String description) {
    myProject = project;
    myListName = listName;
    myNode = new FavoritesListNode(project, listName, description) {
      @NotNull
      @Override
      public Collection<? extends AbstractTreeNode> getChildren() {
        return myChildren;
      }

      @Override
      public FavoritesListProvider getProvider() {
        return AbstractFavoritesListProvider.this;
      }
    };
  }

  @Override
  public String getListName(Project project) {
    return myListName;
  }

  @Override
  @Nullable
  public FavoritesListNode createFavoriteListNode(Project project) {
    return myNode;
  }

  @Override
  public int compare(FavoritesTreeNodeDescriptor o1, FavoritesTreeNodeDescriptor o2) {
    return o1.getIndex() - o2.getIndex();
  }

  @Nullable
  @Override
  public String getCustomName(@NotNull CommonActionsPanel.Buttons type) {
    return null;
  }

  @Override
  public boolean willHandle(@NotNull CommonActionsPanel.Buttons type, Project project, @NotNull Set<Object> selectedObjects) {
    return false;
  }

  @Override
  public void handle(@NotNull CommonActionsPanel.Buttons type, Project project, @NotNull Set<Object> selectedObjects, JComponent component) {
  }

  @Override
  public int compareTo(FavoritesListProvider o) {
    return Integer.compare(getWeight(), o.getWeight());
  }

  @Override
  public void customizeRenderer(ColoredTreeCellRenderer renderer,
                                JTree tree,
                                @NotNull Object value,
                                boolean selected,
                                boolean expanded,
                                boolean leaf,
                                int row,
                                boolean hasFocus) {
  }
}
