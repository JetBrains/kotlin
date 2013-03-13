/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;


public class SuperVisitor extends JavaRecursiveElementVisitor {
    @NotNull
    private final HashSet<PsiExpressionList> myResolvedSuperCallParameters;

    public SuperVisitor() {
        myResolvedSuperCallParameters = new HashSet<PsiExpressionList>();
    }

    @NotNull
    public HashSet<PsiExpressionList> getResolvedSuperCallParameters() {
        return myResolvedSuperCallParameters;
    }

    @Override
    public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (isSuper(expression.getMethodExpression())) {
            myResolvedSuperCallParameters.add(expression.getArgumentList());
        }
    }

    static boolean isSuper(@NotNull PsiReference r) {
        if (r.getCanonicalText().equals("super")) {
            PsiElement baseConstructor = r.resolve();
            if (baseConstructor != null && baseConstructor instanceof PsiMethod && ((PsiMethod) baseConstructor).isConstructor()) {
                return true;
            }
        }
        return false;
    }
}
