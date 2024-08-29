/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.explanation

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.powerassert.diagram.IrTemporaryVariable
import org.jetbrains.kotlin.powerassert.diagram.SourceFile
import org.jetbrains.kotlin.powerassert.sourceRange

fun IrBuilderWithScope.irVariableExplanation(
    factory: ExplanationFactory,
    sourceFile: SourceFile,
    variable: IrVariable,
    variables: List<IrTemporaryVariable>,
    variableDiagrams: Map<IrVariable, IrVariable>,
): IrExpression {
    val variableInfo = sourceFile.getSourceRangeInfo(
        // TODO K1 and K2 have different offsets for the variable...
        //  K2 doesn't include the annotations and val/var keyword
        variable.sourceRange.start,
        variable.sourceRange.endInclusive,
    )

    // Get call source string starting at the very beginning of the first line.
    // This is so multiline calls all start from the same column offset.
    val startOffset = variableInfo.startOffset - variableInfo.startColumnNumber
    val source = sourceFile.getText(startOffset, variableInfo.endOffset)
        .clearSourcePrefix(variableInfo.startColumnNumber)

    val initializerSourceRange = variable.initializer!!.sourceRange
    return with(factory) {
        irVariableDiagram(
            offset = startOffset,
            source = irString(source),
            name = variable.name.asString(),
            assignment = irAssignment(
                startOffset = initializerSourceRange.start - startOffset,
                endOffset = initializerSourceRange.endInclusive - startOffset,
                expressions = variables.map { irExpression(it, startOffset, variableDiagrams) },
            ),
        )
    }
}
