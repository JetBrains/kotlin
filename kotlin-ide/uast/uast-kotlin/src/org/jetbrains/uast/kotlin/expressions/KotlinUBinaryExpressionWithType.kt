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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.uast.*

class KotlinUBinaryExpressionWithType(
        override val sourcePsi: KtBinaryExpressionWithTypeRHS,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UBinaryExpressionWithType,
        KotlinUElementWithType, KotlinEvaluatableUElement {
    
    override val operand by lz { KotlinConverter.convertOrEmpty(sourcePsi.left, this) }
    override val type by lz { sourcePsi.right.toPsiType(this) }
    
    override val typeReference by lz {
        sourcePsi.right?.let { LazyKotlinUTypeReferenceExpression(it, this) }
    }
    
    override val operationKind = when (sourcePsi.operationReference.getReferencedNameElementType()) {
        KtTokens.AS_KEYWORD -> UastBinaryExpressionWithTypeKind.TYPE_CAST
        KtTokens.AS_SAFE -> KotlinBinaryExpressionWithTypeKinds.SAFE_TYPE_CAST
        else -> UastBinaryExpressionWithTypeKind.UNKNOWN
    }
}

class KotlinCustomUBinaryExpressionWithType(
        override val psi: PsiElement,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UBinaryExpressionWithType {
    lateinit override var operand: UExpression
        internal set

    lateinit override var operationKind: UastBinaryExpressionWithTypeKind
        internal set

    override val type: PsiType by lz { typeReference?.type ?: UastErrorType }

    override var typeReference: UTypeReferenceExpression? = null
        internal set
}