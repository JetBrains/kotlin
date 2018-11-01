/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.JKBinaryExpression
import org.jetbrains.kotlin.j2k.tree.JKBranchElement
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKBinaryExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaOperatorImpl
import org.jetbrains.kotlin.j2k.tree.impl.toKtToken
import org.jetbrains.kotlin.j2k.kotlinBinaryExpression


class BinaryExpressionConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKBinaryExpression) return recurse(element)
        val ktOperatorToken = (element.operator as? JKJavaOperatorImpl)?.token?.toKtToken() ?: return recurse(element)
        val left = recurse(element.left)
        val right = recurse(element.right)
        (element as? JKBranchElement)?.invalidate()
        val ktBinaryExpression = kotlinBinaryExpression(left, right, ktOperatorToken, context)
            ?: return JKBinaryExpressionImpl(left, right, element.operator)
        return recurse(ktBinaryExpression)
    }

}