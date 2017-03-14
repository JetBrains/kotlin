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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression

class KotlinULiteralExpression(
        override val psi: KtConstantExpression,
        override val uastParent: UElement?
) : KotlinAbstractUExpression(), ULiteralExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val isNull: Boolean
        get() = psi.unwrapBlockOrParenthesis().node?.elementType == KtNodeTypes.NULL

    override val value by lz { evaluate() }
}

class KotlinStringULiteralExpression(
        override val psi: PsiElement,
        override val uastParent: UElement?,
        val text: String
) : KotlinAbstractUExpression(), ULiteralExpression, KotlinUElementWithType{
    constructor(psi: PsiElement, uastParent: UElement?)
            : this(psi, uastParent, if (psi is KtEscapeStringTemplateEntry) psi.unescapedValue else psi.text)

    override val value: String
        get() = text

    override fun evaluate() = value
}