/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.parameter

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.powerassert.diagram.IrDiagramVariable
import org.jetbrains.kotlin.powerassert.diagram.SourceFile
import org.jetbrains.kotlin.powerassert.sourceRange

class CallExplanationParameterBuilder(
    private val factory: ExplanationFactory,
    private val sourceFile: SourceFile,
    private val originalCall: IrCall,
) : ParameterBuilder {
    override fun build(
        builder: IrBuilderWithScope,
        argumentVariables: Map<IrValueParameter, List<IrDiagramVariable>>,
    ): IrExpression {
        val callInfo = sourceFile.getCompleteSourceRangeInfo(originalCall)

        // Get call source string starting at the very beginning of the first line.
        // This is so multiline calls all start from the same column offset.
        val startOffset = callInfo.startOffset - callInfo.startColumnNumber
        val source = sourceFile.getRedactedTextBlock(callInfo)

        return with(factory) {
            builder.irCallExplanation(
                offset = startOffset,
                source = builder.irString(source),
                arguments = originalCall.symbol.owner.parameters
                    .map { parameter ->
                        val variables = argumentVariables[parameter]
                            ?.filterIsInstance<IrDiagramVariable.Displayable>()
                            ?.takeIf { it.isNotEmpty() }

                        if (variables == null) {
                            builder.irNull()
                        } else {
                            val sourceRange = originalCall.arguments[parameter]!!.sourceRange
                            builder.irArgument(
                                startOffset = sourceRange.start - startOffset,
                                endOffset = sourceRange.endInclusive - startOffset,
                                kind = parameter.kind,
                                expressions = variables.map { builder.irExpression(it, startOffset) },
                            )
                        }
                    },
            )
        }
    }
}
