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
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.stream.Stream;

public final class MethodChainsSearchUtil {
  private MethodChainsSearchUtil() {
  }

  @Nullable
  public static PsiMethod getMethodWithMinNotPrimitiveParameters(PsiMethod @NotNull [] methods,
                                                                 @NotNull PsiClass target) {
    return Stream.of(methods)
      .filter(m -> {
        for (PsiParameter parameter : m.getParameterList().getParameters()) {
          PsiType t = parameter.getType();
          PsiClass aClass = PsiUtil.resolveClassInClassTypeOnly(t);
          if (aClass == target) {
            return false;
          }
        }
        return true;
      }).min(Comparator.comparing(MethodChainsSearchUtil::getNonPrimitiveParameterCount)).orElse(null);
  }

  private static int getNonPrimitiveParameterCount(PsiMethod method) {
    return (int)Stream.of(method.getParameterList().getParameters())
      .map(p -> p.getType())
      .filter(t -> !TypeConversionUtil.isPrimitiveAndNotNull(t))
      .count();
  }
}
