/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;

/**
 * @author cdr
 */
public class DuplicateNodeRenderer {
  public interface DuplicatableNode<T> {
    //returns first duplicate node, if any, or null if there are none
    //duplicate nodes are painted gray
    @Nullable T getDuplicate();

  }

  public static void paintDuplicateNodesBackground(Graphics g, JTree tree) {
    Rectangle clipBounds = g.getClipBounds();
    int start = tree.getClosestRowForLocation(clipBounds.x, clipBounds.y);
    int end = Math.min(tree.getRowCount(), tree.getClosestRowForLocation(clipBounds.x+clipBounds.width, clipBounds.y+clipBounds.height)+1);
    Color old = g.getColor();
    for (int i = start; i < end; i++) {
      TreePath path = tree.getPathForRow(i);
      if (path == null) continue;
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Rectangle accumRect = null;
      TreePath accumPath = null;
      while (node != null) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof DuplicatableNode)) break;
        DuplicatableNode duplicatableNode = (DuplicatableNode)userObject;
        Object duplicate = duplicatableNode.getDuplicate();
        if (duplicate == null) break;
        accumPath = accumRect == null ? path : accumPath.getParentPath();
        accumRect = union(tree.getPathBounds(accumPath), accumRect);
        node = (DefaultMutableTreeNode)node.getParent();
      }
      if (accumRect != null) {
        Rectangle rowRect = tree.getRowBounds(tree.getRowForPath(accumPath));
        accumRect = accumRect.intersection(new Rectangle(rowRect.x, rowRect.y, Integer.MAX_VALUE, Integer.MAX_VALUE));

        //unite all expanded children node rectangles since they can stretch out of parent's
        node = (DefaultMutableTreeNode)accumPath.getLastPathComponent();
        accumRect = union(accumRect, getExpandedNodesRect(tree, node, accumPath));

        g.setColor(Gray._230);
        g.fillRoundRect(accumRect.x, accumRect.y, accumRect.width, accumRect.height, 10, 10);
        g.setColor(Color.lightGray);
        g.drawRoundRect(accumRect.x, accumRect.y, accumRect.width, accumRect.height, 10, 10);
      }
    }
    g.setColor(old);
  }

  @NotNull
  private static Rectangle union(Rectangle r1, Rectangle r2) {
    if (r1 == null) return r2;
    if (r2 == null) return r1;
    return r1.union(r2);
  }

  private static Rectangle getExpandedNodesRect(JTree tree, DefaultMutableTreeNode node, TreePath path) {
    Rectangle rect = tree.getRowBounds(tree.getRowForPath(path));
    if (tree.isExpanded(path)) {
      Enumeration<TreeNode> children = node.children();
      while (children.hasMoreElements()) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
        TreePath childPath = path.pathByAddingChild(child);
        assert !path.equals(childPath) : path+";"+child;
        rect = union(rect, getExpandedNodesRect(tree, child, childPath));
      }
    }
    return rect;
  }

}
