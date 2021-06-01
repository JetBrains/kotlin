/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIdentifier
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

sealed class AbstractFirKotlinUClass(
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UClass, UAnchorOwner {
    override val uAnnotations: List<UAnnotation>
        get() {
            // TODO: Not yet implemented
            return emptyList()
        }

    override val uastDeclarations: List<UDeclaration> by lz {
        mutableListOf<UDeclaration>().apply {
            addAll(fields)
            addAll(initializers)
            addAll(methods)
            addAll(innerClasses)
        }
    }

    abstract val ktClass: KtClassOrObject?

    override val uastSuperTypes: List<UTypeReferenceExpression>
        get() = ktClass?.superTypeListEntries.orEmpty().mapNotNull { it.typeReference }.map {
            KotlinUTypeReferenceExpression(it, this)
        }

    // TODO: delegateExpressions

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitClass(this)) return
        // TODO: delegate expressions
        uAnnotations.acceptList(visitor)
        uastDeclarations.acceptList(visitor)
        visitor.afterVisitClass(this)
    }

    companion object {
        fun create(psi: KtLightClass, givenParent: UElement?): UClass {
            return when (psi) {
                // TODO: PsiAnonymousClass
                // TODO: Script
                else ->
                    FirKotlinUClass(psi, givenParent)
            }
        }
    }
}

class FirKotlinUClass(
    override val javaPsi: KtLightClass,
    givenParent: UElement?,
) : AbstractFirKotlinUClass(givenParent), PsiClass by javaPsi {
    override val ktClass: KtClassOrObject? = javaPsi.kotlinOrigin

    override val psi = unwrap<UClass, PsiClass>(javaPsi)

    override fun getSourceElement() = sourcePsi

    override fun getOriginalElement(): PsiElement? = sourcePsi?.originalElement

    override fun getNameIdentifier(): PsiIdentifier = UastLightIdentifier(psi, ktClass)

    override fun getContainingFile(): PsiFile = unwrapFakeFileForLightClass(psi.containingFile)

    override val uastAnchor: UIdentifier? by lz {
        getIdentifierSourcePsi()?.let {
            KotlinUIdentifier(nameIdentifier, it, this)
        }
    }

    private fun getIdentifierSourcePsi(): PsiElement? {
        ktClass?.nameIdentifier?.let { return it }
        (ktClass as? KtObjectDeclaration)?.getObjectKeyword()?.let { return it }
        return null
    }

    override fun getInnerClasses(): Array<UClass> {
        // TODO: Not yet implemented
        return super.getInnerClasses()
    }

    override fun getSuperClass(): UClass? {
        return super.getSuperClass()
    }

    override fun getFields(): Array<UField> {
        // TODO: Not yet implemented
        return super.getFields()
    }

    override fun getInitializers(): Array<UClassInitializer> {
        // TODO: why not just emptyList()? Kotlin class won't have <clinit>?
        return super.getInitializers()
    }

    override fun getMethods(): Array<UMethod> {
        // TODO: Not yet implemented
        return super.getMethods()
    }
}

// TODO: FirKotlinUAnonymousClass or FirKotlinUAnonymousObject ?

// TODO: FirKotlinScriptUClass

// TODO: FirKotlinInvalidUClass
