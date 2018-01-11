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

import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter

class KotlinUCatchClause(
        override val psi: KtCatchClause,
        givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UCatchClause {
    override val body by lz { KotlinConverter.convertOrEmpty(psi.catchBody, this) }
    
    override val parameters by lz {
        val parameter = psi.catchParameter ?: return@lz emptyList<UParameter>()
        listOf(KotlinUParameter(UastKotlinPsiParameter.create(parameter, psi, this, 0), this))
    }

    override val typeReferences by lz {
        val parameter = psi.catchParameter ?: return@lz emptyList<UTypeReferenceExpression>()
        val typeReference = parameter.typeReference ?: return@lz emptyList<UTypeReferenceExpression>()
        listOf(LazyKotlinUTypeReferenceExpression(typeReference, this) { typeReference.toPsiType(this, boxed = true) } )
    }
}