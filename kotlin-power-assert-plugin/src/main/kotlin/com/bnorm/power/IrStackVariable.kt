/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.power

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

data class IrStackVariable(
  val temporary: IrVariable,
  val original: IrExpression
)

data class ValueDisplay(
  val value: IrVariable,
  val indent: Int,
  val row: Int,
  val source: String
)

fun IrBuilderWithScope.buildMessage(
  file: IrFile,
  fileSource: String,
  title: IrExpression,
  expression: IrExpression,
  stack: List<IrStackVariable>
): IrExpression {

  val originalInfo = file.info(expression)
  val callIndent = originalInfo.startColumnNumber
  val callSource = fileSource.substring(expression)
    .replace("\n" + " ".repeat(callIndent), "\n") // Remove additional indentation

  val stackValues = stack.map { (temporary, original) ->
    val source = fileSource.substring(original)
      .replace("\n" + " ".repeat(callIndent), "\n") // Remove additional indentation

    val info = file.info(original)
    var indent = info.startColumnNumber - callIndent
    var row = info.startLineNumber - originalInfo.startLineNumber

    val columnOffset: Int = when (original) {
      is IrMemberAccessExpression<*> -> {
        val owner = original.symbol.owner
        when {
          owner is IrSimpleFunction && owner.isInfix -> source.indexOf(owner.name.asString())
          else -> when (original.origin) {
            // TODO handle equality and comparison better?
            IrStatementOrigin.EQEQ, IrStatementOrigin.EQEQEQ -> source.indexOf("==")
            IrStatementOrigin.EXCLEQ, IrStatementOrigin.EXCLEQEQ -> source.indexOf("!=")
            IrStatementOrigin.LT -> source.indexOf("<") // TODO What about generics?
            IrStatementOrigin.GT -> source.indexOf(">") // TODO What about generics?
            IrStatementOrigin.LTEQ -> source.indexOf("<=")
            IrStatementOrigin.GTEQ -> source.indexOf(">=")
            else -> 0
          }
        }
      }
      else -> 0
    }

    val prefix = source.substring(0, columnOffset)
    val rowShift = prefix.count { it == '\n' }
    if (rowShift == 0) {
      indent += columnOffset
    } else {
      row += rowShift
      indent = columnOffset - (prefix.lastIndexOf('\n') + 1)
    }

    ValueDisplay(temporary, indent, row, source)
  }

  val valuesByRow = stackValues.groupBy { it.row }
  val rows = callSource.split("\n")

  return irConcat().apply {
    addArgument(title)

    for ((row, rowSource) in rows.withIndex()) {
      val rowValues = valuesByRow[row]?.let { values -> values.sortedBy { it.indent } } ?: emptyList()
      val indentations = rowValues.map { it.indent }

      addArgument(irString(buildString {
        newline()
        append(rowSource)
        if (indentations.isNotEmpty()) newline()

        var last = -1
        for (i in indentations) {
          if (i > last) {
            indent(i - last - 1).append("|")
          }
          last = i
        }
      }))


      for (tmp in rowValues.asReversed()) {
        addArgument(irString(buildString {
          var last = -1
          newline()
          for (i in indentations) {
            if (i == tmp.indent) break
            if (i > last) {
              indent(i - last - 1).append("|")
            }
            last = i
          }
          indent(tmp.indent - last - 1)
        }))
        addArgument(irGet(tmp.value))
      }
    }
  }
}

fun String.substring(expression: IrElement) = substring(expression.startOffset, expression.endOffset)
fun IrFile.info(expression: IrElement) = fileEntry.getSourceRangeInfo(expression.startOffset, expression.endOffset)

fun StringBuilder.indent(indentation: Int): StringBuilder = append(" ".repeat(indentation))
fun StringBuilder.newline(): StringBuilder = append("\n")
