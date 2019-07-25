// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs.view;

import com.intellij.compiler.backwardRefs.CompilerReferenceServiceImpl;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.LambdaUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestCompilerRefFunctionalExpressionSearchAction extends TestCompilerHierarchyBaseAction {

  public TestCompilerRefFunctionalExpressionSearchAction() {
    super("Compiler Reference Functional Expression Search");
  }

  @Nullable
  @Override
  protected CompilerReferenceHierarchyTestInfo getHierarchy(@NotNull PsiElement element,
                                                            @NotNull CompilerReferenceServiceImpl refService,
                                                            @NotNull GlobalSearchScope scope) {
    return refService.getTestFunExpressions((PsiNamedElement)element, scope, StdFileTypes.JAVA);
  }

  @Override
  protected boolean canBeAppliedFor(@NotNull PsiElement element) {
    return element instanceof PsiClass && LambdaUtil.isFunctionalClass((PsiClass)element);
  }
}
