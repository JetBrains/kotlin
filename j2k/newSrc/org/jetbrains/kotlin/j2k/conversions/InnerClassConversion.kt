/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.JKKtModifier.KtModifierType.INNER
import org.jetbrains.kotlin.j2k.tree.impl.JKKtModifierImpl

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

        val static = element.modifierList.modifiers.find { it is JKJavaModifier && it.type == JKJavaModifier.JavaModifierType.STATIC }
        if (static != null) {
            element.modifierList.modifiers -= static
        } else if (element.classKind != JKClass.ClassKind.INTERFACE && outer.classKind != JKClass.ClassKind.INTERFACE) {
            element.modifierList.modifiers += JKKtModifierImpl(INNER)
        }
        return recurseArmed(element, element)
    }

}