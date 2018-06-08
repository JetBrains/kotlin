/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtModifierImpl

class ModifiersConversion : RecursiveApplicableConversionBase() {

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return if (element is JKModifierList) element.also {
            if (element.parent !is JKLocalVariable && !it.modifiers.filter { it is JKJavaAccessModifier }.any()) {
                it.modifiers += JKKtModifierImpl(JKKtModifier.KtModifierType.INTERNAL)
            }
            it.modifiers = mapModifiers(it.modifiers)
        } else recurse(element)
    }

    private fun mapModifiers(modifiers: List<JKModifier>): List<JKModifier> {
        return modifiers.mapNotNull {
            when (it) {
                is JKJavaAccessModifier -> when (it.type) {
                    JKJavaAccessModifier.AccessModifierType.PUBLIC -> null
                    JKJavaAccessModifier.AccessModifierType.PRIVATE -> JKKtModifierImpl(JKKtModifier.KtModifierType.PRIVATE)
                    JKJavaAccessModifier.AccessModifierType.PROTECTED -> JKKtModifierImpl(JKKtModifier.KtModifierType.PROTECTED)
                }
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