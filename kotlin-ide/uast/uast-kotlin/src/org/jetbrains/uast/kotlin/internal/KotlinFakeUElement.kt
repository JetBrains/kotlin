/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.psi.PsiElement
import org.jetbrains.uast.UElement

interface KotlinFakeUElement {
    fun unwrapToSourcePsi(): List<PsiElement>
}

fun UElement.toSourcePsiFakeAware(): List<PsiElement> {
    if (this is KotlinFakeUElement) return this.unwrapToSourcePsi()
    return listOfNotNull(this.sourcePsi)
}