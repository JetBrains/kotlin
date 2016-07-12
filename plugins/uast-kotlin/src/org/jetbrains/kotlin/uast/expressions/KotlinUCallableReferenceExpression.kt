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

import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUCallableReferenceExpression(
        override val psi: KtCallableReferenceExpression,
        override val parent: UElement
) : KotlinAbstractUElement(), UCallableReferenceExpression, PsiElementBacked, KotlinUElementWithType {
    override val qualifierExpression by lz { KotlinConverter.convertOrEmpty(psi.receiverExpression, this) }
    override val qualifierType: UType? get() = null // TODO
    override val callableName: String
        get() = psi.callableReference.getReferencedName()

    override fun resolve(context: UastContext): UDeclaration? {
        throw UnsupportedOperationException()
    }
}
