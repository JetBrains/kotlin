/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*

class InternalClassConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        return recurseArmed(element, element.visibility == Visibility.INTERNAL)
    }

    private fun recurseArmed(element: JKTreeElement, parentIsInternal: Boolean): JKTreeElement {
        return applyRecursive(element, parentIsInternal, ::applyArmed)
    }

    private fun applyArmed(element: JKTreeElement, parentIsInternal: Boolean): JKTreeElement {
        if (element is JKClass) return recurseArmed(element, parentIsInternal)
        if (element !is JKVisibilityOwner) return recurseArmed(element, parentIsInternal)
        val isInternal = element.visibility == Visibility.INTERNAL
        if (isInternal && parentIsInternal && context.converter.settings.noInternalForMembersOfInternal) {
            element.visibility = Visibility.PUBLIC
        }
        return recurseArmed(element, isInternal)
    }
}