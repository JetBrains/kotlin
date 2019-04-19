// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.scopeView;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.StandardTargetWeights;
import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author cdr
 */
public class ScopePaneSelectInTarget extends ProjectViewSelectInTarget {
  public ScopePaneSelectInTarget(final Project project) {
    super(project);
  }

  public String toString() {
    return SelectInManager.SCOPE;
  }

  @Override
  public boolean canSelect(PsiFileSystemItem fileSystemItem) {
    VirtualFile file = PsiUtilCore.getVirtualFile(fileSystemItem);
    if (file == null || !file.isValid()) return false;
    ProjectRootManager manager = myProject.isDisposed() ? null : ProjectRootManager.getInstance(myProject);
    if (manager == null || null == manager.getFileIndex().getModuleForFile(file)) return false; // libraries are not supported yet
    if (!(fileSystemItem instanceof PsiFile)) return false;
    return getContainingFilter((PsiFile)fileSystemItem) != null;
  }

  @Nullable
  private NamedScopeFilter getContainingFilter(@Nullable PsiFile file) {
    if (file == null) return null;
    if (ScratchUtil.isScratch(file.getVirtualFile())) return null;
    ScopeViewPane pane = getScopeViewPane();
    if (pane == null) return null;
    for (NamedScopeFilter filter : pane.getFilters()) {
      if (filter.accept(file.getVirtualFile())) return filter;
    }
    return null;
  }

  @Override
  public void select(PsiElement element, boolean requestFocus) {
    if (getSubId() == null) {
      NamedScopeFilter filter = getContainingFilter(element.getContainingFile());
      if (filter == null) return;
      setSubId(filter.toString());
    }
    super.select(element, requestFocus);
  }

  @Override
  public String getMinorViewId() {
    return ScopeViewPane.ID;
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.SCOPE_WEIGHT;
  }

  @Override
  public boolean isSubIdSelectable(@NotNull String subId, @NotNull SelectInContext context) {
    PsiFileSystemItem file = getContextPsiFile(context);
    if (!(file instanceof PsiFile)) return false;
    ScopeViewPane pane = getScopeViewPane();
    NamedScopeFilter filter = pane == null ? null : pane.getFilter(subId);
    return filter != null && filter.accept(file.getVirtualFile());
  }

  private ScopeViewPane getScopeViewPane() {
    ProjectView view = ProjectView.getInstance(myProject);
    Object pane = view == null ? null : view.getProjectViewPaneById(ScopeViewPane.ID);
    return pane instanceof ScopeViewPane ? (ScopeViewPane)pane : null;
  }
}
