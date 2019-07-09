/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.*

class RemoveWrongExtraModifiersForSingleFunctionsConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKExtraModifiersOwner) return recurse(element)
        if (element.parentOfType<JKClass>() == null) {
            element.elementByModifier(ExtraModifier.STATIC)?.also { modifierElement ->
                element.extraModifierElements -= modifierElement
            }
        }
        return recurse(element)
    }
}
