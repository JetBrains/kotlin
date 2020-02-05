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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.io.File

fun FileLoweringPass.runOnFileInOrder(irFile: IrFile) {
  irFile.acceptVoid(object : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
      element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
      lower(declaration)
      super.visitFile(declaration)
    }
  })
}

class PowerAssertCallTransformer(
  private val context: JvmBackendContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {
  private lateinit var file: IrFile
  private lateinit var fileSource: String

  private val constructor = this@PowerAssertCallTransformer.context.ir.symbols.assertionErrorConstructor

  override fun lower(irFile: IrFile) {
    file = irFile
    fileSource = File(irFile.path).readText()

    irFile.transformChildrenVoid()
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val function = expression.symbol.owner
    if (!function.isAssert)
      return super.visitCall(expression)

    val callSource = fileSource.substring(expression.startOffset, expression.endOffset)
    val callIndent = file.info(expression).startColumnNumber

    context.createIrBuilder(expression.symbol).run {
      at(expression)

      return irBlock {
        val assertCondition = expression.getValueArgument(0)!!
//        val argumentSource = fileSource.substring(assertCondition.startOffset, assertCondition.endOffset)
        val indent = file.info(assertCondition).startColumnNumber - callIndent

//        println(buildString {
//          append(callSource).newline()
//          indent(indent).append("|").newline()
//          indent(indent).append(argumentSource)
//        })

        // TODO transform tree of expressions and create irTemporary for each
        val temp = irTemporary(assertCondition)
        val throwError = irThrow(irCall(constructor).apply {

          val message = irConcat().apply {
            addArgument(irString(buildString {
              append("Assertion failed:").newline()
              append(callSource).newline()
              indent(indent).append("|").newline()
              indent(indent)
            }))
            addArgument(irGet(temp))
          }
          putValueArgument(0, message)
        })
        +irIfThen(irNot(irGet(temp)), throwError)
      }
    }
  }


}

val IrFunction.isAssert: Boolean
  get() = name.asString() == "assert" && getPackageFragment()?.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME

fun IrFile.info(expression: IrElement): SourceRangeInfo {
  return fileEntry.getSourceRangeInfo(expression.startOffset, expression.endOffset)
}

fun StringBuilder.indent(indentation: Int) = apply {
  repeat(indentation) {
    append(" ")
  }
}

fun StringBuilder.newline() = apply {
  append("\n")
}
