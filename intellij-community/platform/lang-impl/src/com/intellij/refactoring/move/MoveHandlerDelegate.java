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

package com.intellij.refactoring.move;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author yole
 */
public abstract class MoveHandlerDelegate {
  public static final ExtensionPointName<MoveHandlerDelegate> EP_NAME = ExtensionPointName.create("com.intellij.refactoring.moveHandler");

  public boolean canMove(PsiElement[] elements, @Nullable final PsiElement targetContainer) {
    return isValidTarget(targetContainer, elements);
  }

  public boolean canMove(DataContext dataContext){
    return false;
  }

  public boolean isValidTarget(@Nullable final PsiElement targetElement, PsiElement[] sources) {
    return false;
  }

  public void doMove(final Project project, final PsiElement[] elements,
                     @Nullable final PsiElement targetContainer, @Nullable final MoveCallback callback) {
  }

  public PsiElement adjustTargetForMove(DataContext dataContext, PsiElement targetContainer) {
    return targetContainer;
  }

  @Nullable
  public PsiElement[] adjustForMove(Project project, PsiElement[] sourceElements, PsiElement targetElement) {
    return sourceElements;
  }

  /**
   * @return true if the delegate is able to move an element
   */
  public boolean tryToMove(final PsiElement element, final Project project, final DataContext dataContext,
                           @Nullable final PsiReference reference, final Editor editor) {
    return false;
  }

  public void collectFilesOrDirsFromContext(DataContext dataContext, Set<PsiElement> filesOrDirs){
  }

  public boolean isMoveRedundant(PsiElement source, PsiElement target) {
    return false;
  }
}
