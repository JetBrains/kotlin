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
import org.jetbrains.kotlin.backend.common.ir.asSimpleLambda
import org.jetbrains.kotlin.backend.common.ir.inline
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
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
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
) : IrElementTransformerVoid(), FileLoweringPass {
  private lateinit var file: IrFile
  private lateinit var fileSource: String

  private val constructor = this@PowerAssertCallTransformer.context.ir.symbols.assertionErrorConstructor

  override fun lower(irFile: IrFile) {
    file = irFile
    fileSource = File(irFile.path).readText()

    irFile.transformChildrenVoid()
  }

  private data class IrTemporaryVariable(
    val variable: IrVariable,
    val indentation: Int,
    val source: String
  )

  override fun visitCall(expression: IrCall): IrExpression {
    val function = expression.symbol.owner
    if (!function.isAssert)
      return super.visitCall(expression)

    val callSource = fileSource.substring(expression.startOffset, expression.endOffset)
    val callIndent = file.info(expression).startColumnNumber

    context.createIrBuilder(expression.symbol).run {
      at(expression)

      return irBlock {
        val stack = mutableListOf<IrTemporaryVariable>()

        fun push(expression: IrExpression): IrGetValue {
          val variable = irTemporary(expression)
          val source = fileSource.substring(expression.startOffset, expression.endOffset)

          var indentation = file.info(expression).startColumnNumber - callIndent
          if (expression is IrMemberAccessExpression) {
            // TODO Is this the best way to fix indentation of infix operators?
            indentation += when (expression.origin) {
              IrStatementOrigin.EQEQ, IrStatementOrigin.EQEQEQ -> source.indexOf("==")
              IrStatementOrigin.EXCLEQ, IrStatementOrigin.EXCLEQEQ -> source.indexOf("!=")
              IrStatementOrigin.LT -> source.indexOf("<") // TODO What about generics?
              IrStatementOrigin.GT -> source.indexOf(">") // TODO What about generics?
              IrStatementOrigin.LTEQ -> source.indexOf("<=")
              IrStatementOrigin.GTEQ -> source.indexOf(">=")
              else -> 0
            }
          }

          stack.add(IrTemporaryVariable(variable, indentation, source))
          return irGet(variable)
        }

        val assertCondition = expression.getValueArgument(0)!!.transform(object : IrElementTransformerVoid() {
          override fun visitExpression(expression: IrExpression): IrExpression {
            return when (val transformed = super.visitExpression(expression)) {
              is IrGetValue -> push(transformed)
              is IrCall -> push(transformed)
              // TODO what else needs to get pushed in the stack?
              else -> transformed
            }
          }
        }, null)
        require(assertCondition is IrGetValue)

//        print(buildString {
//          append(callSource).newline()
//          val sorted = stack.sortedBy { it.indentation }
//
//          val indentations = sorted.map { it.indentation }
//          var last = -1
//          for (i in indentations) {
//            if (i > last) {
//              indent(i - last - 1).append("|")
//            }
//            last = i
//          }
//          newline()
//
//          for (tmp in sorted.asReversed()) {
//
//            last = -1
//            for (i in indentations) {
//              if (i == tmp.indentation) break
//              if (i > last) {
//                indent(i - last - 1).append("|")
//              }
//              last = i
//            }
//
//            indent(tmp.indentation - last - 1)
//            append(tmp.source).newline()
//          }
//        })

        val lambdaArgument = if (function.valueParameters.size == 2) expression.getValueArgument(1) else null
        val lambda = lambdaArgument?.asSimpleLambda()
        val invokeVar = if (lambda == null && lambdaArgument != null) irTemporary(lambdaArgument) else null

        // Build assertion message
        val throwError = irThrow(irCall(constructor).apply {
          putValueArgument(0, irConcat().apply {

            addArgument(
              when {
                lambda != null -> lambda.inline()
                lambdaArgument != null -> {
                  val invoke = lambdaArgument.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
                  irCallOp(invoke.symbol, invoke.returnType, irGet(invokeVar!!))
                }
                else -> irString("Assertion failed")
              }
            )

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
          })
        })

        +irIfThen(irNot(assertCondition), throwError)
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
