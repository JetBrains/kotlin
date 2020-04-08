/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.goto

import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch

class KotlinExpectOrActualGotoRelatedProvider : GotoRelatedProvider() {
    private class ActualOrExpectGotoRelatedItem(element: PsiElement) : GotoRelatedItem(element) {
        override fun getCustomContainerName(): String? {
            val module = element?.module ?: return null
            return "(in module ${module.name})"
        }
    }

    override fun getItems(psiElement: PsiElement): List<GotoRelatedItem> {
        val declaration = psiElement.getParentOfTypeAndBranch<KtNamedDeclaration> { nameIdentifier } ?: return emptyList()
        val targets = when {
            declaration.isExpectDeclaration() -> declaration.actualsForExpected()
            declaration.isEffectivelyActual() -> listOfNotNull(declaration.expectedDeclarationIfAny())
            else -> emptyList()
        }
        return targets.map(::ActualOrExpectGotoRelatedItem)
    }
}