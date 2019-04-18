/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.asStatement
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKBlockStatementWithoutBracketsImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtConvertedFromForLoopSyntheticWhileStatementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKLabeledStatementImpl
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class LabeledStatementConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKExpressionStatement) return recurse(element)
        val labeledStatement = element.expression  as? JKLabeledStatement ?: return recurse(element)
        val convertedFromForLoopSyntheticWhileStatement = labeledStatement.statement
            .safeAs<JKBlockStatement>()
            ?.block
            ?.statements
            ?.singleOrNull()
            ?.safeAs<JKKtConvertedFromForLoopSyntheticWhileStatementImpl>() ?: return recurse(element)

        return recurse(
            JKBlockStatementWithoutBracketsImpl(
                listOf(
                    convertedFromForLoopSyntheticWhileStatement::variableDeclaration.detached(),
                    JKLabeledStatementImpl(
                        convertedFromForLoopSyntheticWhileStatement::whileStatement.detached(),
                        labeledStatement::labels.detached()
                    ).asStatement()
                )
            )
        )
    }
}