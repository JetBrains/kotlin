// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.listeners.impl;

import com.intellij.openapi.project.Project;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import com.intellij.refactoring.listeners.RefactoringListenerManager;
import com.intellij.refactoring.listeners.impl.impl.RefactoringTransactionImpl;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dsl
 */
public class RefactoringListenerManagerImpl extends RefactoringListenerManager {
  private final List<RefactoringElementListenerProvider> myListenerProviders = ContainerUtil.createLockFreeCopyOnWriteList();
  private final Project myProject;

  public RefactoringListenerManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public void addListenerProvider(RefactoringElementListenerProvider provider) {
    myListenerProviders.add(provider);
  }

  @Override
  public void removeListenerProvider(RefactoringElementListenerProvider provider) {
    myListenerProviders.remove(provider);
  }

  public RefactoringTransaction startTransaction() {
    List<RefactoringElementListenerProvider> providers = new ArrayList<>(myListenerProviders);
    Collections.addAll(providers, RefactoringElementListenerProvider.EP_NAME.getExtensions(myProject));
    return new RefactoringTransactionImpl(myProject, providers);
  }
}
