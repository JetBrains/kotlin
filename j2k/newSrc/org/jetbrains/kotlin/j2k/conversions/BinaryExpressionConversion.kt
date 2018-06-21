/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKBinaryExpression
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKKtOperatorImpl


class BinaryExpressionConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return if (element is JKBinaryExpression) element.also {
            element.operator = JKKtOperatorImpl.javaToKotlinOperator[element.operator] ?: element.operator
        } else recurse(element)
    }
}