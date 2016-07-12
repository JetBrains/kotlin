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

import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUTypeCheckExpression(
        override val psi: KtIsExpression,
        override val parent: UElement
) : KotlinAbstractUElement(), UBinaryExpressionWithType, PsiElementBacked, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val operand by lz { KotlinConverter.convert(psi.leftHandSide, this) }
    override val type by lz { KotlinConverter.convert(psi.typeReference, this) }
    override val typeReference by lz { psi.typeReference?.let { KotlinConverter.convertTypeReference(it, this) } }
    override val operationKind = KotlinBinaryExpressionWithTypeKinds.NEGATED_INSTANCE_CHECK
}