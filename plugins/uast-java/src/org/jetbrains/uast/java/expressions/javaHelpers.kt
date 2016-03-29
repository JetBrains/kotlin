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

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.psi.PsiElementBacked

interface JavaEvaluatableUElement : UExpression, PsiElementBacked {
    override fun evaluate(): Any? {
        val psi = this.psi ?: return null
        return JavaPsiFacade.getInstance(psi.project).constantEvaluationHelper.computeConstantExpression(psi)
    }
}

interface JavaUElementWithType : UExpression, PsiElementBacked {
    override fun getExpressionType() = (psi as? PsiExpression)?.type?.let { JavaConverter.convert(it, this) }
}