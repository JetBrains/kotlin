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

import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol

data class IrStackVariable(
  val variable: IrVariable,
  val indentation: Int,
  val source: String
)

fun IrBlockBuilder.buildThrow(
  constructor: IrConstructorSymbol,
  message: IrExpression
): IrThrow = irThrow(irCall(constructor).apply {
  putValueArgument(0, message)
})

fun IrBlockBuilder.buildMessage(
  title: IrExpression,
  stack: List<IrStackVariable>,
  callSource: String
): IrExpression {
  return irConcat().apply {
    addArgument(title)

    val sorted = stack.sortedBy { it.indentation }
    val indentations = sorted.map { it.indentation }

    addArgument(irString(buildString {
      newline()
      append(callSource).newline()
      var last = -1
      for (i in indentations) {
        if (i > last) {
          indent(i - last - 1).append("|")
        }
        last = i
      }
    }))


    for (tmp in sorted.asReversed()) {
      addArgument(irString(buildString {
        var last = -1
        newline()
        for (i in indentations) {
          if (i == tmp.indentation) break
          if (i > last) {
            indent(i - last - 1).append("|")
          }
          last = i
        }
        indent(tmp.indentation - last - 1)
      }))
      addArgument(irGet(tmp.variable))
    }
  }
}
