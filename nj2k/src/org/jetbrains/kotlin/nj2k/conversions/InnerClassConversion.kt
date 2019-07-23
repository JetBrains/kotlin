/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.OtherModifier
import org.jetbrains.kotlin.nj2k.tree.JKClass
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.impl.JKOtherModifierElementImpl
import org.jetbrains.kotlin.nj2k.tree.isLocalClass

class InnerClassConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        return recurseArmed(element, element)
    }

    private fun recurseArmed(element: JKTreeElement, outer: JKClass): JKTreeElement {
        return applyRecursive(element, outer, ::applyArmed)
    }

    private fun applyArmed(element: JKTreeElement, outer: JKClass): JKTreeElement {
        if (element !is JKClass) return recurseArmed(element, outer)
        if (element.classKind == JKClass.ClassKind.COMPANION) return recurseArmed(element, outer)
        if (element.isLocalClass()) return recurseArmed(element, outer)

        val static = element.otherModifierElements.find { it.otherModifier == OtherModifier.STATIC }
        if (static != null) {
            element.otherModifierElements -= static
        } else if (element.classKind != JKClass.ClassKind.INTERFACE &&
            outer.classKind != JKClass.ClassKind.INTERFACE &&
            element.classKind != JKClass.ClassKind.ENUM
        ) {
            element.otherModifierElements += JKOtherModifierElementImpl(OtherModifier.INNER)
        }
        return recurseArmed(element, element)
    }
}