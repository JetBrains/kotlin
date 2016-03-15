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

import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUQualifiedExpression(
        override val psi: KtDotQualifiedExpression,
        override val parent: UElement
) : UQualifiedExpression, PsiElementBacked, KotlinTypeHelper, KotlinEvaluateHelper {
    override val receiver by lz { KotlinConverter.convertOrEmpty(psi.receiverExpression, this) }
    override val selector by lz { KotlinConverter.convertOrEmpty(psi.selectorExpression, this) }
    override val accessType = UastQualifiedExpressionAccessType.SIMPLE

    override fun resolve(context: UastContext) = psi.selectorExpression.resolveCallToUDeclaration(context)
}

class KotlinUComponentQualifiedExpression(
        override val psi: KtDestructuringDeclarationEntry,
        override val parent: UElement
) : UQualifiedExpression, PsiElementBacked, KotlinTypeHelper, KotlinEvaluateHelper {
    override lateinit var receiver: UExpression
        internal set

    override lateinit var selector: UExpression
        internal set

    override val accessType = UastQualifiedExpressionAccessType.SIMPLE
    override fun resolve(context: UastContext) = null
}