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
package com.intellij.refactoring.introduce;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiIntroduceTarget<T extends PsiElement> implements IntroduceTarget {
  @NotNull protected final SmartPsiElementPointer<T> myPointer;

  public PsiIntroduceTarget(@NotNull T psi) {
    myPointer = SmartPointerManager.getInstance(psi.getProject()).createSmartPsiElementPointer(psi);
  }

  @NotNull
  @Override
  public TextRange getTextRange() {
    return getPlace().getTextRange();
  }

  @Nullable
  @Override
  public T getPlace() {
    return myPointer.getElement();
  }

  @NotNull
  @Override
  public String render() {
    return getPlace().getText();
  }

  @Override
  public boolean isValid() {
    return myPointer.getElement() != null;
  }
}
