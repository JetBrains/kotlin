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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import java.util.HashSet

public open class SuperVisitor() : JavaRecursiveElementVisitor() {
    public val resolvedSuperCallParameters: HashSet<PsiExpressionList> = hashSet()

    public override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
        if (expression != null && isSuper(expression.getMethodExpression())) {
            resolvedSuperCallParameters.add(expression.getArgumentList())
        }
    }
    class object {
        open fun isSuper(r: PsiReference): Boolean {
            if (r.getCanonicalText().equals("super")) {
                val baseConstructor: PsiElement? = r.resolve()
                if (baseConstructor != null && baseConstructor is PsiMethod && baseConstructor.isConstructor()) {
                    return true
                }
            }

            return false
        }
    }
}
