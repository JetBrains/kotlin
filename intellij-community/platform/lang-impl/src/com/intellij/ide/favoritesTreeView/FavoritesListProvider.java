// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.CommonActionsPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.Set;

public interface FavoritesListProvider extends Comparator<FavoriteTreeNodeDescriptor>, Comparable<FavoritesListProvider> {
  ExtensionPointName<FavoritesListProvider> EP_NAME = new ExtensionPointName<>("com.intellij.favoritesListProvider");

  String getListName(final Project project);

  @Nullable
  String getCustomName(@NotNull CommonActionsPanel.Buttons type);

  boolean willHandle(@NotNull CommonActionsPanel.Buttons type, Project project, @NotNull Set<Object> selectedObjects);

  void handle(@NotNull CommonActionsPanel.Buttons type, Project project, @NotNull Set<Object> selectedObjects, JComponent component);

  int getWeight();

  @Nullable
  FavoritesListNode createFavoriteListNode(Project project);

  void customizeRenderer(ColoredTreeCellRenderer renderer,
                         JTree tree,
                         Object value,
                         boolean selected,
                         boolean expanded,
                         boolean leaf,
                         int row,
                         boolean hasFocus);
}
