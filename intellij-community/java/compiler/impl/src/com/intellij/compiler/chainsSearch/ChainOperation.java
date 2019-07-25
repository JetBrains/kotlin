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
package com.intellij.compiler.chainsSearch;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public interface ChainOperation {
  class TypeCast implements ChainOperation {
    // we cast only to a class
    private final PsiClass myOperandClass;
    @NotNull private final PsiClass myCastClass;

    public TypeCast(@NotNull PsiClass operandClass, @NotNull PsiClass castClass) {
      myOperandClass= operandClass;
      myCastClass = castClass;
    }

    @NotNull
    public PsiClass getCastClass() {
      return myCastClass;
    }

    @Override
    public String toString() {
      return "cast of " + myOperandClass.getName();
    }
  }

  class MethodCall implements ChainOperation {
    @NotNull
    private final PsiMethod[] myCandidates;

    public MethodCall(@NotNull PsiMethod[] candidates) {
      if (candidates.length == 0) {
        throw new IllegalStateException();
      }
      myCandidates = candidates;
    }

    @NotNull
    public PsiMethod[] getCandidates() {
      return myCandidates;
    }

    @Override
    public String toString() {
      return myCandidates[0].getName() + "()";
    }
  }
}
