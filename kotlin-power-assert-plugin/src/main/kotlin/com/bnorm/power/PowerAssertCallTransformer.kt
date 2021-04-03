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

import com.bnorm.power.diagram.IrTemporaryVariable
import com.bnorm.power.diagram.DiagramGenerator
import com.bnorm.power.diagram.buildTree
import com.bnorm.power.diagram.info
import com.bnorm.power.diagram.irDiagram
import com.bnorm.power.diagram.substring
import com.bnorm.power.internal.ReturnableBlockTransformer
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.asSimpleLambda
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
  private val context: IrPluginContext,
  private val messageCollector: MessageCollector,
  private val functions: Set<FqName>
) : IrElementTransformerVoidWithContext(), FileLoweringPass {
  private lateinit var file: IrFile
  private lateinit var fileSource: String

  override fun lower(irFile: IrFile) {
    file = irFile
    fileSource = File(irFile.path).readText()
      .replace("\r\n", "\n") // https://youtrack.jetbrains.com/issue/KT-41888

    irFile.transformChildrenVoid()
//    println(irFile.dumpKotlinLike())
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val function = expression.symbol.owner
    val fqName = function.kotlinFqName
    if (function.valueParameters.isEmpty() || functions.none { fqName == it })
      return super.visitCall(expression)

    // Find a valid delegate function or do not translate
    val delegate = findDelegate(function) ?: run {
      val valueType = function.valueParameters[0].type.asString()
      messageCollector.warn(
        expression,
        "Unable to find overload for function $fqName callable as $fqName($valueType, String) or $fqName($valueType, () -> String) for power-assert transformation"
      )
      return super.visitCall(expression)
    }

    val assertionArgument = expression.getValueArgument(0)!!
    val messageArgument = if (function.valueParameters.size == 2) expression.getValueArgument(1) else null

    // If the tree does not contain any children, the expression is not transformable
    val root = buildTree(assertionArgument) ?: run {
      messageCollector.info(expression, "Expression is constant and will not be power-assert transformed")
      return super.visitCall(expression)
    }

    val symbol = currentScope!!.scope.scopeOwnerSymbol
    DeclarationIrBuilder(context, symbol).run {
      at(expression)

      val generator = object : DiagramGenerator() {
        override fun IrBuilderWithScope.buildCall(argument: IrExpression, variables: List<IrTemporaryVariable>): IrExpression {

          val lambda = messageArgument?.asSimpleLambda()
          val title = when {
            messageArgument is IrConst<*> -> messageArgument
            messageArgument is IrStringConcatenation -> messageArgument
            lambda != null -> lambda.deepCopyWithSymbols(parent).inline(parent).transform(ReturnableBlockTransformer(context, symbol), null)
            messageArgument != null -> {
              val invoke = messageArgument.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
              irCallOp(invoke.symbol, invoke.returnType, messageArgument)
            }
            // TODO what should the default message be?
            assertionArgument.type.isBoolean() -> irString("Assertion failed")
            else -> null
          }

          val prefix = title?.deepCopyWithSymbols(parent)
          val diagram = irDiagram(file, fileSource, prefix, expression, variables)
          return delegate.buildCall(this, expression, argument, diagram)
        }
      }

//      println(root.dump())
      return generator.buildExpression(this, root)
//        .also { println(expression.dump()) }
//        .also { println(it.dump()) }
//        .also { println(expression.dumpKotlinLike()) }
//        .also { println(it.dumpKotlinLike()) }
    }
  }

  private interface FunctionDelegate {
    fun buildCall(builder: IrBuilderWithScope, original: IrCall, argument: IrExpression, message: IrExpression): IrExpression
  }

  private fun findDelegate(function: IrFunction): FunctionDelegate? {
    if (function.valueParameters.isEmpty()) return null

    return context.referenceFunctions(function.kotlinFqName)
      .mapNotNull { overload ->
        // TODO allow other signatures than (Boolean, String) and (Boolean, () -> String)
        val parameters = overload.owner.valueParameters
        if (parameters.size != 2) return@mapNotNull null
        if (!function.valueParameters[0].type.isAssignableTo(parameters[0].type)) return@mapNotNull null

        val messageParameter = parameters.last()
        return@mapNotNull when {
          isStringSupertype(messageParameter.type) -> {
            object : FunctionDelegate {
              override fun buildCall(builder: IrBuilderWithScope, original: IrCall, argument: IrExpression, message: IrExpression): IrExpression = with(builder) {
                irCall(overload, type = original.type).apply {
                  dispatchReceiver = original.dispatchReceiver?.deepCopyWithSymbols(parent)
                  extensionReceiver = original.extensionReceiver?.deepCopyWithSymbols(parent)
                  for (i in 0 until original.typeArgumentsCount) {
                    putTypeArgument(i, original.getTypeArgument(i))
                  }
                  putValueArgument(0, argument)
                  putValueArgument(1, message)
                }
              }
            }
          }
          isStringFunction(messageParameter.type) -> {
            object : FunctionDelegate {
              override fun buildCall(builder: IrBuilderWithScope, original: IrCall, argument: IrExpression, message: IrExpression): IrExpression = with(builder) {
                val scope = this
                val lambda = builder.context.irFactory.buildFun {
                  name = Name.special("<anonymous>")
                  returnType = context.irBuiltIns.stringType
                  visibility = DescriptorVisibilities.LOCAL
                  origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                }.apply {
                  val bodyBuilder = DeclarationIrBuilder(this@PowerAssertCallTransformer.context, symbol)
                  body = bodyBuilder.irBlockBody {
                    +irReturn(message)
                  }
                  parent = scope.parent
                }
                val expression = IrFunctionExpressionImpl(original.startOffset, original.endOffset, messageParameter.type, lambda, IrStatementOrigin.LAMBDA)
                irCall(overload, type = original.type).apply {
                  dispatchReceiver = original.dispatchReceiver?.deepCopyWithSymbols(parent)
                  extensionReceiver = original.extensionReceiver?.deepCopyWithSymbols(parent)
                  for (i in 0 until original.typeArgumentsCount) {
                    putTypeArgument(i, original.getTypeArgument(i))
                  }
                  putValueArgument(0, argument)
                  putValueArgument(1, expression)
                }
              }
            }
          }
          else -> {
            null
          }
        }
      }
      .singleOrNull()
  }

  private fun isStringFunction(type: IrType): Boolean =
    type.isFunctionOrKFunction() && type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))

  private fun isStringSupertype(argument: IrTypeArgument): Boolean =
    argument is IrTypeProjection && isStringSupertype(argument.type)

  private fun isStringSupertype(type: IrType): Boolean =
    context.irBuiltIns.stringType.isSubtypeOf(type, context.irBuiltIns)

  private fun IrType.isAssignableTo(type: IrType): Boolean =
    isSubtypeOf(type, context.irBuiltIns) ||
      ((type.classifierOrNull as? IrTypeParameterSymbol)?.owner?.superTypes?.all {
        isSubtypeOf(it, context.irBuiltIns)
      } ?: false)

  private fun MessageCollector.info(expression: IrElement, message: String) {
    report(expression, CompilerMessageSeverity.INFO, message)
  }

  private fun MessageCollector.warn(expression: IrElement, message: String) {
    report(expression, CompilerMessageSeverity.WARNING, message)
  }

  private fun MessageCollector.report(expression: IrElement, severity: CompilerMessageSeverity, message: String) {
    report(severity, message, expression.toCompilerMessageLocation())
  }

  private fun IrElement.toCompilerMessageLocation(): CompilerMessageLocation {
    val info = file.info(this)
    val lineContent = fileSource.substring(this)
    return CompilerMessageLocation.create(file.path, info.startLineNumber, info.startColumnNumber, lineContent)!!
  }
}
