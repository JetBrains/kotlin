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

package org.jetbrains.uast.java

import com.intellij.psi.PsiClassInitializer
import org.jetbrains.uast.*
import org.jetbrains.uast.java.internal.JavaUElementWithComments

class JavaUClassInitializer(
        psi: PsiClassInitializer,
        override val containingElement: UElement?
) : UClassInitializer, JavaUElementWithComments, PsiClassInitializer by psi {
    override val psi = unwrap<UClassInitializer, PsiClassInitializer>(psi)

    override val uastAnchor: UElement?
        get() = null
    
    override val uastBody by lz {
        getLanguagePlugin().convertElement(psi.body, this, null) as? UExpression ?: UastEmptyExpression
    }

    override val annotations by lz { psi.annotations.map { JavaUAnnotation(it, this) } }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()
}