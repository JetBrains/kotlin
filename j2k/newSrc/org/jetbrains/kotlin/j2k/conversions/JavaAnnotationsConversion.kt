/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtLiteralExpressionImpl

class JavaAnnotationsConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKAnnotation) return recurse(element)
        if (element.classSymbol.name == "Deprecated" && element.arguments.expressions.isEmpty()) {
            element.arguments.expressions += JKKtLiteralExpressionImpl("\"\"", JKLiteralExpression.LiteralType.STRING)
        }
        return recurse(element)
    }
}