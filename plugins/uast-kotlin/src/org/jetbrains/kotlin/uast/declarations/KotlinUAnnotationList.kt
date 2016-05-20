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

import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.acceptList
import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

class KotlinUAnnotationList(
        override val psi: KtAnnotation,
        override val parent: UElement
) : KotlinAbstractUElement(), UElement, PsiElementBacked {
    lateinit var annotations: List<UAnnotation>

    override fun logString() = "KotlinUAnnotationList"
    override fun renderString() = annotations.joinToString(" ") { it.renderString() }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitElement(this)) return
        annotations.acceptList(visitor)
        visitor.afterVisitElement(this)
    }
}