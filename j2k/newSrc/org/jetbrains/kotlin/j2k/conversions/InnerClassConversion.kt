/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*

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

        val static = element.extraModifiers.find { it == ExtraModifier.STATIC }
        if (static != null) {
            element.extraModifiers -= static
        } else if (element.classKind != JKClass.ClassKind.INTERFACE &&
            outer.classKind != JKClass.ClassKind.INTERFACE &&
            element.classKind != JKClass.ClassKind.ENUM
        ) {
            element.extraModifiers += ExtraModifier.INNER
        }
        return recurseArmed(element, element)
    }
}