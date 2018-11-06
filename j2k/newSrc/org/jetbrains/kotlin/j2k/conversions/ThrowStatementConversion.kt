/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKExpressionStatement
import org.jetbrains.kotlin.j2k.tree.JKJavaThrowStatement
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.detached
import org.jetbrains.kotlin.j2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtThrowExpressionImpl


class ThrowStatementConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaThrowStatement) return recurse(element)
        val throwExpression = JKKtThrowExpressionImpl(element::exception.detached())
        return recurse(JKExpressionStatementImpl(throwExpression))
    }
}