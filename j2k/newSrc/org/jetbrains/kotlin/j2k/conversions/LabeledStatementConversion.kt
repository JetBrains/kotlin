/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.blockStatement
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*


class LabeledStatementConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKLabeledStatement) return recurse(element)
        val statement = element.statement as? JKKtConvertedFromForLoopSyntheticWhileStatementImpl ?: return recurse(element)

        return recurse(
            JKBlockStatementWithoutBracketsImpl(
                JKBlockImpl(
                    statement::variableDeclaration.detached(),
                    JKLabeledStatementImpl(statement::whileStatement.detached(), element::labels.detached())
                )
            )
        )
    }
}