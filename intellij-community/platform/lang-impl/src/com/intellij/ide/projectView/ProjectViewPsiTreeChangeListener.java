/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.projectView;

import com.intellij.ide.scratch.ScratchProjectViewPane;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

import static com.intellij.util.ObjectUtils.notNull;

public abstract class ProjectViewPsiTreeChangeListener extends PsiTreeChangeAdapter {
  private final PsiModificationTracker myModificationTracker;
  private long myModificationCount;

  protected ProjectViewPsiTreeChangeListener(@NotNull Project project) {
    myModificationTracker = PsiManager.getInstance(project).getModificationTracker();
    myModificationCount = myModificationTracker.getModificationCount();
  }

  protected abstract AbstractTreeUpdater getUpdater();

  protected abstract boolean isFlattenPackages();

  protected abstract DefaultMutableTreeNode getRootNode();

  @Override
  public final void childRemoved(@NotNull PsiTreeChangeEvent event) {
    PsiElement child = event.getOldChild();
    if (child instanceof PsiWhiteSpace) return; //optimization
    childrenChanged(event.getParent(), true);
  }

  @Override
  public final void childAdded(@NotNull PsiTreeChangeEvent event) {
    PsiElement child = event.getNewChild();
    if (child instanceof PsiWhiteSpace) return; //optimization
    childrenChanged(event.getParent(), true);
  }

  @Override
  public final void childReplaced(@NotNull PsiTreeChangeEvent event) {
    PsiElement oldChild = event.getOldChild();
    PsiElement newChild = event.getNewChild();
    if (oldChild instanceof PsiWhiteSpace && newChild instanceof PsiWhiteSpace) return; //optimization
    childrenChanged(event.getParent(), true);
  }

  @Override
  public final void childMoved(@NotNull PsiTreeChangeEvent event) {
    childrenChanged(event.getOldParent(), false);
    childrenChanged(event.getNewParent(), true);
  }

  @Override
  public final void childrenChanged(@NotNull PsiTreeChangeEvent event) {
    childrenChanged(event.getParent(), true);
  }

  protected void childrenChanged(PsiElement parent, final boolean stopProcessingForThisModificationCount) {
    if (parent instanceof PsiDirectory && isFlattenPackages()){
      addSubtreeToUpdateByRoot();
      return;
    }

    long newModificationCount = myModificationTracker.getModificationCount();
    if (newModificationCount == myModificationCount) return;
    if (stopProcessingForThisModificationCount) {
      myModificationCount = newModificationCount;
    }

    while (true) {
      if (parent == null) break;
      if (parent instanceof PsiFile) {
        VirtualFile virtualFile = ((PsiFile)parent).getVirtualFile();
        if (virtualFile != null && virtualFile.getFileType() != FileTypes.PLAIN_TEXT) {
          // adding a class within a file causes a new node to appear in project view => entire dir should be updated
          parent = ((PsiFile)parent).getContainingDirectory();
          if (parent == null) break;
        }
      }
      else if (parent instanceof PsiDirectory &&
               ScratchProjectViewPane.isScratchesMergedIntoProjectTab() &&
               ScratchUtil.isScratch(((PsiDirectory)parent).getVirtualFile())) {
        addSubtreeToUpdateByRoot();
        break;
      }

      if (addSubtreeToUpdateByElementFile(parent)) {
        break;
      }

      if (parent instanceof PsiFile || parent instanceof PsiDirectory) break;
      parent = parent.getParent();
    }
  }

  @Override
  public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
    String propertyName = event.getPropertyName();
    PsiElement element = event.getElement();
    if (propertyName.equals(PsiTreeChangeEvent.PROP_ROOTS)) {
      addSubtreeToUpdateByRoot();
    }
    else if (propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)){
      if (!addSubtreeToUpdateByElementFile(element) && element instanceof PsiFile) {
        addSubtreeToUpdateByElementFile(((PsiFile)element).getContainingDirectory());
      }
    }
    else if (propertyName.equals(PsiTreeChangeEvent.PROP_FILE_NAME) || propertyName.equals(PsiTreeChangeEvent.PROP_DIRECTORY_NAME)){
      if (element instanceof PsiDirectory && isFlattenPackages()){
        addSubtreeToUpdateByRoot();
        return;
      }
      final PsiElement parent = element.getParent();
      if (parent == null || !addSubtreeToUpdateByElementFile(parent)) {
        addSubtreeToUpdateByElementFile(element);
      }
    }
    else if (propertyName.equals(PsiTreeChangeEvent.PROP_FILE_TYPES) || propertyName.equals(PsiTreeChangeEvent.PROP_UNLOADED_PSI)) {
      addSubtreeToUpdateByRoot();
    }
  }

  protected void addSubtreeToUpdateByRoot() {
    AbstractTreeUpdater updater = getUpdater();
    DefaultMutableTreeNode root = getRootNode();
    if (updater != null && root != null) updater.addSubtreeToUpdate(root);
  }

  protected boolean addSubtreeToUpdateByElement(@NotNull PsiElement element) {
    AbstractTreeUpdater updater = getUpdater();
    return updater != null && updater.addSubtreeToUpdateByElement(element);
  }

  private boolean addSubtreeToUpdateByElementFile(PsiElement element) {
    return element != null && addSubtreeToUpdateByElement(notNull(element.getContainingFile(), element));
  }
}
