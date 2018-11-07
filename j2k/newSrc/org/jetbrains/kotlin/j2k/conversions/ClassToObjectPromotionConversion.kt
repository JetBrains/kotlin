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
            val constructor = element.declarationList.firstIsInstanceOrNull<JKKtPrimaryConstructor>()
            val companion = element.declarationList.firstOrNull { it is JKClass && it.classKind == JKClass.ClassKind.COMPANION }
            if (companion != null && (constructor == null || constructor.parameters.isEmpty())) {
                val declarations = element.declarationList - companion - listOfNotNull(constructor)
                if (declarations.isEmpty()) {
                    companion as JKClass
                    companion.invalidate()
                    element.invalidate()
                    return recurse(
                        JKClassImpl(
                            element.modifierList,
                            element.name,
                            element.inheritance,
                            JKClass.ClassKind.OBJECT,
                            element.typeParameterList
                        )
                            .apply {
                                declarationList = companion.declarationList
                            }
                    )
                }
            }
        }
        return recurse(element)
    }
}