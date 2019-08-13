/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.execution.impl;

import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.UndoRefactoringElementListener;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class NameGeneratingListenerDecorator implements RefactoringElementListener, UndoRefactoringElementListener {
  private final LocatableConfiguration myConfiguration;
  private final RefactoringElementListener myListener;

  public NameGeneratingListenerDecorator(LocatableConfiguration configuration, RefactoringElementListener listener) {
    myConfiguration = configuration;
    myListener = listener;
  }

  @Override
  public void elementMoved(@NotNull PsiElement newElement) {
    boolean hasGeneratedName = myConfiguration.isGeneratedName();
    myListener.elementMoved(newElement);
    if (hasGeneratedName) {
      myConfiguration.setName(myConfiguration.suggestedName());
    }
  }

  @Override
  public void elementRenamed(@NotNull PsiElement newElement) {
    boolean hasGeneratedName = myConfiguration.isGeneratedName();
    myListener.elementRenamed(newElement);
    if (hasGeneratedName) {
      myConfiguration.setName(myConfiguration.suggestedName());
    }
  }

  @Override
  public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
    if (myListener instanceof UndoRefactoringElementListener) {
      boolean hasGeneratedName = myConfiguration.isGeneratedName();
      ((UndoRefactoringElementListener) myListener).undoElementMovedOrRenamed(newElement, oldQualifiedName);
      if (hasGeneratedName) {
        myConfiguration.setName(myConfiguration.suggestedName());
      }
    }
  }
}
