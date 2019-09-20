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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface describes target of "Introduce" refactorings. It is used by the Target Chooser popup.
 * @see com.intellij.refactoring.IntroduceTargetChooser
 * @see IntroduceHandler
 */
public interface IntroduceTarget {
  /**
   * @return the range of the target in the file
   */
  @NotNull
  TextRange getTextRange();

  /**
   * @return PSI context of the target if applicable (e.g. if you introduce a part of a string literal you could return the string literal)
   */
  @Nullable
  PsiElement getPlace();

  /**
   * @return string presentation of the target to use in the Target Chooser
   */
  @NotNull
  String render();

  /**
   * @return true if the target is still valid (e.g. it might become invalid if the document was changed after the target had been created)
   */
  boolean isValid();
}
