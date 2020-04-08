/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.declarationList
import org.jetbrains.kotlin.nj2k.getOrCreateCompanionObject
import org.jetbrains.kotlin.nj2k.tree.*


class StaticsToCompanionExtractConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind == JKClass.ClassKind.COMPANION || element.classKind == JKClass.ClassKind.OBJECT) return element
        val statics = element.declarationList.filter { declaration ->
            declaration is JKOtherModifiersOwner && declaration.hasOtherModifier(OtherModifier.STATIC)
        }
        if (statics.isEmpty()) return recurse(element)
        val companion = element.getOrCreateCompanionObject()

        element.classBody.declarations -= statics
        companion.classBody.declarations += statics.onEach { declaration ->
            if (declaration is JKOtherModifiersOwner) {
                declaration.otherModifierElements -= declaration.elementByModifier(OtherModifier.STATIC)!!
            }
            context.externalCodeProcessor.getMember(declaration)?.let {
                it.isStatic = true
            }
        }
        return recurse(element)
    }
}
