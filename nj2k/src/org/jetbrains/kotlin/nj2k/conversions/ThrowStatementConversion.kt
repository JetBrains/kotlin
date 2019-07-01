/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.JKJavaThrowStatement
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.detached
import org.jetbrains.kotlin.nj2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtThrowExpressionImpl


class ThrowStatementConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaThrowStatement) return recurse(element)
        val throwExpression = JKKtThrowExpressionImpl(element::exception.detached())
        return recurse(JKExpressionStatementImpl(throwExpression))
    }
}