/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.equalsExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.lexer.KtTokens

class EqualsOperatorConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        if (element.receiver is JKSuperExpression) return recurse(element)
        val selector = element.selector as? JKMethodCallExpression ?: return (element)
        val argument = selector.arguments.expressions.singleOrNull() ?: return recurse(element)
        if (selector.identifier.deepestFqName() == "java.lang.Object.equals") {
            return recurse(
                JKParenthesizedExpressionImpl(
                    equalsExpression(
                        element::receiver.detached(),
                        argument.detached(selector.arguments),
                        context.symbolProvider
                    )
                )
            )
        }
        return recurse(element)
    }
}