/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ast.EnumConstant
import org.jetbrains.kotlin.j2k.tree.*

//TODO temporary
class SortClassMembersConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        element.declarationList = element.declarationList
            .sortedByDescending { it is JKField }
        return recurse(element)
    }
}