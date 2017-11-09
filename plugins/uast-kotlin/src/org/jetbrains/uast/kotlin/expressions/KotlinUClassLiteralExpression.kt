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

import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

class KotlinUClassLiteralExpression(
        override val psi: KtClassLiteralExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UClassLiteralExpression, KotlinUElementWithType {
    override val type by lz {
        val ktType = psi.analyze()[DOUBLE_COLON_LHS, psi.receiverExpression]?.type ?: return@lz null
        ktType.toPsiType(this, psi, boxed = true)
    }
    
    override val expression: UExpression?
        get() {
            if (type != null) return null
            val receiverExpression = psi.receiverExpression ?: return null
            return KotlinConverter.convertExpression(receiverExpression, this)
        }
}