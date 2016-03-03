/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UType
import org.jetbrains.uast.psi.PsiElementBacked

interface KotlinTypeHelper : UExpression, PsiElementBacked {
    override fun getExpressionType(): UType? {
        val ktElement = psi as? KtExpression ?: return null
        val ktType = ktElement.analyze(BodyResolveMode.PARTIAL)[BindingContext.EXPECTED_EXPRESSION_TYPE, ktElement] ?: return null
        return KotlinConverter.convert(ktType, ktElement.project, null)
    }
}

interface KotlinEvaluateHelper : UExpression, PsiElementBacked {
    override fun evaluate(): Any? {
        val ktElement = psi as? KtExpression ?: return null
        val compileTimeConst = ktElement.analyze(BodyResolveMode.PARTIAL)[BindingContext.COMPILE_TIME_VALUE, ktElement]
        return compileTimeConst?.getValue(TypeUtils.NO_EXPECTED_TYPE)
    }
}