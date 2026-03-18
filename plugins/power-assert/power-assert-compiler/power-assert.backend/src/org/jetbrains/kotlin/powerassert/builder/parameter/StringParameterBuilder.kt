/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.parameter

import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.powerassert.diagram.IrDiagramVariable
import org.jetbrains.kotlin.powerassert.diagram.SourceFile
import org.jetbrains.kotlin.powerassert.diagram.findDisplayOffset
import org.jetbrains.kotlin.powerassert.irString

class StringParameterBuilder(
    private val sourceFile: SourceFile,
    private val originalCall: IrCall,
    private val function: IrSimpleFunction,
    private val messageArgument: IrExpression?,
) : ParameterBuilder {

    override fun build(
        builder: IrBuilderWithScope,
        argumentVariables: Map<IrValueParameter, List<IrDiagramVariable>>,
    ): IrExpression {
        val messageParameter = function.parameters.last { it.kind == IrParameterKind.Regular }
        val prefix = messageArgument?.let { builder.buildMessagePrefix(it, messageParameter) }
        return builder.irDiagramString(sourceFile, prefix, originalCall, argumentVariables.values.flatten())
    }

    private fun IrBuilderWithScope.buildMessagePrefix(
        messageArgument: IrExpression,
        messageParameter: IrValueParameter,
    ): IrExpression? {
        return when (messageArgument) {
            is IrConst -> messageArgument
            is IrStringConcatenation -> messageArgument
            is IrGetValue -> {
                if (messageArgument.type == context.irBuiltIns.stringType) {
                    messageArgument
                } else {
                    val invoke = messageParameter.type.classOrNull!!.functions
                        .filter { !it.owner.isFakeOverride } // TODO best way to find single access method?
                        .single()
                    irCall(invoke).apply { dispatchReceiver = messageArgument }
                }
            }
            // Kotlin Lambda or SAMs conversion lambda
            is IrFunctionExpression, is IrTypeOperatorCall -> {
                val invoke = messageParameter.type.classOrNull!!.functions
                    .filter { !it.owner.isFakeOverride } // TODO best way to find single access method?
                    .single()
                irCall(invoke).apply { dispatchReceiver = messageArgument }
            }
            else -> null
        }
    }

    private fun IrBuilderWithScope.irDiagramString(
        sourceFile: SourceFile,
        prefix: IrExpression? = null,
        call: IrCall,
        variables: List<IrDiagramVariable>,
    ): IrExpression {
        val callInfo = sourceFile.getCompleteSourceRangeInfo(call)

        // Get call source string starting at the very beginning of the first line.
        // This is so multiline calls all start from the same column offset.
        val rows = sourceFile.getRedactedTextBlock(callInfo)
            .split("\n")

        val minSourceIndent = rows.minOf { line ->
            // Find index of first non-whitespace character.
            val indent = line.indexOfFirst { !it.isWhitespace() }
            if (indent == -1) Int.MAX_VALUE else indent
        }

        val valuesByRow = variables
            .filterIsInstance<IrDiagramVariable.Displayable>()
            .mapNotNull { it.toValueDisplay(callInfo) }
            .sortedBy { it.indent }
            .groupBy { it.row }

        return irConcat().apply {
            if (prefix != null) addArgument(prefix)

            for ((row, rowSource) in rows.withIndex()) {
                addArgument(
                    irString {
                        appendLine()
                        val sourceLine = rowSource.substring(minOf(minSourceIndent, rowSource.length))
                        if (sourceLine.isNotBlank() && valuesByRow[row - 1] != null) appendLine() // Add an extra line after displayed values.
                        append(sourceLine)
                    },
                )

                val rowValues = valuesByRow[row] ?: continue

                val lineTemplate = buildString {
                    val indentations = rowValues.mapTo(hashSetOf()) { it.indent }
                    val lastIndent = rowValues.last().indent
                    for ((i, c) in rowSource.withIndex()) {
                        when {
                            i in indentations -> {
                                // Add bar at indents for value display.
                                append('|')
                                if (i == lastIndent) break // Do not add trailing whitespace.
                            }
                            c == '\t' -> append('\t') // Preserve tabs in source code.
                            else -> append(' ')
                        }
                    }
                }

                addArgument(
                    irString {
                        appendLine()
                        append(lineTemplate.substring(minSourceIndent))
                    },
                )

                for (tmp in rowValues.asReversed()) {
                    addArgument(
                        irString {
                            appendLine()
                            append(lineTemplate.substring(minSourceIndent, tmp.indent))
                        },
                    )
                    addArgument(irGet(tmp.value))
                }
            }

            addArgument(
                irString {
                    appendLine()
                }
            )
        }
    }

    private data class ValueDisplay(
        val value: IrVariable,
        val indent: Int,
        val row: Int,
    )

    private fun IrDiagramVariable.Displayable.toValueDisplay(
        originalInfo: SourceRangeInfo,
    ): ValueDisplay? {
        if (literal) return null

        var indent = sourceRangeInfo.startColumnNumber
        var row = sourceRangeInfo.startLineNumber - originalInfo.startLineNumber

        val source = text
        val columnOffset = findDisplayOffset(original, sourceRangeInfo, source)

        val prefix = source.substring(0, columnOffset)
        val rowShift = prefix.count { it == '\n' }
        if (rowShift == 0) {
            indent += columnOffset
        } else {
            row += rowShift
            indent = columnOffset - (prefix.lastIndexOf('\n') + 1)
        }

        return ValueDisplay(temporary, indent, row)
    }
}
