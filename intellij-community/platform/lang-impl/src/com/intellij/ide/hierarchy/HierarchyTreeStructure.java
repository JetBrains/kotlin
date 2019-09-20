// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.tree.LeafState;
import com.intellij.usageView.UsageViewLongNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

public abstract class HierarchyTreeStructure extends AbstractTreeStructure {
  protected HierarchyNodeDescriptor myBaseDescriptor;
  private HierarchyNodeDescriptor myRoot;
  @NotNull
  protected final Project myProject;

  protected HierarchyTreeStructure(@NotNull Project project, HierarchyNodeDescriptor baseDescriptor) {
    myBaseDescriptor = baseDescriptor;
    myProject = project;
    myRoot = baseDescriptor;
  }

  public final HierarchyNodeDescriptor getBaseDescriptor() {
    return myBaseDescriptor;
  }

  protected final void setBaseElement(@NotNull HierarchyNodeDescriptor baseElement) {
    myBaseDescriptor = baseElement;
    myRoot = baseElement;
    while(myRoot.getParentDescriptor() != null){
      myRoot = (HierarchyNodeDescriptor)myRoot.getParentDescriptor();
    }
  }

  @Override
  @NotNull
  public final NodeDescriptor createDescriptor(@NotNull final Object element, final NodeDescriptor parentDescriptor) {
    if (element instanceof HierarchyNodeDescriptor) {
      return (HierarchyNodeDescriptor)element;
    }
    if (element instanceof String) {
      return new TextInfoNodeDescriptor(parentDescriptor, (String)element, myProject);
    }
    throw new IllegalArgumentException("Unknown element type: " + element);
  }

  @Override
  public final boolean isToBuildChildrenInBackground(@NotNull final Object element) {
    if (element instanceof HierarchyNodeDescriptor){
      final HierarchyNodeDescriptor descriptor = (HierarchyNodeDescriptor)element;
      final Object[] cachedChildren = descriptor.getCachedChildren();
      return cachedChildren == null && descriptor.isValid();
    }
    return false;
  }

  @NotNull
  @Override
  public final Object[] getChildElements(@NotNull final Object element) {
    if (element instanceof HierarchyNodeDescriptor) {
      final HierarchyNodeDescriptor descriptor = (HierarchyNodeDescriptor)element;
      Object[] cachedChildren = descriptor.getCachedChildren();
      if (cachedChildren == null) {
        if (descriptor.isValid()) {
          try {
            cachedChildren = AbstractTreeUi.calculateYieldingToWriteAction(() -> buildChildren(descriptor));
          }
          catch (IndexNotReadyException e) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
          }
        }
        else {
          cachedChildren = ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        descriptor.setCachedChildren(cachedChildren);
      }
      return cachedChildren;
    }
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public final Object getParentElement(@NotNull final Object element) {
    if (element instanceof HierarchyNodeDescriptor) {
      return ((HierarchyNodeDescriptor)element).getParentDescriptor();
    }

    return null;
  }

  @Override
  public final void commit() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
  }

  @Override
  public final boolean hasSomethingToCommit() {
    return PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }
  @NotNull
  @Override
  public ActionCallback asyncCommit() {
    return asyncCommitDocuments(myProject);
  }

  @NotNull
  protected abstract Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor);

  @NotNull
  @Override
  public final Object getRootElement() {
    return myRoot;
  }

  protected SearchScope getSearchScope(final String scopeType, final PsiElement thisClass) {
    SearchScope searchScope = GlobalSearchScope.allScope(myProject);
    if (HierarchyBrowserBaseEx.SCOPE_CLASS.equals(scopeType)) {
      searchScope = new LocalSearchScope(thisClass);
    }
    else if (HierarchyBrowserBaseEx.SCOPE_PROJECT.equals(scopeType)) {
      searchScope = GlobalSearchScopesCore.projectProductionScope(myProject);
    }
    else if (HierarchyBrowserBaseEx.SCOPE_TEST.equals(scopeType)) {
      searchScope = GlobalSearchScopesCore.projectTestScope(myProject);
    } else {
      final NamedScope namedScope = NamedScopesHolder.getScope(myProject, scopeType);
      if (namedScope != null) {
        searchScope = GlobalSearchScopesCore.filterScope(myProject, namedScope);
      }
    }
    return searchScope;
  }

  protected boolean isInScope(final PsiElement baseClass, @NotNull PsiElement srcElement, final String scopeType) {
    if (HierarchyBrowserBaseEx.SCOPE_CLASS.equals(scopeType)) {
      return PsiTreeUtil.isAncestor(baseClass, srcElement, true);
    }
    if (HierarchyBrowserBaseEx.SCOPE_PROJECT.equals(scopeType)) {
      final VirtualFile virtualFile = srcElement.getContainingFile().getVirtualFile();
      return virtualFile == null || !TestSourcesFilter.isTestSources(virtualFile, myProject);
    }
    if (HierarchyBrowserBaseEx.SCOPE_TEST.equals(scopeType)) {
      final VirtualFile virtualFile = srcElement.getContainingFile().getVirtualFile();
      return virtualFile == null || TestSourcesFilter.isTestSources(virtualFile, myProject);
    }
    if (HierarchyBrowserBaseEx.SCOPE_ALL.equals(scopeType)) {
      return true;
    }
    final NamedScope namedScope = NamedScopesHolder.getScope(myProject, scopeType);
    if (namedScope == null) {
      return false;
    }
    final PackageSet namedScopePattern = namedScope.getValue();
    if (namedScopePattern == null) {
      return false;
    }
    PsiFile psiFile = srcElement.getContainingFile();
    if (psiFile == null) {
      return true;
    }
    NamedScopesHolder holder = NamedScopesHolder.getHolder(myProject, scopeType, NamedScopeManager.getInstance(myProject));
    return namedScopePattern.contains(psiFile, holder);
  }

  private static final class TextInfoNodeDescriptor extends NodeDescriptor {
    TextInfoNodeDescriptor(final NodeDescriptor parentDescriptor, final String text, final Project project) {
      super(project, parentDescriptor);
      myName = text;
      myColor = JBColor.RED;
    }

    @Override
    public final Object getElement() {
      return myName;
    }

    @Override
    public final boolean update() {
      return true;
    }
  }

  @NotNull
  @Override
  public LeafState getLeafState(@NotNull Object element) {
    if (isAlwaysShowPlus()) return LeafState.NEVER;
    LeafState state = super.getLeafState(element);
    return state != LeafState.DEFAULT ? state : LeafState.ASYNC;
  }

  public boolean isAlwaysShowPlus() {
    return false;
  }

  @NotNull
  protected String formatBaseElementText() {
    HierarchyNodeDescriptor descriptor = getBaseDescriptor();
    if (descriptor == null) return toString();
    PsiElement element = descriptor.getPsiElement();
    if (element == null) return descriptor.toString();
    return ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE) + " " +
           ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE);
  }
}
