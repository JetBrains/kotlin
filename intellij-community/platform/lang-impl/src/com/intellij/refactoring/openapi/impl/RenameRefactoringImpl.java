// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.openapi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringImpl;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;

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

  @Override
  public void respectEnabledAutomaticRenames() {
    for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
      if (factory.getOptionName() != null && factory.isEnabled() && getElements().stream().anyMatch(element -> factory.isApplicable(element))) {
        myProcessor.addRenamerFactory(factory);
      }
    }
  }

  @Override
  public void respectAllAutomaticRenames() {
    for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
      if (factory.getOptionName() != null && getElements().stream().anyMatch(element -> factory.isApplicable(element))) {
        myProcessor.addRenamerFactory(factory);
      }
    }
  }

  @Override
  public boolean hasNonCodeUsages() {
    return myProcessor.hasNonCodeUsages();
  }
}
