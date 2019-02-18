/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.getOrCreateCompainonObject
import org.jetbrains.kotlin.nj2k.tree.*

class StaticsToCompanionExtractConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind == JKClass.ClassKind.COMPANION || element.classKind == JKClass.ClassKind.OBJECT) return element
        val statics = element.declarationList.filter { declaration ->
            declaration is JKExtraModifiersOwner && declaration.hasExtraModifier(ExtraModifier.STATIC)
        }
        if (statics.isEmpty()) return recurse(element)
        val companion = element.getOrCreateCompainonObject()

        element.classBody.declarations -= statics
        companion.classBody.declarations += statics.onEach { declaration ->
            (declaration as JKExtraModifiersOwner)
            declaration.extraModifierElements -= declaration.elementByModifier(ExtraModifier.STATIC)!!
        }
        return recurse(element)
    }
}
