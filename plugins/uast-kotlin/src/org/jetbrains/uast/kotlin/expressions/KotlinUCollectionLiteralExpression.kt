/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.expressions

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinAbstractUExpression
import org.jetbrains.uast.kotlin.KotlinConverter
import org.jetbrains.uast.kotlin.KotlinUElementWithType

class KotlinUCollectionLiteralExpression(
    val sourcePsi: KtCollectionLiteralExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UCallExpression, KotlinUElementWithType {

    override val classReference: UReferenceExpression? get() = null

    override val kind: UastCallKind = UastCallKind.NESTED_ARRAY_INITIALIZER

    override val methodIdentifier: UIdentifier? by lazy { UIdentifier(sourcePsi.leftBracket, this) }

    override val methodName: String? get() = null

    override val receiver: UExpression? get() = null

    override val receiverType: PsiType? get() = null

    override val returnType: PsiType? get() = getExpressionType()

    override val typeArgumentCount: Int get() = typeArguments.size

    override val typeArguments: List<PsiType> get() = listOfNotNull((returnType as? PsiArrayType)?.componentType)

    override val valueArgumentCount: Int
        get() = sourcePsi.getInnerExpressions().size

    override val valueArguments by lazy {
        sourcePsi.getInnerExpressions().map { KotlinConverter.convertOrEmpty(it, this) }
    }

    override fun asRenderString(): String = "collectionLiteral[" + valueArguments.joinToString { it.asRenderString() } + "]"

    override fun resolve(): PsiMethod? = null

    override val psi: PsiElement get() = sourcePsi

}