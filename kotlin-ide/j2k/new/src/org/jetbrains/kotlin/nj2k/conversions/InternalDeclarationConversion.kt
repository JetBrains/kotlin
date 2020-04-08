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
        if (element.visibility != Visibility.INTERNAL) return recurse(element)

        val containingClass = element.parentOfType<JKClass>()
        val containingClassKind = containingClass?.classKind ?: element.psi<PsiMember>()?.containingClass?.classKind?.toJk()

        val containingClassVisibility = containingClass?.visibility
            ?: element.psi<PsiMember>()
                ?.containingClass
                ?.visibility(context.converter.oldConverterServices.referenceSearcher, null)
                ?.visibility

        element.visibility = when {
            containingClassKind == JKClass.ClassKind.INTERFACE || containingClassKind == JKClass.ClassKind.ANNOTATION ->
                Visibility.PUBLIC
            containingClassKind == JKClass.ClassKind.ENUM && element is JKConstructor ->
                Visibility.PRIVATE
            element is JKClass && !element.isLocalClass() ->
                Visibility.INTERNAL
            element is JKConstructor && containingClassVisibility != Visibility.INTERNAL ->
                Visibility.INTERNAL
            element is JKField || element is JKMethod ->
                Visibility.PUBLIC
            else -> Visibility.INTERNAL
        }

        return recurse(element)
    }
}