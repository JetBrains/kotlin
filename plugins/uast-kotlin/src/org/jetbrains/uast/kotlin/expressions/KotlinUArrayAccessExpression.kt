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

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.uast.UElement

class KotlinUArrayAccessExpression(
    override val sourcePsi: KtArrayAccessExpression,
    givenParent: UElement?
) : KotlinAbstractUArrayAccessExpression(sourcePsi, givenParent) {

    override fun getExpressionType(): PsiType? {
        super.getExpressionType()?.let { return it }

        // for unknown reason in assignment position there is no `EXPRESSION_TYPE_INFO` so we getting it from the array type
        val arrayExpression = sourcePsi.arrayExpression ?: return null
        val arrayType = arrayExpression.analyze()[BindingContext.EXPRESSION_TYPE_INFO, arrayExpression]?.type ?: return null
        return arrayType.arguments.firstOrNull()?.type?.toPsiType(this, arrayExpression, false )
    }
}
