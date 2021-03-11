/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.asStatement
import org.jetbrains.kotlin.nj2k.tree.*


import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class LabeledStatementConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKExpressionStatement) return recurse(element)
        val labeledStatement = element.expression as? JKLabeledExpression ?: return recurse(element)
        val convertedFromForLoopSyntheticWhileStatement = labeledStatement.statement
            .safeAs<JKBlockStatement>()
            ?.block
            ?.statements
            ?.singleOrNull()
            ?.safeAs<JKKtConvertedFromForLoopSyntheticWhileStatement>() ?: return recurse(element)

        return recurse(
            JKBlockStatementWithoutBrackets(
                convertedFromForLoopSyntheticWhileStatement::variableDeclarations.detached() +
                        JKLabeledExpression(
                            convertedFromForLoopSyntheticWhileStatement::whileStatement.detached(),
                            labeledStatement::labels.detached()
                        ).asStatement()
            )
        )
    }
}