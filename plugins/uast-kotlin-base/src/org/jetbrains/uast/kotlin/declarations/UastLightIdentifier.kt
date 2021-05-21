/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.psi.KtDeclaration

class UastLightIdentifier(
    lightOwner: PsiNameIdentifierOwner,
    ktDeclaration: KtDeclaration?
) : KtLightIdentifier(lightOwner, ktDeclaration) {
    override fun getContainingFile(): PsiFile {
        return unwrapFakeFileForLightClass(super.getContainingFile())
    }
}
