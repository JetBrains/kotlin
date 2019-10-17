// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ExpandableItemsHandler;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

@ApiStatus.Internal
public class ServiceViewTreeLinkMouseListener extends RepaintLinkMouseListenerBase<Object> {
  private final Tree myTree;

  public ServiceViewTreeLinkMouseListener(@NotNull Tree tree) {
    myTree = tree;
  }

  @Override
  protected void repaintComponent(MouseEvent e) {
    ExpandableItemsHandler<Integer> handler = myTree.getExpandableItemsHandler();
    if (handler.isEnabled() && !handler.getExpandedItems().isEmpty()) {
      // Dispatch MOUSE_ENTERED in order to repaint ExpandableItemsHandler's tooltip component, since it ignores MOUSE_MOVE.
      myTree.dispatchEvent(new MouseEvent((Component)e.getSource(), MouseEvent.MOUSE_ENTERED, e.getWhen(), e.getModifiers(),
                                          e.getX(), e.getY(), e.getClickCount(), e.isPopupTrigger(), e.getButton()));
    }
    // Repaint all tree since nodes which cursor just leaved should be repaint too.
    myTree.repaint();
  }

  @Nullable
  @Override
  protected Object getTagAt(@NotNull MouseEvent e) {
    final TreePath path = myTree.getPathForLocation(e.getX(), e.getY());
    if (path == null) return null;

    final Rectangle rectangle = myTree.getPathBounds(path);
    if (rectangle == null) return null;

    int dx = e.getX() - rectangle.x;
    final Object treeNode = path.getLastPathComponent();
    final int row = myTree.getRowForLocation(e.getX(), e.getY());
    boolean isLeaf = myTree.getModel().isLeaf(treeNode);

    Component component = myTree.getCellRenderer().getTreeCellRendererComponent(myTree, treeNode, true, false, isLeaf, row, true);
    return component instanceof ColoredTreeCellRenderer ? ((ColoredTreeCellRenderer)component).getFragmentTagAt(dx) : null;
  }

  @Override
  protected boolean isEnabled() {
    return myTree.getRowCount() > 0;
  }
}
