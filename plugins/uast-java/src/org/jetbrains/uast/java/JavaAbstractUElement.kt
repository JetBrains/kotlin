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

package org.jetbrains.uast.java

import com.intellij.psi.*
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.java.internal.JavaUElementWithComments
import org.jetbrains.uast.psi.PsiElementBacked

abstract class JavaAbstractUElement : JavaUElementWithComments {
    private val psiElement: PsiElement?
        get() = (this as? PsiElementBacked)?.psi

    override fun equals(other: Any?): Boolean {
        if (this !is PsiElementBacked || other !is PsiElementBacked) {
            return this === other
        }

        if (other.javaClass != this.javaClass) return false
        return this.psi == other.psi
    }

    override fun asSourceString(): String {
        if (this is PsiElementBacked) {
            return this.psi?.text ?: super<JavaUElementWithComments>.asSourceString()
        }
        return super<JavaUElementWithComments>.asSourceString()
    }

    override fun toString() = asRenderString()
}

abstract class JavaAbstractUExpression : JavaAbstractUElement(), UExpression {
    override fun evaluate(): Any? {
        val psi = (this as? PsiElementBacked)?.psi ?: return null
        return JavaPsiFacade.getInstance(psi.project).constantEvaluationHelper.computeConstantExpression(psi)
    }
    
    override fun getExpressionType(): PsiType? {
        val expression = (this as? PsiElementBacked)?.psi as? PsiExpression ?: return null
        return expression.type
    }
}