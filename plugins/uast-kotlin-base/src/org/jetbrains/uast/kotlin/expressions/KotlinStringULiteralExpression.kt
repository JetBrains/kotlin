/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.kotlin.internal.KotlinFakeUElement
import org.jetbrains.uast.wrapULiteral

class KotlinStringULiteralExpression(
    override val sourcePsi: PsiElement,
    givenParent: UElement?,
    val text: String
) : KotlinAbstractUExpression(givenParent), ULiteralExpression, KotlinUElementWithType, KotlinFakeUElement {
    constructor(psi: PsiElement, uastParent: UElement?)
            : this(psi, uastParent, if (psi is KtEscapeStringTemplateEntry) psi.unescapedValue else psi.text)

    override val value: String
        get() = text

    override fun evaluate() = value

    override fun getExpressionType(): PsiType = PsiType.getJavaLangString(sourcePsi.manager, sourcePsi.resolveScope)

    override fun unwrapToSourcePsi(): List<PsiElement> = listOfNotNull(wrapULiteral(this).sourcePsi)
}
