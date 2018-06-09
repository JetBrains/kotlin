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
                    if (!modifiers.any { it is JKJavaModifier && it.type == JKJavaModifier.JavaModifierType.FINAL }) {
                        modifiers += JKKtModifierImpl(JKKtModifier.KtModifierType.OPEN)
                    }
                    modifiers = mapOtherModifiers(modifiers)
                }
                if (!modifiers.any { it is JKJavaAccessModifier }) {
                    modifiers += JKKtModifierImpl(JKKtModifier.KtModifierType.INTERNAL)
                }
                it.modifierList.modifiers = mapAccessModifiers(modifiers)
            } else recurse(element)
    }

    private fun mapAccessModifiers(modifiers: List<JKModifier>): List<JKModifier> {
        return modifiers.mapNotNull {
            when (it) {
                is JKJavaAccessModifier -> when (it.type) {
                    JKJavaAccessModifier.AccessModifierType.PUBLIC -> null
                    JKJavaAccessModifier.AccessModifierType.PRIVATE -> JKKtModifierImpl(JKKtModifier.KtModifierType.PRIVATE)
                    JKJavaAccessModifier.AccessModifierType.PROTECTED -> JKKtModifierImpl(JKKtModifier.KtModifierType.PROTECTED)
                }
                else -> it
            }
        }
    }


    private fun mapOtherModifiers(modifiers: List<JKModifier>): List<JKModifier> {
        return modifiers.mapNotNull {
            when (it) {
                is JKJavaModifier -> when (it.type) {
                    JKJavaModifier.JavaModifierType.ABSTRACT -> JKKtModifierImpl(JKKtModifier.KtModifierType.ABSTRACT)
                    JKJavaModifier.JavaModifierType.FINAL -> null
                    JKJavaModifier.JavaModifierType.NATIVE -> JKKtModifierImpl(JKKtModifier.KtModifierType.EXTERNAL)
                    else -> it
                }
                else -> it
            }
        }
    }
}