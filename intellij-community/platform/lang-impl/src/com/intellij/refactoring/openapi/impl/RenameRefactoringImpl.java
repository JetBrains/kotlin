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
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringImpl;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.rename.RenameProcessor;

import java.util.Collection;
import java.util.Set;

public class RenameRefactoringImpl extends RefactoringImpl<RenameProcessor> implements RenameRefactoring {
  public RenameRefactoringImpl(Project project,
                               PsiElement element,
                               String newName,
                               boolean toSearchInComments,
                               boolean toSearchInNonJavaFiles) {
    super(new RenameProcessor(project, element, newName, toSearchInComments, toSearchInNonJavaFiles));
  }

  @Override
  public void addElement(PsiElement element, String newName) {
    myProcessor.addElement(element, newName);
  }

  @Override
  public Set<PsiElement> getElements() {
    return myProcessor.getElements();
  }

  @Override
  public Collection<String> getNewNames() {
    return myProcessor.getNewNames();
  }

  @Override
  public void setSearchInComments(boolean value) {
    myProcessor.setSearchInComments(value);
  }

  @Override
  public void setSearchInNonJavaFiles(boolean value) {
    myProcessor.setSearchTextOccurrences(value);
  }

  @Override
  public boolean isSearchInComments() {
    return myProcessor.isSearchInComments();
  }

  @Override
  public boolean isSearchInNonJavaFiles() {
    return myProcessor.isSearchTextOccurrences();
  }
}
