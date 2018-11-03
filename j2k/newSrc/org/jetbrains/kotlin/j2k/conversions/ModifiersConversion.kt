/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtModifierImpl
import org.jetbrains.kotlin.j2k.tree.impl.mutability
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
            if (element is JKParameter) {
                convertParameterModifiers(element)
            }
            element.sortModifiers()
        }
        return recurse(element)
    }

    private fun convertParameterModifiers(jkParameter: JKParameter) {
        if (jkParameter.modifierList.mutability == Mutability.Default) {
            jkParameter.modifierList.modifiers = emptyList()
        }
    }


    private fun relaxVisibility(element: JKTreeElement) {
        if (element is JKParameter) return
        element as JKModifierListOwner // TODO: Assert
        val modifierList = element.modifierList
        val parent = element.parent as? JKClass
        if (parent == null
            || modifierList.visibility != JKAccessModifier.Visibility.PACKAGE_PRIVATE
            || parent.modifierList.visibility != JKAccessModifier.Visibility.PACKAGE_PRIVATE
        ) return
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

    private fun JKModifierListOwner.sortModifiers() {
        modifierList.modifiers = modifierList.modifiers.sortedBy { it.priority }
    }

    private val JKModifier.priority: Int
        get() =
            when (this) {
                is JKAccessModifier -> 0
                is JKModalityModifier ->
                    when (this.modality) {
                        JKModalityModifier.Modality.FINAL, JKModalityModifier.Modality.OPEN, JKModalityModifier.Modality.ABSTRACT -> 1
                        JKModalityModifier.Modality.OVERRIDE -> 2
                    }
                is JKJavaModifier -> 3
                is JKMutabilityModifier -> 4
                else -> TODO(this.toString())
            }


    private fun JKModifierListOwner.filterModifiers(filter: (JKModifier) -> Boolean) {
        modifierList.modifiers = modifierList.modifiers.filter(filter)
    }
}