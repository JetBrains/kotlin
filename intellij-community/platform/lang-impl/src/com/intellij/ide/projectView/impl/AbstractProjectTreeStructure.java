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

package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractProjectTreeStructure extends ProjectAbstractTreeStructureBase implements ViewSettings {
  private final AbstractTreeNode myRoot;

  public AbstractProjectTreeStructure(@NotNull Project project) {
    super(project);
    myRoot = createRoot(project, this);
  }

  protected AbstractTreeNode createRoot(@NotNull Project project, @NotNull ViewSettings settings) {
    return new ProjectViewProjectNode(myProject, this);
  }

  @Override
  public abstract boolean isShowMembers();

  @NotNull
  @Override
  public final Object getRootElement() {
    return myRoot;
  }

  @Override
  public final void commit() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
  }

  @NotNull
  @Override
  public ActionCallback asyncCommit() {
    return asyncCommitDocuments(myProject);
  }

  @Override
  public final boolean hasSomethingToCommit() {
    return !myProject.isDisposed()
           && PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }

  @Override
  public boolean isStructureView() {
    return false;
  }

  @Override
  public boolean isAlwaysLeaf(@NotNull Object element) {
    if (element instanceof ProjectViewNode) {
      return ((ProjectViewNode)element).isAlwaysLeaf();
    }
    return super.isAlwaysLeaf(element);
  }
}