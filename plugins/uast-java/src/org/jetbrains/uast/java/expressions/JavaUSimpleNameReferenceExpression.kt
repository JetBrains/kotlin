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

import com.intellij.psi.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.expressions.UTypeReferenceExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUSimpleNameReferenceExpression(
        override val psi: PsiElement?,
        override val identifier: String,
        override val containingElement: UElement?,
        val reference: PsiReference? = null 
) : JavaAbstractUExpression(), USimpleNameReferenceExpression, PsiElementBacked {
    override fun resolve() = (reference ?: psi as? PsiReference)?.resolve()
    override val resolvedName: String?
        get() = ((reference ?: psi as? PsiReference)?.resolve() as? PsiNamedElement)?.name
}

class JavaUTypeReferenceExpression(
        override val psi: PsiTypeElement,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UTypeReferenceExpression, PsiElementBacked {
    override val type: PsiType
        get() = psi.type
}

class JavaClassUSimpleNameReferenceExpression(
        override val identifier: String,
        val ref: PsiJavaReference,
        override val psi: PsiElement?,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), USimpleNameReferenceExpression, PsiElementBacked {
    override fun resolve() = ref.resolve()
    override val resolvedName: String?
        get() = (ref.resolve() as? PsiNamedElement)?.name
}