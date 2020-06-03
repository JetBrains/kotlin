// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.commander;

import com.intellij.ide.CopyPasteUtil;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProjectListBuilder extends AbstractListBuilder {
  private final MyPsiTreeChangeListener myPsiTreeChangeListener;
  private final MyFileStatusListener myFileStatusListener;
  private final CopyPasteManager.ContentChangedListener myCopyPasteListener;
  private final Alarm myUpdateAlarm;

  public ProjectListBuilder(final Project project,
                            final CommanderPanel panel,
                            final AbstractTreeStructure treeStructure,
                            final Comparator comparator,
                            final boolean showRoot) {
    super(project, panel.getList(), panel.getModel(), treeStructure, comparator, showRoot);

    myList.setCellRenderer(new ColoredCommanderRenderer(panel));
    myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, myProject);

    myPsiTreeChangeListener = new MyPsiTreeChangeListener();
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeListener, this);
    myFileStatusListener = new MyFileStatusListener();
    FileStatusManager.getInstance(myProject).addFileStatusListener(myFileStatusListener, this);
    myCopyPasteListener = new MyCopyPasteListener();
    CopyPasteManager.getInstance().addContentChangedListener(myCopyPasteListener, this);
    buildRoot();
  }

  @Override
  protected void updateParentTitle() {
    if (myParentTitle == null) return;

    AbstractTreeNode node = getParentNode();
    if (node instanceof ProjectViewNode) {
      myParentTitle.setText(((ProjectViewNode)node).getTitle());
    }
    else {
      myParentTitle.setText(null);
    }
  }

  @Override
  protected boolean shouldEnterSingleTopLevelElement(Object rootChild) {
    return true;
  }

  @Override
  protected boolean nodeIsAcceptableForElement(AbstractTreeNode node, Object element) {
    return Comparing.equal(node.getValue(), element);
  }

  @Override
  protected List<AbstractTreeNode<?>> getAllAcceptableNodes(final Object[] childElements, VirtualFile file) {
    ArrayList<AbstractTreeNode<?>> result = new ArrayList<>();

    for (Object childElement1 : childElements) {
      ProjectViewNode childElement = (ProjectViewNode)childElement1;
      if (childElement.contains(file)) result.add(childElement);
    }

    return result;
  }

  public void addUpdateRequest() {
    addUpdateRequest(false);
  }
  public void addUpdateRequest(final boolean shouldRefreshSelection) {
    final Runnable request = new Runnable() {
      @Override
      public void run() {
        if (!myProject.isDisposed()) {
          // Rely on project view to commit PSI and wait until it's updated.
          if (myTreeStructure.hasSomethingToCommit() ) {
            myUpdateAlarm.cancelAllRequests();
            myUpdateAlarm.addRequest(this, 300, ModalityState.stateForComponent(myList));
            return;
          }
          updateList(shouldRefreshSelection);
        }
      }
    };

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      myUpdateAlarm.cancelAllRequests();
      myUpdateAlarm.addRequest(request, 300, ModalityState.stateForComponent(myList));
    }
    else {
      request.run();
    }
  }

  public void updateList(final boolean shouldRefreshSelection) {
    updateList();
    if (shouldRefreshSelection) {
      refreshSelection();
    }
  }

  protected void refreshSelection() {}

  private final class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
    private final PsiModificationTracker myModificationTracker;
    private long myOutOfCodeBlockModificationCount;

    private MyPsiTreeChangeListener() {
      myModificationTracker = PsiManager.getInstance(myProject).getModificationTracker();
      myOutOfCodeBlockModificationCount = myModificationTracker.getModificationCount();
    }

    @Override
    public void childRemoved(@NotNull final PsiTreeChangeEvent event) {
      final PsiElement child = event.getOldChild();
      if (child instanceof PsiWhiteSpace) return; //optimization
      childrenChanged();
    }

    @Override
    public void childAdded(@NotNull final PsiTreeChangeEvent event) {
      final PsiElement child = event.getNewChild();
      if (child instanceof PsiWhiteSpace) return; //optimization
      childrenChanged();
    }

    @Override
    public void childReplaced(@NotNull final PsiTreeChangeEvent event) {
      final PsiElement oldChild = event.getOldChild();
      final PsiElement newChild = event.getNewChild();
      if (oldChild instanceof PsiWhiteSpace && newChild instanceof PsiWhiteSpace) return; //optimization
      childrenChanged();
    }

    @Override
    public void childMoved(@NotNull final PsiTreeChangeEvent event) {
      childrenChanged();
    }

    @Override
    public void childrenChanged(@NotNull final PsiTreeChangeEvent event) {
      childrenChanged();
    }

    private void childrenChanged() {
      long newModificationCount = myModificationTracker.getModificationCount();
      if (newModificationCount == myOutOfCodeBlockModificationCount) return;
      myOutOfCodeBlockModificationCount = newModificationCount;
      addUpdateRequest();
    }

    @Override
    public void propertyChanged(@NotNull final PsiTreeChangeEvent event) {
      final String propertyName = event.getPropertyName();
      if (propertyName.equals(PsiTreeChangeEvent.PROP_ROOTS)) {
        addUpdateRequest();
      }
      else if (propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)){
        childrenChanged();
      }
      else if (propertyName.equals(PsiTreeChangeEvent.PROP_FILE_NAME) || propertyName.equals(PsiTreeChangeEvent.PROP_DIRECTORY_NAME)){
        childrenChanged();
      }
      else if (propertyName.equals(PsiTreeChangeEvent.PROP_FILE_TYPES)){
        addUpdateRequest();
      }
    }
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      addUpdateRequest();
    }

    @Override
    public void fileStatusChanged(@NotNull final VirtualFile vFile) {
      PsiFileSystemItem item = PsiUtilCore.findFileSystemItem(myProject, vFile);
      if (item != null) myPsiTreeChangeListener.childrenChanged();
    }
  }

  private final class MyCopyPasteListener implements CopyPasteManager.ContentChangedListener {
    @Override
    public void contentChanged(final Transferable oldTransferable, final Transferable newTransferable) {
      updateByTransferable(oldTransferable);
      updateByTransferable(newTransferable);
    }

    private void updateByTransferable(final Transferable t) {
      final PsiElement[] psiElements = CopyPasteUtil.getElementsInTransferable(t);
      for (int i = 0; i < psiElements.length; i++) {
        myPsiTreeChangeListener.childrenChanged();
      }
    }
  }
}
