/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.references

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtPsiFactory

interface SimpleNameReferenceExtension {
    companion object {
        val EP_NAME: ExtensionPointName<SimpleNameReferenceExtension> =
            ExtensionPointName.create("org.jetbrains.kotlin.simpleNameReferenceExtension")
    }

    fun isReferenceTo(reference: KtSimpleNameReference, element: PsiElement): Boolean

    fun handleElementRename(reference: KtSimpleNameReference, psiFactory: KtPsiFactory, newElementName: String): PsiElement?
}