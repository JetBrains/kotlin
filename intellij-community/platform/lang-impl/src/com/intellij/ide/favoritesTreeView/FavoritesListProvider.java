/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.CommonActionsPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.Set;

public interface FavoritesListProvider extends Comparator<FavoritesTreeNodeDescriptor>, Comparable<FavoritesListProvider> {
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
