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

package com.intellij.ide.todo;

import com.intellij.ide.todo.nodes.ToDoRootNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.vcsUtil.VcsUtil;

import java.util.Collection;

public class ChangeListTodosTreeStructure extends TodoTreeStructure {
  public ChangeListTodosTreeStructure(Project project) {
    super(project);
  }

  @Override
  public boolean accept(final PsiFile psiFile) {
    if (!psiFile.isValid()) return false;

    VirtualFile file = psiFile.getVirtualFile();
    ChangeListManager listManager = ChangeListManager.getInstance(myProject);

    FileStatus status = listManager.getStatus(file);
    if (status == FileStatus.NOT_CHANGED) return false;

    FilePath filePath = VcsUtil.getFilePath(file);
    final Collection<Change> changes = listManager.getDefaultChangeList().getChanges();
    for (Change change : changes) {
      ContentRevision afterRevision = change.getAfterRevision();
      if (afterRevision != null && afterRevision.getFile().equals(filePath)) {
        return (myTodoFilter != null && myTodoFilter.accept(mySearchHelper, psiFile) ||
                (myTodoFilter == null && mySearchHelper.getTodoItemsCount(psiFile) > 0));
      }
    }
    return false;
  }

  @Override
  public boolean getIsPackagesShown() {
    return myArePackagesShown;
  }

  @Override
  Object getFirstSelectableElement() {
    return ((ToDoRootNode)myRootElement).getSummaryNode();
  }

  @Override
  protected AbstractTreeNode createRootElement() {
    return new ToDoRootNode(myProject, new Object(), myBuilder, mySummaryElement);
  }
}