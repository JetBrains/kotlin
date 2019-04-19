/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.RefactoringListenerProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerComposite;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;

/**
 * @author spleaner
*/
public class RunConfigurationRefactoringElementListenerProvider implements RefactoringElementListenerProvider {
  private static final Logger LOG = Logger.getInstance("#com.intellij.execution.impl.RunConfigurationRefactoringElementListenerProvider");

  @Override
  public RefactoringElementListener getListener(final PsiElement element) {
    RefactoringElementListenerComposite composite = null;
    for (RunConfiguration configuration : RunManager.getInstance(element.getProject()).getAllConfigurationsList()) {
      if (configuration instanceof RefactoringListenerProvider) { // todo: perhaps better way to handle listeners?
        RefactoringElementListener listener;
        try {
          listener = ((RefactoringListenerProvider)configuration).getRefactoringElementListener(element);
        }
        catch (Exception e) {
          LOG.error(e);
          continue;
        }
        if (listener != null) {
          if (configuration instanceof LocatableConfiguration) {
            listener = new NameGeneratingListenerDecorator((LocatableConfiguration)configuration, listener);
          }
          if (composite == null) {
            composite = new RefactoringElementListenerComposite();
          }
          composite.addListener(listener);
        }
      }
    }
    return composite;
  }
}
