/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.util.treeView.TreeAnchorizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class PsiTreeAnchorizer extends TreeAnchorizer {
  @NotNull
  @Override
  public Object createAnchor(@NotNull Object element) {
    if (element instanceof PsiElement) {
      PsiElement psi = (PsiElement)element;
      return ReadAction.compute(() -> {
        if (!psi.isValid()) return psi;
        return SmartPointerManager.getInstance(psi.getProject()).createSmartPsiElementPointer(psi);
      });
    }
    return super.createAnchor(element);
  }
  @Override
  @Nullable
  public Object retrieveElement(@NotNull final Object pointer) {
    if (pointer instanceof SmartPsiElementPointer) {
      return ReadAction.compute(() -> ((SmartPsiElementPointer)pointer).getElement());
    }

    return super.retrieveElement(pointer);
  }

  @Override
  public void freeAnchor(final Object element) {
    if (element instanceof SmartPsiElementPointer) {
      ApplicationManager.getApplication().runReadAction(() -> {
        SmartPsiElementPointer pointer = (SmartPsiElementPointer)element;
        Project project = pointer.getProject();
        if (!project.isDisposed()) {
          SmartPointerManager.getInstance(project).removePointer(pointer);
        }
      });
    }
  }
}
