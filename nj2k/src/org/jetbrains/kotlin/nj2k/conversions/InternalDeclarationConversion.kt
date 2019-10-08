/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*

class InternalDeclarationConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKVisibilityOwner || element !is JKModalityOwner) return recurse(element)
        val containingClass = element.parentOfType<JKClass>()

        if (element is JKClass && element.isLocalClass()) {
            element.visibility = Visibility.PUBLIC
        }

        val containingClassVisibility =
            containingClass?.visibility
                ?: element.psi<PsiMember>()
                    ?.containingClass
                    ?.visibility(context.converter.oldConverterServices.referenceSearcher, null)
                    ?.visibility

        val containingClassKind =
            containingClass?.classKind
                ?: element.psi<PsiMember>()
                    ?.containingClass
                    ?.classKind

        if (containingClassVisibility == Visibility.INTERNAL
            && element.visibility == Visibility.INTERNAL
            && element.modality == Modality.FINAL
            && (element is JKMethod || element is JKField)
        ) {
            element.visibility = Visibility.PUBLIC
        }

        if (containingClassKind == JKClass.ClassKind.INTERFACE
            || containingClassKind == JKClass.ClassKind.ANNOTATION
        ) {
            element.visibility = Visibility.PUBLIC
        }

        if (containingClassKind == JKClass.ClassKind.ENUM
            && element is JKKtPrimaryConstructor
        ) {
            element.visibility = Visibility.PRIVATE
        }
        return recurse(element)
    }
}