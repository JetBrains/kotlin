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

import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringActionHandlerFactory;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.safeDelete.SafeDeleteHandler;

/**
 * @author dsl
 */
public class RefactoringActionHandlerFactoryImpl extends RefactoringActionHandlerFactory {

  @Override
  public RefactoringActionHandler createSafeDeleteHandler() {
    return new SafeDeleteHandler();
  }

  @Override
  public RefactoringActionHandler createMoveHandler() {
    return new MoveHandler();
  }

  @Override
  public RefactoringActionHandler createRenameHandler() {
    return new PsiElementRenameHandler();
  }
}
