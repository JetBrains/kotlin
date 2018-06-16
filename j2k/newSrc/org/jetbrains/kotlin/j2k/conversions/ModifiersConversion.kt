/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtModifierImpl

class ModifiersConversion : RecursiveApplicableConversionBase() {

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return if (element is JKModifierListOwner && element !is JKLocalVariable)
            element.also {
                var modifiers = it.modifierList.modifiers
                if (element !is JKField) {
                    modifiers = mapOtherModifiers(modifiers)
                }
                it.modifierList.modifiers = modifiers
            } else recurse(element)
    }

    private fun mapOtherModifiers(modifiers: List<JKModifier>): List<JKModifier> {
        return modifiers.map {
            when (it) {
                is JKJavaModifier -> when (it.type) {
                    JKJavaModifier.JavaModifierType.NATIVE -> JKKtModifierImpl(JKKtModifier.KtModifierType.EXTERNAL)
                    else -> it
                }
                else -> it
            }
        }
    }
}