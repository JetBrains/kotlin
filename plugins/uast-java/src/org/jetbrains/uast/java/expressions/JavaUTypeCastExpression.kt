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

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastBinaryExpressionWithTypeKind
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUTypeCastExpression(
        override val psi: PsiTypeCastExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UBinaryExpressionWithType, PsiElementBacked {
    override val operand by lz { JavaConverter.convertOrEmpty(psi.operand, this) }
    
    override val type: PsiType
        get() = psi.castType?.type ?: UastErrorType
    
    override val typeReference by lz { psi.castType?.let { JavaUTypeReferenceExpression(it, this) } }
    
    override val operationKind: UastBinaryExpressionWithTypeKind.TypeCast
        get() = UastBinaryExpressionWithTypeKind.TYPE_CAST
}