// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.ProjectTopics;
import com.intellij.ide.CopyPasteUtil;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewPsiTreeChangeListener;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.ProblemListener;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Alarm;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;

public class ProjectTreeBuilder extends BaseProjectTreeBuilder {
  public ProjectTreeBuilder(@NotNull Project project,
                            @NotNull JTree tree,
                            @NotNull DefaultTreeModel treeModel,
                            @Nullable Comparator<NodeDescriptor> comparator,
                            @NotNull ProjectAbstractTreeStructureBase treeStructure) {
    super(project, tree, treeModel, treeStructure, comparator);

    final MessageBusConnection connection = project.getMessageBus().connect(this);

    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        queueUpdate();
      }
    });

    connection.subscribe(BookmarksListener.TOPIC, new MyBookmarksListener());

    PsiManager.getInstance(project).addPsiTreeChangeListener(createPsiTreeChangeListener(project), this);
    FileStatusManager.getInstance(project).addFileStatusListener(new MyFileStatusListener(), this);
    CopyPasteUtil.addDefaultListener(this, this::addSubtreeToUpdateByElement);

    connection.subscribe(ProblemListener.TOPIC, new MyProblemListener());

    setCanYieldUpdate(true);

    initRootNode();
  }

  /**
   * Creates psi tree changes listener. This method will be invoked in constructor of ProjectTreeBuilder
   * thus builder object will be not completely initialized
   * @param project Project
   * @return Listener
   */
  protected ProjectViewPsiTreeChangeListener createPsiTreeChangeListener(final Project project) {
    return new ProjectTreeBuilderPsiListener(project);
  }

  protected class ProjectTreeBuilderPsiListener extends ProjectViewPsiTreeChangeListener {
    public ProjectTreeBuilderPsiListener(final Project project) {
      super(project);
    }

    @Override
    protected DefaultMutableTreeNode getRootNode(){
      return ProjectTreeBuilder.this.getRootNode();
    }

    @Override
    protected AbstractTreeUpdater getUpdater() {
      return ProjectTreeBuilder.this.getUpdater();
    }

    @Override
    protected boolean isFlattenPackages(){
      AbstractTreeStructure structure = getTreeStructure();
      return structure instanceof AbstractProjectTreeStructure && ((AbstractProjectTreeStructure)structure).isFlattenPackages();
    }
  }

  private final class MyBookmarksListener implements BookmarksListener {
    @Override
    public void bookmarkAdded(@NotNull Bookmark b) {
      updateForFile(b.getFile());
    }

    @Override
    public void bookmarkRemoved(@NotNull Bookmark b) {
      updateForFile(b.getFile());
    }

    @Override
    public void bookmarkChanged(@NotNull Bookmark b) {
      updateForFile(b.getFile());
    }

    private void updateForFile(@NotNull VirtualFile file) {
      PsiElement element = findPsi(file);
      if (element != null) {
        queueUpdateFrom(element, false);
      }
    }
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      queueUpdate(false);
    }

    @Override
    public void fileStatusChanged(@NotNull VirtualFile vFile) {
       queueUpdate(false);
    }
  }

  private PsiElement findPsi(@NotNull VirtualFile vFile) {
    return PsiUtilCore.findFileSystemItem(myProject, vFile);
  }

  private class MyProblemListener implements ProblemListener {
    private final Alarm myUpdateProblemAlarm = new Alarm();
    private final Collection<VirtualFile> myFilesToRefresh = new THashSet<>();

    @Override
    public void problemsAppeared(@NotNull VirtualFile file) {
      queueUpdate(file);
    }

    @Override
    public void problemsDisappeared(@NotNull VirtualFile file) {
      queueUpdate(file);
    }

    private void queueUpdate(@NotNull VirtualFile fileToRefresh) {
      synchronized (myFilesToRefresh) {
        if (myFilesToRefresh.add(fileToRefresh)) {
          myUpdateProblemAlarm.cancelAllRequests();
          myUpdateProblemAlarm.addRequest(() -> {
            if (!myProject.isOpen()) return;
            Set<VirtualFile> filesToRefresh;
            synchronized (myFilesToRefresh) {
              filesToRefresh = new THashSet<>(myFilesToRefresh);
            }
            final DefaultMutableTreeNode rootNode = getRootNode();
            if (rootNode != null) {
              updateNodesContaining(filesToRefresh, rootNode);
            }
            synchronized (myFilesToRefresh) {
              myFilesToRefresh.removeAll(filesToRefresh);
            }
          }, 200, ModalityState.NON_MODAL);
        }
      }
    }
  }

  private void updateNodesContaining(@NotNull Collection<? extends VirtualFile> filesToRefresh, @NotNull DefaultMutableTreeNode rootNode) {
    if (!(rootNode.getUserObject() instanceof ProjectViewNode)) return;
    ProjectViewNode node = (ProjectViewNode)rootNode.getUserObject();
    Collection<VirtualFile> containingFiles = null;
    for (VirtualFile virtualFile : filesToRefresh) {
      if (!virtualFile.isValid()) {
        addSubtreeToUpdate(rootNode); // file must be deleted
        return;
      }
      if (node.contains(virtualFile)) {
        if (containingFiles == null) containingFiles = new SmartList<>();
        containingFiles.add(virtualFile);
      }
    }
    if (containingFiles != null) {
      updateNode(rootNode);
      Enumeration children = rootNode.children();
      while (children.hasMoreElements()) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
        updateNodesContaining(containingFiles, child);
      }
    }
  }
}