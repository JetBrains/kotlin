/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.impl;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SelectInTargetPsiWrapper implements SelectInTarget {
  protected final Project myProject;

  protected SelectInTargetPsiWrapper(@NotNull final Project project) {
    myProject = project;
  }

  public abstract String toString();

  protected abstract boolean canSelect(PsiFileSystemItem file);

  @Override
  public final boolean canSelect(@NotNull SelectInContext context) {
    if (!isContextValid(context)) return false;

    return canSelectInner(context);
  }

  protected boolean canSelectInner(@NotNull SelectInContext context) {
    PsiFileSystemItem psiFile = getContextPsiFile(context);
    return psiFile != null && canSelect(psiFile);
  }

  private boolean isContextValid(SelectInContext context) {
    if (myProject.isDisposed()) return false;

    VirtualFile virtualFile = context.getVirtualFile();
    return virtualFile.isValid();
  }

  @Nullable
  protected PsiFileSystemItem getContextPsiFile(@NotNull SelectInContext context) {
    VirtualFile virtualFile = context.getVirtualFile();
    PsiFileSystemItem psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
    if (psiFile != null) {
      return psiFile;
    }

    if (context.getSelectorInFile() instanceof PsiFile) {
      return (PsiFile)context.getSelectorInFile();
    }
    if (virtualFile.isDirectory()) {
      return PsiManager.getInstance(myProject).findDirectory(virtualFile);
    }
    return null;
  }

  @Override
  public final void selectIn(@NotNull SelectInContext context, boolean requestFocus) {
    VirtualFile file = context.getVirtualFile();
    Object selector = context.getSelectorInFile();
    if (selector == null) {
      selector = PsiUtilCore.findFileSystemItem(myProject, file);
    }

    if (selector instanceof PsiElement) {
      PsiUtilCore.ensureValid((PsiElement)selector);
      PsiElement original = ((PsiElement)selector).getOriginalElement();
      if (original != null && !original.isValid()) {
        throw new PsiInvalidElementAccessException(original, "Returned by " + selector + " of " + selector.getClass());
      }
      select(original, requestFocus);
    }
    else {
      select(selector, file, requestFocus);
    }
  }

  protected abstract void select(Object selector, VirtualFile virtualFile, boolean requestFocus);

  /**
   * @deprecated unused, implement canSelectInner(context) instead
   */
  @Deprecated
  protected boolean canWorkWithCustomObjects() {
    return false;
  }

  protected abstract void select(PsiElement element, boolean requestFocus);

  @Nullable
  protected static PsiElement findElementToSelect(PsiElement element, PsiElement candidate) {
    PsiElement toSelect = candidate;

    if (toSelect == null) {
      if (element instanceof PsiFile || element instanceof PsiDirectory) {
        toSelect = element;
      }
      else {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null) {
          FileViewProvider viewProvider = containingFile.getViewProvider();
          toSelect = viewProvider.getPsi(viewProvider.getBaseLanguage());
        }
      }
    }

    if (toSelect != null) {
      PsiElement originalElement = null;
      try {
        originalElement = toSelect.getOriginalElement();
      }
      catch (IndexNotReadyException ignored) { }
      if (originalElement != null) {
        toSelect = originalElement;
      }
    }

    return toSelect;
  }
}