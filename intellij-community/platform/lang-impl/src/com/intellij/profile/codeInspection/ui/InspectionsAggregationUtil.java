// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class InspectionsAggregationUtil {
  public static List<HighlightDisplayKey> getInspectionsKeys(final InspectionConfigTreeNode node) {
    return ContainerUtil.map(getInspectionsNodes(node), node1 -> node1.getKey());
  }

  public static List<InspectionConfigTreeNode.Tool> getInspectionsNodes(final InspectionConfigTreeNode node) {
    final Queue<InspectionConfigTreeNode> q = new Queue<>(1);
    q.addLast(node);
    return getInspectionsNodes(q);
  }

  public static List<InspectionConfigTreeNode.Tool> getInspectionsNodes(final TreePath[] paths) {
    if (paths == null) return Collections.emptyList();
    final Queue<InspectionConfigTreeNode> q = new Queue<>(paths.length);
    for (final TreePath path : paths) {
      if (path != null) {
        q.addLast((InspectionConfigTreeNode)path.getLastPathComponent());
      }
    }
    return getInspectionsNodes(q);
  }

  private static List<InspectionConfigTreeNode.Tool> getInspectionsNodes(final Queue<InspectionConfigTreeNode> queue) {
    final List<InspectionConfigTreeNode.Tool> nodes = new ArrayList<>();
    while (!queue.isEmpty()) {
      final InspectionConfigTreeNode node = queue.pullFirst();
      if (node instanceof InspectionConfigTreeNode.Group) {
        for (int i = 0; i < node.getChildCount(); i++) {
          final InspectionConfigTreeNode childNode = (InspectionConfigTreeNode) node.getChildAt(i);
          queue.addLast(childNode);
        }
      } else {
        nodes.add((InspectionConfigTreeNode.Tool)node);
      }
    }
    return new ArrayList<>(nodes);
  }
}
