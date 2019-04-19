// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView;

import com.intellij.ide.UiActivity;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.ide.favoritesTreeView.FavoritesTreeNodeDescriptor;
import com.intellij.ide.util.treeView.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.StatusBarProgress;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.FocusRequestor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class BaseProjectTreeBuilder extends AbstractTreeBuilder {
  protected final Project myProject;

  public BaseProjectTreeBuilder(@NotNull Project project,
                                @NotNull JTree tree,
                                @NotNull DefaultTreeModel treeModel,
                                @NotNull AbstractTreeStructure treeStructure,
                                @Nullable Comparator<NodeDescriptor> comparator) {
    init(tree, treeModel, treeStructure, comparator, DEFAULT_UPDATE_INACTIVE);
    getUi().setClearOnHideDelay(Registry.intValue("ide.tree.clearOnHideTime"));
    myProject = project;
  }

  @NotNull
  @Override
  public Promise<Object> revalidateElement(@NotNull Object element) {
    if (!(element instanceof AbstractTreeNode)) {
      return Promises.rejectedPromise();
    }

    final AsyncPromise<Object> result = new AsyncPromise<>();
    AbstractTreeNode node = (AbstractTreeNode)element;
    final Object value = node.getValue();
    final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(ObjectUtils.tryCast(value, PsiElement.class));
    batch(indicator -> {
      final Ref<Object> target = new Ref<>();
      Promise<Object> callback = _select(element, virtualFile, true, Conditions.alwaysTrue());
      callback
        .onSuccess(it -> result.setResult(target.get()))
        .onError(e -> result.setError(e));
    });
    return result;
  }

  @Override
  protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    return nodeDescriptor instanceof AbstractTreeNode && ((AbstractTreeNode)nodeDescriptor).isAlwaysShowPlus();
  }

  @Override
  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return nodeDescriptor.getParentDescriptor() == null ||
           nodeDescriptor instanceof AbstractTreeNode && ((AbstractTreeNode)nodeDescriptor).isAlwaysExpand();
  }

  @Override
  protected final void expandNodeChildren(@NotNull final DefaultMutableTreeNode node) {
    final NodeDescriptor userObject = (NodeDescriptor)node.getUserObject();
    if (userObject == null) return;
    Object element = userObject.getElement();
    VirtualFile virtualFile = getFileToRefresh(element);
    super.expandNodeChildren(node);
    if (virtualFile != null) {
      virtualFile.refresh(true, false);
    }
  }

  private static VirtualFile getFileToRefresh(Object element) {
    Object object = element;
    if (element instanceof AbstractTreeNode) {
      object = ((AbstractTreeNode)element).getValue();
    }

    return object instanceof PsiDirectory
           ? ((PsiDirectory)object).getVirtualFile()
           : object instanceof PsiFile ? ((PsiFile)object).getVirtualFile() : null;
  }

  @NotNull
  private static List<AbstractTreeNode> collectChildren(@NotNull DefaultMutableTreeNode node) {
    int childCount = node.getChildCount();
    List<AbstractTreeNode> result = new ArrayList<>(childCount);
    for (int i = 0; i < childCount; i++) {
      TreeNode childAt = node.getChildAt(i);
      DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)childAt;
      if (defaultMutableTreeNode.getUserObject() instanceof AbstractTreeNode) {
        AbstractTreeNode treeNode = (AbstractTreeNode)defaultMutableTreeNode.getUserObject();
        result.add(treeNode);
      }
      else if (defaultMutableTreeNode.getUserObject() instanceof FavoritesTreeNodeDescriptor) {
        AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)defaultMutableTreeNode.getUserObject()).getElement();
        result.add(treeNode);
      }
    }
    return result;
  }

  /**
   * @deprecated Use {@link #selectAsync}
   */
  @Deprecated
  @NotNull
  public ActionCallback select(Object element, VirtualFile file, final boolean requestFocus) {
    return Promises.toActionCallback(_select(element, file, requestFocus, Conditions.alwaysTrue()));
  }

  @NotNull
  public Promise<Object> selectAsync(Object element, VirtualFile file, final boolean requestFocus) {
    return _select(element, file, requestFocus, Conditions.alwaysTrue());
  }

  public ActionCallback selectInWidth(final Object element,
                                      final boolean requestFocus,
                                      final Condition<AbstractTreeNode> nonStopCondition) {
    return Promises.toActionCallback(_select(element, null, requestFocus, nonStopCondition));
  }

  @NotNull
  private Promise<Object> _select(final Object element,
                                 final VirtualFile file,
                                 final boolean requestFocus,
                                 final Condition<? super AbstractTreeNode> nonStopCondition) {
    AbstractTreeUpdater updater = getUpdater();
    if (updater == null) {
      return Promises.rejectedPromise();
    }

    final AsyncPromise<Object> result = new AsyncPromise<>();
    UiActivityMonitor.getInstance().addActivity(myProject, new UiActivity.AsyncBgOperation("projectViewSelect"), updater.getModalityState());
    batch(indicator -> {
      _select(element, file, requestFocus, nonStopCondition, result, indicator, null, null, false);
      UiActivityMonitor.getInstance().removeActivity(myProject, new UiActivity.AsyncBgOperation("projectViewSelect"));
    });
    return result;
  }

  private void _select(final Object element,
                       final VirtualFile file,
                       final boolean requestFocus,
                       final Condition<? super AbstractTreeNode> nonStopCondition,
                       final AsyncPromise<Object> result,
                       @NotNull final ProgressIndicator indicator,
                       @Nullable final Ref<Object> virtualSelectTarget,
                       final FocusRequestor focusRequestor,
                       final boolean isSecondAttempt) {
    final AbstractTreeNode alreadySelected = alreadySelectedNode(element);

    final Runnable onDone = () -> {
      JTree tree = getTree();
      if (tree != null && requestFocus && virtualSelectTarget == null && getUi().isReady()) {
        tree.requestFocus();
      }

      result.setResult(null);
    };

    final Condition<AbstractTreeNode> condition = abstractTreeNode -> result.getState() == Promise.State.PENDING && nonStopCondition.value(abstractTreeNode);

    if (alreadySelected == null) {
      expandPathTo(file, (AbstractTreeNode)getTreeStructure().getRootElement(), element, condition, indicator, virtualSelectTarget)
        .onSuccess(node -> {
          if (virtualSelectTarget == null) {
            select(node, onDone);
          }
          else {
            onDone.run();
          }
        })
        .onError(error -> {
          if (isSecondAttempt) {
            result.cancel();
          }
          else {
            _select(file, file, requestFocus, nonStopCondition, result, indicator, virtualSelectTarget, focusRequestor, true);
          }
        });
    }
    else if (virtualSelectTarget == null) {
      scrollTo(alreadySelected, onDone);
    }
    else {
      onDone.run();
    }
  }

  private AbstractTreeNode alreadySelectedNode(final Object element) {
    final TreePath[] selectionPaths = getTree().getSelectionPaths();
    if (selectionPaths == null || selectionPaths.length == 0) {
      return null;
    }
    for (TreePath selectionPath : selectionPaths) {
      Object selected = selectionPath.getLastPathComponent();
      if (selected instanceof DefaultMutableTreeNode && elementIsEqualTo(selected, element)) {
        Object userObject = ((DefaultMutableTreeNode)selected).getUserObject();
        if (userObject instanceof AbstractTreeNode) return (AbstractTreeNode)userObject;
      }
    }
    return null;
  }

  private static boolean elementIsEqualTo(final Object node, final Object element) {
    if (node instanceof DefaultMutableTreeNode) {
      final Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
      if (userObject instanceof ProjectViewNode) {
        final AbstractTreeNode projectViewNode = (ProjectViewNode)userObject;
        return projectViewNode.canRepresent(element);
      }
    }
    return false;
  }

  @SuppressWarnings("WeakerAccess")
  protected boolean canExpandPathTo(@NotNull final AbstractTreeNode root, final Object element) {
    return true;
  }

  @NotNull
  private Promise<AbstractTreeNode> expandPathTo(final VirtualFile file,
                                                 @NotNull final AbstractTreeNode root,
                                                 final Object element,
                                                 @NotNull final Condition<AbstractTreeNode> nonStopCondition,
                                                 @NotNull final ProgressIndicator indicator,
                                                 @Nullable final Ref<Object> target) {
    final AsyncPromise<AbstractTreeNode> async = new AsyncPromise<>();
    if (root.canRepresent(element)) {
      if (target == null) {
        expand(root, () -> async.setResult(root));
      }
      else {
        target.set(root);
        async.setResult(root);
      }
      return async;
    }

    if (!canExpandPathTo(root, element)) {
      async.setError("cannot expand");
      return async;
    }

    if (root instanceof ProjectViewNode && file != null && !((ProjectViewNode)root).contains(file)) {
      async.setError("not applicable");
      return async;
    }

    if (target == null) {
      expand(root, () -> {
        indicator.checkCanceled();

        final DefaultMutableTreeNode rootNode = getNodeForElement(root);
        if (rootNode != null) {
          final List<AbstractTreeNode> kids = collectChildren(rootNode);
          expandChild(kids, 0, nonStopCondition, file, element, async, indicator, target);
        }
        else {
          async.cancel();
        }
      });
    }
    else {
      if (indicator.isCanceled()) {
        async.cancel();
      }
      else {
        final DefaultMutableTreeNode rootNode = getNodeForElement(root);
        final ArrayList<AbstractTreeNode> kids = new ArrayList<>();
        if (rootNode != null && getTree().isExpanded(new TreePath(rootNode.getPath()))) {
          kids.addAll(collectChildren(rootNode));
        }
        else {
          Object[] childElements = getTreeStructure().getChildElements(root);
          for (Object each : childElements) {
            kids.add((AbstractTreeNode)each);
          }
        }

        yield(() -> {
          if (isDisposed()) return;
          expandChild(kids, 0, nonStopCondition, file, element, async, indicator, target);
        });
      }
    }

    return async;
  }

  private void expandChild(@NotNull final List<? extends AbstractTreeNode> kids,
                           int i,
                           @NotNull final Condition<AbstractTreeNode> nonStopCondition,
                           final VirtualFile file,
                           final Object element,
                           @NotNull final AsyncPromise<? super AbstractTreeNode> async,
                           @NotNull final ProgressIndicator indicator,
                           final Ref<Object> virtualSelectTarget) {
    while (i < kids.size()) {
      final AbstractTreeNode eachKid = kids.get(i);
      final boolean[] nodeWasCollapsed = {true};
      final DefaultMutableTreeNode nodeForElement = getNodeForElement(eachKid);
      if (nodeForElement != null) {
        nodeWasCollapsed[0] = getTree().isCollapsed(new TreePath(nodeForElement.getPath()));
      }

      if (nonStopCondition.value(eachKid)) {
        final Promise<AbstractTreeNode> result = expandPathTo(file, eachKid, element, nonStopCondition, indicator, virtualSelectTarget);
        result.onSuccess(abstractTreeNode -> {
          indicator.checkCanceled();
          async.setResult(abstractTreeNode);
        });

        if (result.getState() == Promise.State.PENDING) {
          final int next = i + 1;
          result.onError(error -> {
            indicator.checkCanceled();

            if (nodeWasCollapsed[0] && virtualSelectTarget == null) {
              collapseChildren(eachKid, null);
            }
            expandChild(kids, next, nonStopCondition, file, element, async, indicator, virtualSelectTarget);
          });
          return;
        }
        else {
          if (result.getState() == Promise.State.REJECTED) {
            indicator.checkCanceled();
            if (nodeWasCollapsed[0] && virtualSelectTarget == null) {
              collapseChildren(eachKid, null);
            }
            i++;
          }
          else {
            return;
          }
        }
      }
      else {
        //filter tells us to stop here (for instance, in case of module nodes)
        break;
      }
    }
    async.cancel();
  }

  @Override
  protected boolean validateNode(@NotNull final Object child) {
    if (child == null) {
      return false;
    }
    if (child instanceof ProjectViewNode) {
      final ProjectViewNode projectViewNode = (ProjectViewNode)child;
      return projectViewNode.validate();
    }
    return true;
  }

  @Override
  @NotNull
  protected ProgressIndicator createProgressIndicator() {
    return new StatusBarProgress();
  }
}
