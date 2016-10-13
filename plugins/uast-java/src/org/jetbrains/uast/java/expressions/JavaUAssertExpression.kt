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

import com.intellij.psi.PsiAssertStatement
import com.intellij.psi.PsiType
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.psi.PsiElementBacked


class JavaUAssertExpression(
        override val psi: PsiAssertStatement,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UCallExpression, PsiElementBacked {
    val condition: UExpression by lz { JavaConverter.convertOrEmpty(psi.assertCondition, this) }
    val message: UExpression? by lz { JavaConverter.convertOrNull(psi.assertDescription, this) }
    
    override val methodIdentifier: UIdentifier?
        get() = null

    override val classReference: UReferenceExpression?
        get() = null

    override val methodName: String
        get() = "assert"

    override val receiver: UExpression?
        get() = null

    override val receiverType: PsiType?
        get() = null
    
    override val valueArgumentCount: Int
        get() = if (message != null) 2 else 1

    override val valueArguments by lz {
        val message = this.message
        if (message != null) listOf(condition, message) else listOf(condition)
    }

    override val typeArgumentCount: Int
        get() = 0
    
    override val typeArguments: List<PsiType>
        get() = emptyList()

    override val returnType: PsiType
        get() = PsiType.VOID
    
    override val kind: UastCallKind
        get() = JavaUastCallKinds.ASSERT

    override fun resolve() = null
}