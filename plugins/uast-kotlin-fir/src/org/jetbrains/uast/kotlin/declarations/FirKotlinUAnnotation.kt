/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.declarations

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinAbstractUElement

class FirKotlinUAnnotation(
    annotationEntry: KtAnnotationEntry,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UAnnotation, UAnchorOwner, UMultiResolvable {
    override val javaPsi = annotationEntry.toLightAnnotation()

    override val psi: PsiElement = annotationEntry

    override val attributeValues: List<UNamedExpression>
        get() = listOf() // TODO("Not yet implemented")

    override val qualifiedName: String?
        get() = "not-implemented-annotation"

    override val uastAnchor: UIdentifier?
        get() = TODO("Not yet implemented")

    override fun findAttributeValue(name: String?): UExpression? {
        // TODO("Not yet implemented")
        return null
    }

    override fun findDeclaredAttributeValue(name: String?): UExpression? {
        TODO("Not yet implemented")
    }

    override fun resolve(): PsiClass? {
        TODO("Not yet implemented")
    }

    override fun multiResolve(): Iterable<ResolveResult> {
        TODO("Not yet implemented")
    }
}
