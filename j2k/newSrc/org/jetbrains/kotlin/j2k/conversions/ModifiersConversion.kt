/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtModifierImpl
import org.jetbrains.kotlin.j2k.tree.impl.visibility

class ModifiersConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKModifierListOwner && element !is JKLocalVariable) {
            var modifiers = element.modifierList.modifiers
            if (element !is JKField) {
                modifiers = mapOtherModifiers(modifiers)
            }
            element.modifierList.modifiers = modifiers
            // Is declared inside some class
            // TODO: Cleanup
            if (context.converter.settings.noInternalForMembersOfInternal) {
                relaxVisibility(element)
            }
        }
        return recurse(element)
    }

    private fun relaxVisibility(element: JKTreeElement) {
        if (element is JKParameter) return
        element as JKModifierListOwner // TODO: Assert
        val modifierList = element.modifierList
        val parent = element.parent as? JKClass
        if (parent == null
            || modifierList.visibility != JKAccessModifier.Visibility.PACKAGE_PRIVATE
            || parent.modifierList.visibility != JKAccessModifier.Visibility.PACKAGE_PRIVATE) return
        modifierList.visibility = JKAccessModifier.Visibility.PUBLIC
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