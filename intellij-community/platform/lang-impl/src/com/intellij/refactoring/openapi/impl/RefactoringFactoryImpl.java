/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.refactoring.openapi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.SafeDeleteRefactoring;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class RefactoringFactoryImpl extends RefactoringFactory {
  private final Project myProject;

  public RefactoringFactoryImpl(final Project project) {
    myProject = project;
  }

  @Override
  public RenameRefactoring createRename(@NotNull final PsiElement element, final String newName) {
    return new RenameRefactoringImpl(myProject, element, newName, true, true);
  }

  @Override
  public RenameRefactoring createRename(@NotNull PsiElement element, String newName, boolean searchInComments, boolean searchInNonJavaFiles) {
    return new RenameRefactoringImpl(myProject, element, newName, searchInComments, searchInNonJavaFiles);
  }

  @Override
  public SafeDeleteRefactoring createSafeDelete(final PsiElement[] elements) {
    return new SafeDeleteRefactoringImpl(myProject, elements);
  }
}
