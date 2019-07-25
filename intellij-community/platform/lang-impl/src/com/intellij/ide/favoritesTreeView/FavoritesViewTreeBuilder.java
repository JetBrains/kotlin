// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ProjectTopics;
import com.intellij.ide.CopyPasteUtil;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewPsiTreeChangeListener;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesViewTreeBuilder extends BaseProjectTreeBuilder {

  public FavoritesViewTreeBuilder(@NotNull Project project,
                                  JTree tree,
                                  DefaultTreeModel treeModel,
                                  ProjectAbstractTreeStructureBase treeStructure) {
    super(project,
          tree,
          treeModel,
          treeStructure,
          new FavoritesComparator(ProjectView.getInstance(project), FavoritesProjectViewPane.ID));
    final MessageBusConnection bus = myProject.getMessageBus().connect(this);
    ProjectViewPsiTreeChangeListener psiTreeChangeListener = new ProjectViewPsiTreeChangeListener(myProject) {
      @Override
      protected DefaultMutableTreeNode getRootNode() {
        return FavoritesViewTreeBuilder.this.getRootNode();
      }

      @Override
      protected AbstractTreeUpdater getUpdater() {
        return FavoritesViewTreeBuilder.this.getUpdater();
      }

      @Override
      protected boolean isFlattenPackages() {
        return getStructure().isFlattenPackages();
      }

      @Override
      protected void childrenChanged(PsiElement parent, final boolean stopProcessingForThisModificationCount) {
        PsiElement containingFile = parent instanceof PsiDirectory ? parent : parent.getContainingFile();
        if (containingFile != null && findNodeByElement(containingFile) == null) {
          queueUpdate(true);
        }
        else {
          super.childrenChanged(parent, true);
        }
      }
    };
    bus.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        queueUpdate(true);
      }
    });
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(psiTreeChangeListener, this);
    FileStatusListener fileStatusListener = new MyFileStatusListener();
    FileStatusManager.getInstance(myProject).addFileStatusListener(fileStatusListener, this);
    CopyPasteUtil.addDefaultListener(this, this::addSubtreeToUpdateByElement);

    FavoritesListener favoritesListener = new FavoritesListener() {
      @Override
      public void rootsChanged() {
        updateFromRoot();
      }

      @Override
      public void listAdded(@NotNull String listName) {
        updateFromRoot();
      }

      @Override
      public void listRemoved(@NotNull String listName) {
        updateFromRoot();
      }
    };
    initRootNode();
    FavoritesManager.getInstance(myProject).addFavoritesListener(favoritesListener, this);
  }

  @NotNull
  public FavoritesTreeStructure getStructure() {
    final AbstractTreeStructure structure = getTreeStructure();
    assert structure instanceof FavoritesTreeStructure;
    return (FavoritesTreeStructure)structure;
  }

  public AbstractTreeNode getRoot() {
    final Object rootElement = getRootElement();
    assert rootElement instanceof AbstractTreeNode;
    return (AbstractTreeNode)rootElement;
  }

  @Override
  public void updateFromRoot() {
    updateFromRootCB();
  }

  @NotNull
  public ActionCallback updateFromRootCB() {
    getStructure().rootsChanged();
    if (isDisposed()) return ActionCallback.DONE;
    getUpdater().cancelAllRequests();
    return queueUpdate();
  }

  @NotNull
  @Override
  public Promise<Object> selectAsync(Object element, VirtualFile file, boolean requestFocus) {
    final DefaultMutableTreeNode node = findSmartFirstLevelNodeByElement(element);
    if (node != null) {
      return Promises.toPromise(TreeUtil.selectInTree(node, requestFocus, getTree()));
    }
    return super.selectAsync(element, file, requestFocus);
  }

  @Nullable
  private static DefaultMutableTreeNode findFirstLevelNodeWithObject(final DefaultMutableTreeNode aRoot, final Object aObject) {
    for (int i = 0; i < aRoot.getChildCount(); i++) {
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode)aRoot.getChildAt(i);
      Object userObject = child.getUserObject();
      if (userObject instanceof FavoritesTreeNodeDescriptor) {
        if (Comparing.equal(((FavoritesTreeNodeDescriptor)userObject).getElement(), aObject)) {
          return child;
        }
      }
    }
    return null;
  }

  @Override
  protected Object findNodeByElement(@NotNull Object element) {
    final Object node = findSmartFirstLevelNodeByElement(element);
    if (node != null) return node;
    return super.findNodeByElement(element);
  }

  @Nullable
  DefaultMutableTreeNode findSmartFirstLevelNodeByElement(final Object element) {
    for (Object child : getRoot().getChildren()) {
      AbstractTreeNode favorite = (AbstractTreeNode)child;
      Object currentValue = favorite.getValue();
      if (currentValue instanceof SmartPsiElementPointer) {
        currentValue = ((SmartPsiElementPointer)favorite.getValue()).getElement();
      }
       /*else if (currentValue instanceof PsiJavaFile) {
        final PsiClass[] classes = ((PsiJavaFile)currentValue).getClasses();
        if (classes.length > 0) {
          currentValue = classes[0];
        }
      }*/
      if (Comparing.equal(element, currentValue)) {
        final DefaultMutableTreeNode nodeWithObject =
          findFirstLevelNodeWithObject((DefaultMutableTreeNode)getTree().getModel().getRoot(), favorite);
        if (nodeWithObject != null) {
          return nodeWithObject;
        }
      }
    }
    return null;
  }

  @Override
  protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    final Object[] childElements = getStructure().getChildElements(nodeDescriptor);
    return childElements != null && childElements.length > 0;
  }

  @Override
  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return nodeDescriptor.getParentDescriptor() == null;
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      queueUpdateFrom(getRootNode(), false);
    }

    @Override
    public void fileStatusChanged(@NotNull VirtualFile vFile) {
      PsiElement element = PsiUtilCore.findFileSystemItem(myProject, vFile);
      if (element != null && !addSubtreeToUpdateByElement(element) &&
          element instanceof PsiFile &&
          ((PsiFile)element).getFileType() == StdFileTypes.JAVA) {
        addSubtreeToUpdateByElement(((PsiFile)element).getContainingDirectory());
      }
    }
  }
}

