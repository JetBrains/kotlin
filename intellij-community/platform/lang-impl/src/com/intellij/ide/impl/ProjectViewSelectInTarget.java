// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl;

import com.intellij.ide.CompositeSelectInTarget;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.SelectableTreeStructureProvider;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.notebook.editor.BackedVirtualFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.intellij.ide.projectView.impl.ProjectViewPane.canBeSelectedInProjectView;
import static com.intellij.psi.SmartPointerManager.createPointer;

public abstract class ProjectViewSelectInTarget extends SelectInTargetPsiWrapper implements CompositeSelectInTarget {
  private String mySubId;

  protected ProjectViewSelectInTarget(Project project) {
    super(project);
  }

  @Override
  protected final void select(final Object selector, final VirtualFile virtualFile, final boolean requestFocus) {
    select(myProject, selector, getMinorViewId(), mySubId, virtualFile, requestFocus);
  }

  @NotNull
  public static ActionCallback select(@NotNull Project project,
                                      final Object toSelect,
                                      @Nullable final String viewId,
                                      @Nullable final String subviewId,
                                      final VirtualFile virtualFile,
                                      final boolean requestFocus) {
    final ProjectView projectView = ProjectView.getInstance(project);
    if (projectView == null) return ActionCallback.REJECTED;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      AbstractProjectViewPane pane = projectView.getProjectViewPaneById(ObjectUtils.chooseNotNull(viewId, ProjectViewImpl.getDefaultViewId()));
      pane.select(toSelect, virtualFile, requestFocus);
      return ActionCallback.DONE;
    }

    Supplier<Object> toSelectSupplier = toSelect instanceof PsiElement
                                        ? createPointer((PsiElement)toSelect)::getElement
                                        : () -> toSelect;

    ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
    final ToolWindow projectViewToolWindow = windowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
    if (projectViewToolWindow == null) return ActionCallback.REJECTED;

    ActionCallback result = new ActionCallback();
    final Runnable runnable = () -> {
      Runnable r = () -> projectView.selectCB(toSelectSupplier.get(), virtualFile, requestFocus).notify(result);
      projectView.changeViewCB(ObjectUtils.chooseNotNull(viewId, ProjectViewImpl.getDefaultViewId()), subviewId).doWhenProcessed(r);
    };

    if (requestFocus) {
      projectViewToolWindow.activate(runnable, true);
    }
    else {
      projectViewToolWindow.show(runnable);
    }

    return result;
  }

  @Override
  @NotNull
  public Collection<SelectInTarget> getSubTargets(@NotNull SelectInContext context) {
    List<SelectInTarget> result = new ArrayList<>();
    AbstractProjectViewPane pane = ProjectView.getInstance(myProject).getProjectViewPaneById(getMinorViewId());
    int index = 0;
    for (String subId : pane.getSubIds()) {
      result.add(new ProjectSubViewSelectInTarget(this, subId, index++));
    }
    return result;
  }

  public boolean isSubIdSelectable(String subId, SelectInContext context) {
    return false;
  }

  @Override
  protected boolean canSelect(PsiFileSystemItem file) {
    VirtualFile vFile = PsiUtilCore.getVirtualFile(file);
    vFile = BackedVirtualFile.getOriginFileIfBacked(vFile);
    if (vFile == null || !vFile.isValid()) return false;

    return canBeSelectedInProjectView(myProject, vFile);
  }

  public String getSubIdPresentableName(String subId) {
    AbstractProjectViewPane pane = ProjectView.getInstance(myProject).getProjectViewPaneById(getMinorViewId());
    return pane.getPresentableSubIdName(subId);
  }

  @Override
  public void select(PsiElement element, final boolean requestFocus) {
    PsiUtilCore.ensureValid(element);
    PsiElement toSelect = null;
    for (TreeStructureProvider provider : getProvidersDumbAware()) {
      if (provider instanceof SelectableTreeStructureProvider) {
        toSelect = ((SelectableTreeStructureProvider) provider).getTopLevelElement(element);
      }
      if (toSelect != null) {
        if (!toSelect.isValid()) {
          throw new PsiInvalidElementAccessException(toSelect, "Returned by " + provider);
        }
        break;
      }
    }

    toSelect = findElementToSelect(element, toSelect);

    if (toSelect != null) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(toSelect);
      virtualFile = BackedVirtualFile.getOriginFileIfBacked(virtualFile);
      select(toSelect, virtualFile, requestFocus);
    }
  }

  private TreeStructureProvider[] getProvidersDumbAware() {
    List<TreeStructureProvider> dumbAware = DumbService.getInstance(myProject).filterByDumbAwareness(TreeStructureProvider.EP.getExtensions(myProject));
    return dumbAware.toArray(new TreeStructureProvider[0]);
  }

  @Override
  public final String getToolWindowId() {
    return ToolWindowId.PROJECT_VIEW;
  }

  public final void setSubId(String subId) {
    mySubId = subId;
  }

  public final String getSubId() {
    return mySubId;
  }
}