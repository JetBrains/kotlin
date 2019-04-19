// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs.view;

import com.intellij.compiler.CompilerReferenceService;
import com.intellij.compiler.backwardRefs.CompilerReferenceServiceImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TestCompilerHierarchyBaseAction extends TestCompilerReferenceServiceAction {

  public TestCompilerHierarchyBaseAction(String text) {
    super(text);
  }

  @Override
  protected final void startActionFor(@NotNull PsiElement element) {
    final Project project = element.getProject();
    final CompilerReferenceServiceImpl compilerReferenceService = (CompilerReferenceServiceImpl)CompilerReferenceService.getInstance(
      project);
    final GlobalSearchScope scope = GlobalSearchScopeUtil.toGlobalSearchScope(element.getUseScope(), project);
    final CompilerReferenceHierarchyTestInfo hierarchyTestInfo = getHierarchy(element, compilerReferenceService, scope);
    if (hierarchyTestInfo == null) return;
    InternalCompilerRefServiceView.showHierarchyInfo(hierarchyTestInfo, element);
  }

  @Nullable
  protected abstract CompilerReferenceHierarchyTestInfo getHierarchy(@NotNull PsiElement element,
                                                                     @NotNull CompilerReferenceServiceImpl refService,
                                                                     @NotNull GlobalSearchScope scope);
}
