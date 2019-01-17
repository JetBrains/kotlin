/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.tree.*


class InterfaceWithFieldConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind != JKClass.ClassKind.INTERFACE) return recurse(element)
        val finalFields = element.declarationList
            .filterIsInstance<JKField>()
            .filter { it.modality == Modality.FINAL }
        if (finalFields.isNotEmpty()) {
            element.classBody.declarations -= finalFields
            val companion = element.getOrCreateCompainonObject()
            companion.classBody.declarations += finalFields
        }
        return recurse(element)
    }
}