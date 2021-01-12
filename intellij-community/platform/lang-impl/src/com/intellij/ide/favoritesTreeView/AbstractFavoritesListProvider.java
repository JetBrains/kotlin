// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
      public Collection<? extends AbstractTreeNode<?>> getChildren() {
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
  public int compare(FavoriteTreeNodeDescriptor o1, FavoriteTreeNodeDescriptor o2) {
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
