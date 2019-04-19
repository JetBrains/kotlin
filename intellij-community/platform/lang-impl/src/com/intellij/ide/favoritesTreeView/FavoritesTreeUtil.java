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

public class FavoritesTreeUtil {
  @NotNull
  public static FavoritesTreeNodeDescriptor[] getSelectedNodeDescriptors(final DnDAwareTree tree) {
    TreePath[] path = tree.getSelectionPaths();
    if (path == null) {
      return FavoritesTreeNodeDescriptor.EMPTY_ARRAY;
    }
    ArrayList<FavoritesTreeNodeDescriptor> result = new ArrayList<>();
    for (TreePath treePath : path) {
      DefaultMutableTreeNode lastPathNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
      Object userObject = lastPathNode.getUserObject();
      if (!(userObject instanceof FavoritesTreeNodeDescriptor)) {
        continue;
      }
      FavoritesTreeNodeDescriptor treeNodeDescriptor = (FavoritesTreeNodeDescriptor)userObject;
      result.add(treeNodeDescriptor);
    }
    return result.toArray(FavoritesTreeNodeDescriptor.EMPTY_ARRAY);
  }

  public static List<AbstractTreeNode> getLogicalPathToSelected(final Tree tree) {
    final List<AbstractTreeNode> result = new ArrayList<>();
    final TreePath selectionPath = tree.getSelectionPath();
    return getLogicalPathTo(result, selectionPath);
  }

  public static List<Integer> getLogicalIndexPathTo(TreePath selectionPath) {
    final List<Integer> result = new ArrayList<>();
    final Object component = selectionPath.getLastPathComponent();
    if (component instanceof DefaultMutableTreeNode) {
      final Object uo = ((DefaultMutableTreeNode)component).getUserObject();
      if (uo instanceof FavoritesTreeNodeDescriptor) {
        AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)uo).getElement();
        while ((!(treeNode instanceof FavoritesListNode)) && treeNode != null) {
//          final int idx = getIndex(treeNode.getParent().getChildren(), treeNode);
//          if (idx == -1) return null;
          result.add(treeNode.getIndex());
          treeNode = treeNode.getParent();
        }
        Collections.reverse(result);
        return result;
      }
    }
    return Collections.emptyList();
  }

  /*private static int getIndex(Collection<AbstractTreeNode> children, AbstractTreeNode node) {
    int idx = 0;
    for (AbstractTreeNode child : children) {
      if (child == node) {
        return idx;
      }
      ++ idx;
    }
    assert false;
    return -1;
  }*/

  public static List<AbstractTreeNode> getLogicalPathTo(List<AbstractTreeNode> result, TreePath selectionPath) {
    final Object component = selectionPath.getLastPathComponent();
    if (component instanceof DefaultMutableTreeNode) {
      final Object uo = ((DefaultMutableTreeNode)component).getUserObject();
      if (uo instanceof FavoritesTreeNodeDescriptor) {
        AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)uo).getElement();
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
  public static FavoritesListNode extractParentList(FavoritesTreeNodeDescriptor descriptor) {
    AbstractTreeNode current = descriptor.getElement();
    while (current != null) {
      if (current instanceof FavoritesListNode) {
        return (FavoritesListNode)current;
      }
      current = current.getParent();
    }
    return null;
  }

  static FavoritesListProvider getProvider(@NotNull FavoritesManager manager, @NotNull FavoritesTreeNodeDescriptor descriptor) {
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
