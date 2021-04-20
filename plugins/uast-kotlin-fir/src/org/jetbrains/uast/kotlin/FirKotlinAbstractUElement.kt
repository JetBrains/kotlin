/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.internal.FirKotlinUElementWithComments

abstract class FirKotlinAbstractUElement(
    private val givenParent: UElement?
) : FirKotlinUElementWithComments {

    final override val uastParent: UElement? by lz {
        givenParent ?: convertParent()
    }

    protected open fun convertParent(): UElement? {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UElement) {
            return false
        }

        return this.sourcePsi == other.sourcePsi
    }

    override fun hashCode(): Int {
        return sourcePsi?.hashCode() ?: 0
    }
}

abstract class FirKotlinAbstractUExpression(
    givenParent: UElement?
) : FirKotlinAbstractUElement(givenParent), UExpression {
    override val javaPsi: PsiElement? = null

    override val psi: PsiElement?
        get() = sourcePsi

    // TODO: annotations
}
