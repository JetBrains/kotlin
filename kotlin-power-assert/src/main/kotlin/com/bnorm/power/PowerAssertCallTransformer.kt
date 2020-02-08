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
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
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

fun String.substring(expression: IrElement) = substring(expression.startOffset, expression.endOffset)
fun IrFile.info(expression: IrElement) = fileEntry.getSourceRangeInfo(expression.startOffset, expression.endOffset)

class PowerAssertCallTransformer(
  private val context: JvmBackendContext
) : IrElementTransformerVoid(), FileLoweringPass {
  private lateinit var file: IrFile
  private lateinit var fileSource: String

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

    val assertionArgument = expression.getValueArgument(0)!!
    val lambdaArgument = if (function.valueParameters.size == 2) expression.getValueArgument(1) else null

    context.createIrBuilder(expression.symbol).run {
      at(expression)

      val lambda = lambdaArgument?.asSimpleLambda()
      val title = when {
        lambda != null -> lambda.inline()
        lambdaArgument != null -> {
          val invoke = lambdaArgument.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
          irCallOp(invoke.symbol, invoke.returnType, lambdaArgument)
        }
        else -> irString("Assertion failed")
      }

      val tree = buildAssertTree(assertionArgument)
      val root = tree.children.single()

//      println(assertionArgument.dump())
//      println(tree.dump())

      return irBlock {
        buildAssert(this@PowerAssertCallTransformer.context, file, fileSource, callSource, callIndent, title, root)
      }
    }
  }
}

fun IrBlockBuilder.buildAssert(
  context: JvmBackendContext,
  file: IrFile,
  fileSource: String,
  callSource: String,
  callIndent: Int,
  title: IrExpression,
  node: Node,
  stack: MutableList<IrStackVariable> = mutableListOf(),
  constructor: IrConstructorSymbol = context.ir.symbols.assertionErrorConstructor,
  thenPart: IrBlockBuilder.(stack: MutableList<IrStackVariable>) -> IrExpression = { subStack -> buildThrow(constructor, buildMessage(title, subStack, callSource)) }
) {
  fun IrBlockBuilder.nest(children: List<Node>, index: Int, stack: MutableList<IrStackVariable>) {
    val child = children[index]
    buildAssert(context, file, fileSource, callSource, callIndent, title, child, stack, constructor) { subStack ->
      if (index + 1 == children.size) buildThrow(constructor, buildMessage(title, subStack, callSource))
      else irBlock { nest(children, index + 1, subStack) }
    }
  }

  when (node) {
    is ExpressionNode -> {
      +irIfNotThan(stack, file, fileSource, callIndent, node, thenPart)
    }
    is AndNode -> {
      for (child in node.children) {
        buildAssert(context, file, fileSource, callSource, callIndent, title, child, stack, constructor, thenPart)
      }
    }
    is OrNode -> {
      nest(node.children, 0, stack)
    }
  }
}

private inline fun IrBlockBuilder.irIfNotThan(
  stack: MutableList<IrStackVariable>,
  file: IrFile,
  fileSource: String,
  callIndent: Int,
  node: ExpressionNode,
  thenPart: IrBlockBuilder.(subStack: MutableList<IrStackVariable>) -> IrExpression
): IrWhen {
  val stackTransformer = StackBuilder(this, stack, file, fileSource, callIndent, node.expressions)
  val transformed = node.expressions.first().transform(stackTransformer, null)
  return irIfThen(irNot(transformed), thenPart(stack.toMutableList()))
}

class StackBuilder(
  private val builder: IrBlockBuilder,
  private val stack: MutableList<IrStackVariable>,
  private val file: IrFile,
  private val fileSource: String,
  private val callIndent: Int,
  private val transform: List<IrExpression>
) : IrElementTransformerVoid() {
  private fun push(expression: IrExpression): IrGetValue = with(builder) {
    val variable = irTemporary(expression)
    val source = fileSource.substring(expression)

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

    stack.add(IrStackVariable(variable, indentation, source))
    irGet(variable)
  }

  override fun visitExpression(expression: IrExpression): IrExpression {
    return super.visitExpression(expression).also { if (expression in transform) push(it) }
  }
}

val IrFunction.isAssert: Boolean
  get() = name.asString() == "assert" && getPackageFragment()?.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME

fun StringBuilder.indent(indentation: Int): StringBuilder = append(" ".repeat(indentation))
fun StringBuilder.newline(): StringBuilder = append("\n")
