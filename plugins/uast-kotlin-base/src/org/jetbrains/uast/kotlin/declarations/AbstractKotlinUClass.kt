/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

abstract class AbstractKotlinUClass(
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UClass, UAnchorOwner {

    override val uastDeclarations by lz {
        mutableListOf<UDeclaration>().apply {
            addAll(fields)
            addAll(initializers)
            addAll(methods)
            addAll(innerClasses)
        }
    }

    open val ktClass: KtClassOrObject? get() = (psi as? KtLightClass)?.kotlinOrigin

    override val uastSuperTypes: List<UTypeReferenceExpression>
        get() = ktClass?.superTypeListEntries.orEmpty().mapNotNull { it.typeReference }.map {
            KotlinUTypeReferenceExpression(it, this)
        }

    val delegateExpressions: List<UExpression>
        get() = ktClass?.superTypeListEntries.orEmpty()
            .filterIsInstance<KtDelegatedSuperTypeEntry>()
            .map { KotlinSupertypeDelegationUExpression(it, this) }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitClass(this)) return
        delegateExpressions.acceptList(visitor)
        uAnnotations.acceptList(visitor)
        uastDeclarations.acceptList(visitor)
        visitor.afterVisitClass(this)
    }

    override val uAnnotations: List<UAnnotation> by lz {
        (sourcePsi as? KtModifierListOwner)?.annotationEntries.orEmpty().map {
            baseResolveProviderService.baseKotlinConverter.convertAnnotation(it, this)
        }
    }

    override fun equals(other: Any?) = other is AbstractKotlinUClass && psi == other.psi
    override fun hashCode() = psi.hashCode()
}
