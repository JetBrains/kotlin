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
package com.intellij.refactoring.openapi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringImpl;
import com.intellij.refactoring.SafeDeleteRefactoring;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

/**
 * @author dsl
 */
public class SafeDeleteRefactoringImpl extends RefactoringImpl<SafeDeleteProcessor> implements SafeDeleteRefactoring {
  public SafeDeleteRefactoringImpl(Project project, PsiElement[] elements) {
    super(SafeDeleteProcessor.createInstance(project, EmptyRunnable.INSTANCE, elements, true, true));
  }

  @Override
  public List<PsiElement> getElements() {
    final PsiElement[] elements = myProcessor.getElements();
    return ContainerUtil.immutableList(elements);
  }

  @Override
  public boolean isSearchInComments() {
    return myProcessor.isSearchInCommentsAndStrings();
  }

  @Override
  public void setSearchInComments(boolean value) {
    myProcessor.setSearchInCommentsAndStrings(value);
  }

  @Override
  public void setSearchInNonJavaFiles(boolean value) {
    myProcessor.setSearchNonJava(value);
  }

  @Override
  public boolean isSearchInNonJavaFiles() {
    return myProcessor.isSearchNonJava();
  }
}
