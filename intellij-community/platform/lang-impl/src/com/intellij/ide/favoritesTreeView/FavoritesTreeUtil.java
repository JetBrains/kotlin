// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FavoritesTreeUtil {
  public static FavoriteTreeNodeDescriptor @NotNull [] getSelectedNodeDescriptors(final DnDAwareTree tree) {
    TreePath[] path = tree.getSelectionPaths();
    if (path == null) {
      return FavoriteTreeNodeDescriptor.EMPTY_ARRAY;
    }
    ArrayList<FavoriteTreeNodeDescriptor> result = new ArrayList<>();
    for (TreePath treePath : path) {
      DefaultMutableTreeNode lastPathNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
      Object userObject = lastPathNode.getUserObject();
      if (!(userObject instanceof FavoriteTreeNodeDescriptor)) {
        continue;
      }
      FavoriteTreeNodeDescriptor treeNodeDescriptor = (FavoriteTreeNodeDescriptor)userObject;
      result.add(treeNodeDescriptor);
    }
    return result.toArray(FavoriteTreeNodeDescriptor.EMPTY_ARRAY);
  }

  public static List<AbstractTreeNode<?>> getLogicalPathToSelected(final Tree tree) {
    final List<AbstractTreeNode<?>> result = new ArrayList<>();
    final TreePath selectionPath = tree.getSelectionPath();
    return getLogicalPathTo(result, selectionPath);
  }

  public static List<AbstractTreeNode<?>> getLogicalPathTo(List<AbstractTreeNode<?>> result, TreePath selectionPath) {
    final Object component = selectionPath.getLastPathComponent();
    if (component instanceof DefaultMutableTreeNode) {
      final Object uo = ((DefaultMutableTreeNode)component).getUserObject();
      if (uo instanceof FavoriteTreeNodeDescriptor) {
        AbstractTreeNode treeNode = ((FavoriteTreeNodeDescriptor)uo).getElement();
        while ((!(treeNode instanceof FavoritesListNode)) && treeNode != null) {
          result.add(treeNode);
          treeNode = treeNode.getParent();
        }
        Collections.reverse(result);
        return result;
      }
    }
    return Collections.emptyList();
  }

  @Nullable
  public static FavoritesListNode extractParentList(FavoriteTreeNodeDescriptor descriptor) {
    AbstractTreeNode current = descriptor.getElement();
    while (current != null) {
      if (current instanceof FavoritesListNode) {
        return (FavoritesListNode)current;
      }
      current = current.getParent();
    }
    return null;
  }

  static FavoritesListProvider getProvider(@NotNull FavoritesManager manager, @NotNull FavoriteTreeNodeDescriptor descriptor) {
    AbstractTreeNode treeNode = descriptor.getElement();
    while (treeNode != null && (!(treeNode instanceof FavoritesListNode))) {
      treeNode = treeNode.getParent();
    }
    if (treeNode != null) {
      final String name = ((FavoritesListNode)treeNode).getValue();
      return manager.getListProvider(name);
    }
    return null;
  }
}
