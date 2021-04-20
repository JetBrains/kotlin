/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

sealed class AbstractFirKotlinUVariable(
    givenParent: UElement?
) : FirKotlinAbstractUElement(givenParent), PsiVariable, UVariableEx, UAnchorOwner {
    override val uAnnotations: List<UAnnotation>
        get() {
            // TODO: Not yet implemented
            return emptyList()
        }

    override fun getNameIdentifier(): PsiIdentifier {
        val kotlinOrigin = (sourcePsi as? KtLightElement<*, *>)?.kotlinOrigin
        return FirUastLightIdentifier(psi, kotlinOrigin as? KtDeclaration)
    }

    override fun getContainingFile(): PsiFile = unwrapFakeFileForLightClass(psi.containingFile)

    override val uastAnchor: UIdentifier? by lz {
        val identifierSourcePsi = when (val sourcePsi = sourcePsi) {
            is KtNamedDeclaration -> sourcePsi.nameIdentifier
            is KtTypeReference -> sourcePsi.typeElement?.let {
                // receiver param in extension function
                (it as? KtUserType)?.referenceExpression?.getIdentifier() ?: it
            } ?: sourcePsi
            is KtNameReferenceExpression -> sourcePsi.getReferencedNameElement()
            is KtBinaryExpression, is KtCallExpression -> null // e.g. `foo("Lorem ipsum") ?: foo("dolor sit amet")`
            is KtDestructuringDeclaration -> sourcePsi.valOrVarKeyword
            is KtLambdaExpression -> sourcePsi.functionLiteral.lBrace
            else -> sourcePsi
        } ?: return@lz null
        FirKotlinUIdentifier(nameIdentifier, identifierSourcePsi, this)
    }

    override val typeReference: UTypeReferenceExpression? by lz {
        (sourcePsi as? KtCallableDeclaration)?.typeReference?.let {
            FirKotlinUTypeReferenceExpression(it, this) { type }
        }
    }

    private val kotlinOrigin by lz { getKotlinMemberOrigin(psi.originalElement) ?: sourcePsi }

    override val uastInitializer: UExpression? by lz {
        val initializerExpression = when (val kotlinOrigin = kotlinOrigin) {
            is KtProperty ->
                kotlinOrigin.initializer
            is KtParameter ->
                kotlinOrigin.defaultValue
            is KtVariableDeclaration ->
                kotlinOrigin.initializer
            else -> null
        } ?: return@lz null

        UastFacade.findPlugin(this)?.convertElement(initializerExpression, this) as? UExpression
    }

    // TODO: delegateExpression
}

open class FirKotlinUParameter(
    psi: PsiParameter,
    override val sourcePsi: KtElement?,
    givenParent: UElement?
) : AbstractFirKotlinUVariable(givenParent), UParameterEx, PsiParameter by psi {
    override val psi = unwrap<UParameter, PsiParameter>(psi)

    override val javaPsi: PsiParameter = this.psi

    override fun getInitializer(): PsiExpression? {
        return super<AbstractFirKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractFirKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }
}

class FirKotlinReceiverUParameter(
    psi: PsiParameter,
    private val receiver: KtTypeReference,
    givenParent: UElement?
) : FirKotlinUParameter(psi, receiver, givenParent) {
    override val uAnnotations: List<UAnnotation>
        get() {
            // TODO: Not yet implemented: take annotations on receiver with RECEIVER use site target
            return emptyList()
        }
}

class FirKotlinUField(
    psi: PsiField,
    override val sourcePsi: KtElement?,
    givenParent: UElement?
) : AbstractFirKotlinUVariable(givenParent), UFieldEx, PsiField by psi {
    override fun getSourceElement(): PsiElement {
        return sourcePsi ?: this
    }

    override val psi = unwrap<UField, PsiField>(psi)

    override val javaPsi: PsiField = this.psi

    override fun getInitializer(): PsiExpression? {
        return super<AbstractFirKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractFirKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitField(this)) return
        uAnnotations.acceptList(visitor)
        uastInitializer?.accept(visitor)
        // TODO: delegateExpression
        visitor.afterVisitField(this)
    }
}

// TODO: FirKotlinU(Annotated)LocalVariable

// TODO: FirKotlinUEnumConstant
