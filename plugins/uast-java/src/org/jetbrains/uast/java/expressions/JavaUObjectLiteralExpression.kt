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

import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUObjectLiteralExpression(
        override val psi: PsiNewExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UObjectLiteralExpression, PsiElementBacked {
    override val declaration by lz { JavaUClass.create(psi.anonymousClass!!, this) }

    override val classReference by lz {
        psi.classReference?.let { ref ->
            JavaClassUSimpleNameReferenceExpression(ref.element?.text.orAnonymous(), ref, ref.element, this)
        }
    }

    override val valueArgumentCount: Int
        get() = psi.argumentList?.expressions?.size ?: 0

    override val valueArguments by lz {
        psi.argumentList?.expressions?.map { JavaConverter.convertExpression(it, this) } ?: emptyList()
    }

    override val typeArgumentCount by lz { psi.classReference?.typeParameters?.size ?: 0 }

    override val typeArguments: List<PsiType>
        get() = psi.classReference?.typeParameters?.toList() ?: emptyList()

    override fun resolve() = psi.resolveMethod()
}