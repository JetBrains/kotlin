/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl.ui;

import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.FrameworkDetectionContext;
import com.intellij.framework.detection.impl.FrameworkDetectionUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.util.Consumer;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * @author nik
 */
public class DetectedFrameworksTree extends CheckboxTree {
  private List<? extends DetectedFrameworkDescription> myDetectedFrameworks;
  private final FrameworkDetectionContext myContext;
  private DetectedFrameworksComponent.GroupByOption myGroupByOption;

  public DetectedFrameworksTree(final FrameworkDetectionContext context, DetectedFrameworksComponent.GroupByOption groupByOption) {
    super(new DetectedFrameworksTreeRenderer(), new CheckedTreeNode(null), new CheckPolicy(true, true, true, false));
    myContext = context;
    myGroupByOption = groupByOption;
    setShowsRootHandles(false);
    setRootVisible(false);
  }

  private void createNodesGroupedByDirectory(CheckedTreeNode root, final List<? extends DetectedFrameworkDescription> frameworks) {
    Map<VirtualFile, FrameworkDirectoryNode> nodes = new HashMap<>();
    List<DetectedFrameworkNode> externalNodes = new ArrayList<>();
    for (DetectedFrameworkDescription framework : frameworks) {
      VirtualFile parent = VfsUtil.getCommonAncestor(framework.getRelatedFiles());
      if (parent != null && !parent.isDirectory()) {
        parent = parent.getParent();
      }

      final DetectedFrameworkNode frameworkNode = new DetectedFrameworkNode(framework, myContext);
      if (parent != null) {
        createDirectoryNodes(parent, nodes).add(frameworkNode);
      }
      else {
        externalNodes.add(frameworkNode);
      }
    }
    List<FrameworkDirectoryNode> rootDirs = new ArrayList<>();
    for (FrameworkDirectoryNode directoryNode : nodes.values()) {
      if (directoryNode.getParent() == null) {
        rootDirs.add(directoryNode);
      }
    }
    for (FrameworkDirectoryNode dir : rootDirs) {
      root.add(collapseDirectoryNode(dir));
    }
    for (DetectedFrameworkNode node : externalNodes) {
      root.add(node);
    }
  }

  public void processUncheckedNodes(@NotNull final Consumer<? super DetectedFrameworkTreeNodeBase> consumer) {
    TreeUtil.traverse(getRoot(), node -> {
      if (node instanceof DetectedFrameworkTreeNodeBase) {
        final DetectedFrameworkTreeNodeBase frameworkNode = (DetectedFrameworkTreeNodeBase)node;
        if (!frameworkNode.isChecked()) {
          consumer.consume(frameworkNode);
        }
      }
      return true;
    });
  }

  @Override
  protected void onNodeStateChanged(CheckedTreeNode node) {
    final List<DetectedFrameworkDescription> checked = Arrays.asList(getCheckedNodes(DetectedFrameworkDescription.class, null));
    final List<DetectedFrameworkDescription> disabled = FrameworkDetectionUtil.getDisabledDescriptions(checked, Collections.emptyList());
    for (DetectedFrameworkDescription description : disabled) {
      final DefaultMutableTreeNode treeNode = TreeUtil.findNodeWithObject(getRoot(), description);
      if (treeNode instanceof CheckedTreeNode) {
        ((CheckedTreeNode)treeNode).setChecked(false);
      }
    }
  }

  private static FrameworkDirectoryNode collapseDirectoryNode(FrameworkDirectoryNode node) {
    if (node.getChildCount() == 1) {
      final TreeNode child = node.getChildAt(0);
      if (child instanceof FrameworkDirectoryNode) {
        return collapseDirectoryNode((FrameworkDirectoryNode)child);
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      TreeNode child = node.getChildAt(i);
      if (child instanceof FrameworkDirectoryNode) {
        final FrameworkDirectoryNode collapsed = collapseDirectoryNode((FrameworkDirectoryNode)child);
        if (collapsed != child) {
          node.remove(i);
          node.insert(collapsed, i);
        }
      }
    }
    return node;
  }

  @NotNull
  private static FrameworkDirectoryNode createDirectoryNodes(@NotNull VirtualFile dir, @NotNull Map<VirtualFile, FrameworkDirectoryNode> nodes) {
    final FrameworkDirectoryNode node = nodes.get(dir);
    if (node != null) {
      return node;
    }

    final FrameworkDirectoryNode newNode = new FrameworkDirectoryNode(dir);
    nodes.put(dir, newNode);
    final VirtualFile parent = dir.getParent();
    if (parent != null) {
      createDirectoryNodes(parent, nodes).add(newNode);
    }
    return newNode;
  }

  private void createNodesGroupedByType(CheckedTreeNode root, final List<? extends DetectedFrameworkDescription> frameworks) {
    Map<FrameworkType, FrameworkTypeNode> groupNodes = new HashMap<>();
    for (DetectedFrameworkDescription framework : frameworks) {
      final FrameworkType type = framework.getDetector().getFrameworkType();
      FrameworkTypeNode group = groupNodes.get(type);
      if (group == null) {
        group = new FrameworkTypeNode(type);
        groupNodes.put(type, group);
        root.add(group);
      }
      group.add(new DetectedFrameworkNode(framework, myContext));
    }
  }

  private CheckedTreeNode getRoot() {
    return ((CheckedTreeNode)getModel().getRoot());
  }

  public void changeGroupBy(DetectedFrameworksComponent.GroupByOption option) {
    if (myGroupByOption.equals(option)) return;
    myGroupByOption = option;
    if (myDetectedFrameworks != null) {
      rebuildTree(myDetectedFrameworks);
    }
  }

  public void rebuildTree(final List<? extends DetectedFrameworkDescription> frameworks) {
    final CheckedTreeNode root = getRoot();
    root.removeAllChildren();
    if (myGroupByOption == DetectedFrameworksComponent.GroupByOption.TYPE) {
      createNodesGroupedByType(root, frameworks);
    }
    else {
      createNodesGroupedByDirectory(root, frameworks);
    }
    ((DefaultTreeModel)getModel()).nodeStructureChanged(root);
    TreeUtil.expandAll(this);
    myDetectedFrameworks = frameworks;
  }

  private static class DetectedFrameworksTreeRenderer extends CheckboxTreeCellRenderer {
    private DetectedFrameworksTreeRenderer() {
      super(true, false);
    }

    @Override
    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      if (value instanceof DetectedFrameworkTreeNodeBase) {
        ((DetectedFrameworkTreeNodeBase)value).renderNode(getTextRenderer());
      }
    }
  }
}
