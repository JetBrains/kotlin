// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.offline.OfflineProblemDescriptor;
import com.intellij.codeInspection.offlineViewer.OfflineDescriptorResolveResult;
import com.intellij.codeInspection.offlineViewer.OfflineProblemDescriptorNode;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.ui.tree.TreePathUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.TreeTraversal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.function.Supplier;

public class InspectionTreeModel extends BaseTreeModel<InspectionTreeNode> implements InvokerSupplier {
  private static final Logger LOG = Logger.getInstance(InspectionTreeModel.class);
  private final InspectionRootNode myRoot = new InspectionRootNode(this);
  private final Invoker myInvoker;

  public InspectionTreeModel() {
    myInvoker = ApplicationManager.getApplication().isUnitTestMode() ? new Invoker.EDT(this) : new Invoker.Background(this);
  }

  @Override
  public int getIndexOfChild(Object object, Object child) {
    return ((InspectionTreeNode)object).getIndex((TreeNode)child);
  }

  public void reload() {
    treeNodesChanged(null, null, null);
  }

  @Override
  public List<? extends InspectionTreeNode> getChildren(Object parent) {
    List<? extends InspectionTreeNode> children = ((InspectionTreeNode)parent).getChildren();
    children.forEach(InspectionTreeNode::uiRequested);
    return children;
  }

  @Override
  public boolean isLeaf(Object object) {
    if (object == myRoot) return false;
    return super.isLeaf(object);
  }

  @Override
  public int getChildCount(Object object) {
    return ((InspectionTreeNode)object).getChildren().size();
  }

  @Override
  public InspectionRootNode getRoot() {
    return myRoot;
  }

  @Nullable
  public InspectionTreeNode getParent(InspectionTreeNode node) {
    return node.myParent;
  }

  public JBIterable<InspectionTreeNode> traverse(InspectionTreeNode node) {
    return TreeTraversal.PRE_ORDER_DFS.traversal(node, n -> getChildren(n));
  }

  @NotNull
  JBIterable<InspectionTreeNode> traverseFrom(InspectionTreeNode node, boolean direction) {
    return JBIterable.generate(node, n -> getParent(n)).filter(n -> getParent(n) != null).flatMap(n1 -> {
      InspectionTreeNode p = getParent(n1);
      int idx = getIndexOfChild(p, n1);
      if (idx < 0) return JBIterable.empty();
      assert p != null;
      InspectionTreeNode.Children children = p.myChildren;
      if (children == null) return JBIterable.empty();
      InspectionTreeNode[] arr = children.myChildren;
      List<? extends InspectionTreeNode> sublist;
      if (direction) {
        sublist = Arrays.asList(arr).subList(idx + (n1 == node ? 0 : 1), arr.length);
      }
      else {
        sublist = ContainerUtil.reverse(Arrays.asList(arr).subList(0, idx));
      }
      return TreeTraversal.PRE_ORDER_DFS.traversal(sublist, (InspectionTreeNode n) -> direction ? getChildren(n) : ContainerUtil.reverse(getChildren(n)));
    });
  }

  public void remove(@NotNull InspectionTreeNode node) {
    doRemove(node, null);
    treeNodesChanged(null, null, null);
    treeStructureChanged(null, null, null);
  }

  private synchronized void doRemove(@NotNull InspectionTreeNode node, @Nullable InspectionTreeNode skip) {
    for (InspectionTreeNode child : getChildren(node)) {
      doRemove(child, skip);
    }
    if (node != skip) {
      InspectionTreeNode parent = getParent(node);
      if (parent != null) {
        InspectionTreeNode.Children parentChildren = parent.myChildren;
        assert parentChildren != null;
        parentChildren.myChildren = ArrayUtil.remove(parentChildren.myChildren, node);
        parentChildren.myUserObject2Node.removeValue(node);
      }
    }
  }

  synchronized void clearTree() {
    InspectionTreeNode.Children children = myRoot.myChildren;
    if (children != null) {
      children.clear();
    }
  }

  @NotNull
  public InspectionModuleNode createModuleNode(@NotNull Module module, @NotNull InspectionTreeNode parent) {
    return getOrAdd(module, () -> new InspectionModuleNode(module, parent), parent);
  }

  @NotNull
  public InspectionPackageNode createPackageNode(String packageName, @NotNull InspectionTreeNode parent) {
    return getOrAdd(packageName, () -> new InspectionPackageNode(packageName, parent), parent);
  }

  @NotNull
  InspectionGroupNode createGroupNode(String group, @NotNull InspectionTreeNode parent) {
    return getOrAdd(group, () -> new InspectionGroupNode(group, parent), parent);
  }

  @NotNull
  InspectionSeverityGroupNode createSeverityGroupNode(SeverityRegistrar severityRegistrar,
                                                      HighlightDisplayLevel level,
                                                      @NotNull InspectionTreeNode parent) {
    return getOrAdd(level, () -> new InspectionSeverityGroupNode(severityRegistrar, level, parent), parent);
  }

  @NotNull
  public RefElementNode createRefElementNode(@Nullable RefEntity entity,
                                             @NotNull Supplier<? extends RefElementNode> supplier,
                                             @NotNull InspectionTreeNode parent) {
    return getOrAdd(entity, () -> ReadAction.compute(supplier::get), parent);
  }

  public <T extends InspectionTreeNode> T createCustomNode(@NotNull Object userObject, @NotNull Supplier<T> supplier, @NotNull InspectionTreeNode parent) {
    return getOrAdd(userObject, supplier, parent);
  }

  @NotNull
  InspectionNode createInspectionNode(@NotNull InspectionToolWrapper toolWrapper,
                                      InspectionProfileImpl profile,
                                      @NotNull InspectionTreeNode parent) {
    return getOrAdd(toolWrapper.getShortName(), () -> new InspectionNode(toolWrapper, profile, parent), parent);
  }

  public void createProblemDescriptorNode(RefEntity element,
                                          @NotNull CommonProblemDescriptor descriptor,
                                          @NotNull InspectionToolPresentation presentation,
                                          @NotNull InspectionTreeNode parent) {
    getOrAdd(descriptor, () -> ReadAction.compute(() -> new ProblemDescriptionNode(element, descriptor, presentation, parent)), parent);
  }

  public void createOfflineProblemDescriptorNode(@NotNull OfflineProblemDescriptor offlineDescriptor,
                                                 @NotNull OfflineDescriptorResolveResult resolveResult,
                                                 @NotNull InspectionToolPresentation presentation,
                                                 @NotNull InspectionTreeNode parent) {
    getOrAdd(offlineDescriptor,
             () -> ReadAction.compute(() -> new OfflineProblemDescriptorNode(resolveResult, presentation, offlineDescriptor, parent)),
             parent);
  }

  private synchronized <T extends InspectionTreeNode> T getOrAdd(Object userObject, Supplier<? extends T> supplier, InspectionTreeNode parent) {
    LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode() || !ApplicationManager.getApplication().isDispatchThread());
    if (userObject == null) {
      userObject = ObjectUtils.NULL;
    }
    InspectionTreeNode.Children children = parent.myChildren;
    if (children == null) {
      parent.myChildren = children = new InspectionTreeNode.Children();
    }
    InspectionTreeNode node = children.myUserObject2Node.get(userObject);
    if (node == null) {
      node = supplier.get();
      InspectionTreeNode finalNode = node;
      InspectionTreeNode.Children finalChildren = children;
      int idx = ReadAction.compute(() -> Arrays.binarySearch(finalChildren.myChildren, finalNode, InspectionResultsViewComparator.INSTANCE));
      // it's allowed to have idx >= 0 for example for problem descriptor nodes.
      int insertionPoint = idx >= 0 ? idx : -idx - 1;
      children.myChildren = ArrayUtil.insert(children.myChildren, insertionPoint, node);
      children.myUserObject2Node.put(userObject, node);

      LOG.assertTrue(children.myChildren.length == children.myUserObject2Node.size());

      if (node instanceof SuppressableInspectionTreeNode) {
        ((SuppressableInspectionTreeNode)node).nodeAdded();
      }

      TreePath path = TreePathUtil.pathToTreeNode(node);
      TreePath parentPath = path.getParentPath();
      treeNodesInserted(parentPath, null, null);
      while (parentPath != null) {
        treeStructureChanged(parentPath, null, null);
        parentPath = parentPath.getParentPath();
        if (parentPath == null || parentPath.getLastPathComponent() == myRoot) {
          break;
        }
      }
    }
    //noinspection unchecked
    return (T)node;
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return myInvoker;
  }
}
