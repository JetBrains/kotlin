/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiAnonymousClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.uast.*

class FirKotlinUClass(
    psi: KtLightClass,
    givenParent: UElement?,
) : BaseKotlinUClass(psi, givenParent) {
    override fun buildPrimaryConstructorUMethod(ktClass: KtClassOrObject?, psi: KtLightMethod, givenParent: UElement?): UMethod {
        return FirKotlinConstructorUMethod(ktClass, psi, givenParent)
    }

    override fun buildSecondaryConstructorUMethod(ktClass: KtClassOrObject?, psi: KtLightMethod, givenParent: UElement?): UMethod {
        return FirKotlinSecondaryConstructorWithInitializersUMethod(ktClass, psi, givenParent)
    }

    companion object {
        fun create(psi: KtLightClass, givenParent: UElement?): UClass {
            return when (psi) {
                is PsiAnonymousClass -> KotlinUAnonymousClass(psi, givenParent)
                // TODO: Script
                else ->
                    FirKotlinUClass(psi, givenParent)
            }
        }
    }
}

// TODO: FirKotlinScriptUClass
