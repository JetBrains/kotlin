/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKKtPrimaryConstructor
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKClassImpl
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ClassToObjectPromotionConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKClass && element.classKind == JKClass.ClassKind.CLASS) {
            val companion =
                element.declarationList.firstIsInstanceOrNull<JKClass>()
                    ?.takeIf { it.classKind == JKClass.ClassKind.COMPANION }
                    ?: return recurse(element)

            val allDeclarationsMatches = element.declarationList.all {
                when (it) {
                    is JKKtPrimaryConstructor -> it.parameters.isEmpty() && it.block.statements.isEmpty()
                    is JKClass -> it.classKind == JKClass.ClassKind.COMPANION
                    else -> false
                }
            }

            if (allDeclarationsMatches) {
                companion.invalidate()
                element.invalidate()
                return recurse(
                    JKClassImpl(
                        element.modifierList,
                        element.name,
                        element.inheritance,
                        JKClass.ClassKind.OBJECT,
                        element.typeParameterList
                    ).apply {
                        declarationList = companion.declarationList
                    }
                )
            }
        }

        return recurse(element)
    }
}