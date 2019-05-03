/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.fixLiteral
import org.jetbrains.kotlin.nj2k.tree.JKJavaLiteralExpression
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement

class LiteralConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaLiteralExpression) return recurse(element)
        return element.fixLiteral(element.type)
    }
}
