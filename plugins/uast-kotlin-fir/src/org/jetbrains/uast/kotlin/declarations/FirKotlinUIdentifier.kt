/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElement

class FirUastLightIdentifier(
    lightOwner: PsiNameIdentifierOwner,
    ktDeclaration: KtDeclaration?
) : KtLightIdentifier(lightOwner, ktDeclaration) {
    override fun getContainingFile(): PsiFile {
        return unwrapFakeFileForLightClass(super.getContainingFile())
    }
}

class FirKotlinUIdentifier(
    override val javaPsi: PsiElement? = null,
    override val sourcePsi: PsiElement?,
    givenParent: UElement?
) : UIdentifier(sourcePsi, givenParent) {
    override val psi: PsiElement?
        get() = javaPsi ?: sourcePsi

    override val uastParent: UElement? by lz {
        if (givenParent != null) return@lz givenParent
        val parent = sourcePsi?.parent ?: return@lz null
        getIdentifierParentForCall(parent) ?: parent.toUElement()
    }

    private fun getIdentifierParentForCall(parent: PsiElement): UElement? {
        val parentParent = parent.parent
        if (parentParent is KtCallElement && parentParent.calleeExpression == parent) { // method identifiers in calls
            return parentParent.toUElement()
        }
        if (parentParent is KtTypeReference && parentParent.parent is KtConstructorCalleeExpression ) {
            return parentParent.parent.toUElement()
        }
        return null
    }

    constructor(sourcePsi: PsiElement?, uastParent: UElement?) : this(null, sourcePsi, uastParent)
}
