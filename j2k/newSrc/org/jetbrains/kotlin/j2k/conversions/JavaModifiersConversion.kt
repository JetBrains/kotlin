/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*

class JavaModifiersConversion: RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKVisibilityOwner) {
            if (element.visibility == Visibility.PACKAGE_PRIVATE) {
                element.visibility = Visibility.INTERNAL
            }
        }
        return recurse(element)
    }
}