/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.psi.KtElement

class KotlinToJavaInlineHandler : AbstractCrossLanguageInlineHandler() {
    override fun prepareReference(reference: PsiReference, referenced: PsiElement): MultiMap<PsiElement, String> {
        if (referenced is KtElement) {
            KotlinInlineRefactoringFUSCollector.log(
                elementFrom = referenced,
                languageTo = reference.element.language,
                isCrossLanguage = true
            )
        }

        return super.prepareReference(reference, referenced)
    }
}
