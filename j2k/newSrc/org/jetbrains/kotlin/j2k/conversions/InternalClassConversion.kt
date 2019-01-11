/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*

class InternalClassConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKVisibilityOwner || element !is JKModalityOwner) return recurse(element)
        val containingClass = element.parentOfType<JKClass>() ?: return recurse(element)

        if (containingClass.visibility == Visibility.INTERNAL
            && element.visibility == Visibility.INTERNAL
            && element.modality == Modality.FINAL
            && (element is JKMethod || element is JKField)
        ) {
            element.visibility = Visibility.PUBLIC
        }
        return recurse(element)
    }
}