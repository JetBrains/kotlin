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


class BinaryExpressionConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKBinaryExpression) return recurse(element)
        val ktOperator = (element.operator as? JKJavaOperatorImpl)?.toKtToken() ?: return recurse(element)
        val left = recurse(element.left)
        val right = recurse(element.right)
        (element as? JKBranchElement)?.invalidate()
        val ktBinaryExpression = JKBinaryExpressionImpl.createKotlinBinaryExpression(left, right, ktOperator, context)
            ?: return JKBinaryExpressionImpl(left, right, element.operator)
        return recurse(ktBinaryExpression)
    }

}