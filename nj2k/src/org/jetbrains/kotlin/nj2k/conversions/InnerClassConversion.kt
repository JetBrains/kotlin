/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.isLocalClass
import org.jetbrains.kotlin.nj2k.tree.JKClass
import org.jetbrains.kotlin.nj2k.tree.JKOtherModifierElement
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.OtherModifier

class InnerClassConversion(context: NewJ2kConverterContext) : StatefulRecursiveApplicableConversionBase<JKClass>(context) {
    override fun applyToElement(element: JKTreeElement, state: JKClass?): JKTreeElement {
        if (element !is JKClass) return recurse(element, state)

        if (element.classKind == JKClass.ClassKind.COMPANION) return recurse(element, state)
        if (element.isLocalClass()) return recurse(element, state)

        val static = element.otherModifierElements.find { it.otherModifier == OtherModifier.STATIC }
        when {
            static != null -> element.otherModifierElements -= static
            state != null
                    && element.classKind != JKClass.ClassKind.INTERFACE
                    && state.classKind != JKClass.ClassKind.INTERFACE
                    && element.classKind != JKClass.ClassKind.ENUM
            -> element.otherModifierElements += JKOtherModifierElement(OtherModifier.INNER)
        }
        return recurse(element, element)
    }
}