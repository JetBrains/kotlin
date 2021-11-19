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

import com.bnorm.power.delegate.FunctionDelegate
import com.bnorm.power.delegate.LambdaFunctionDelegate
import com.bnorm.power.delegate.SimpleFunctionDelegate
import com.bnorm.power.diagram.IrTemporaryVariable
import com.bnorm.power.diagram.Node
import com.bnorm.power.diagram.buildDiagramNesting
import com.bnorm.power.diagram.buildTree
import com.bnorm.power.diagram.info
import com.bnorm.power.diagram.irDiagramString
import com.bnorm.power.diagram.substring
import com.bnorm.power.internal.ReturnableBlockTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.asInlinable
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.FqName

class PowerAssertCallTransformer(
  private val file: IrFile,
  private val fileSource: String,
  private val context: IrPluginContext,
  private val messageCollector: MessageCollector,
  private val functions: Set<FqName>
) : IrElementTransformerVoidWithContext() {
  private val irTypeSystemContext = IrTypeSystemContextImpl(context.irBuiltIns)

  override fun visitCall(expression: IrCall): IrExpression {
    val function = expression.symbol.owner
    val fqName = function.kotlinFqName
    if (function.valueParameters.isEmpty() || functions.none { fqName == it }) {
      return super.visitCall(expression)
    }

    // Find a valid delegate function or do not translate
    // TODO better way to determine which delegate to actually use
    val delegates = findDelegates(function)
    val delegate = delegates.maxByOrNull { it.function.valueParameters.size }
    if (delegate == null) {
      val valueTypesTruncated = function.valueParameters.subList(0, function.valueParameters.size - 1)
        .joinToString("") { it.type.asString() + ", " }
      val valueTypesAll = function.valueParameters.joinToString("") { it.type.asString() + ", " }
      messageCollector.warn(
        expression = expression,
        message = """
          |Unable to find overload of function $fqName for power-assert transformation callable as:
          | - $fqName(${valueTypesTruncated}String)
          | - $fqName($valueTypesTruncated() -> String)
          | - $fqName(${valueTypesAll}String)
          | - $fqName($valueTypesAll() -> String)
        """.trimMargin()
      )
      return super.visitCall(expression)
    }

    val messageArgument: IrExpression?
    val roots: List<Node?>
    if (delegate.function.valueParameters.size == function.valueParameters.size) {
      messageArgument = expression.getValueArgument(expression.valueArgumentsCount - 1)
      roots = (0 until expression.valueArgumentsCount - 1)
        .map { index -> expression.getValueArgument(index) }
        .map { arg -> arg?.let { buildTree(it) } }
    } else {
      messageArgument = null
      roots = (0 until expression.valueArgumentsCount)
        .map { index -> expression.getValueArgument(index) }
        .map { arg -> arg?.let { buildTree(it) } }
    }

    // If all roots are null, there are no transformable parameters
    if (roots.all { it == null }) {
      messageCollector.info(expression, "Expression is constant and will not be power-assert transformed")
      return super.visitCall(expression)
    }

    val symbol = currentScope!!.scope.scopeOwnerSymbol
    val builder = DeclarationIrBuilder(context, symbol, expression.startOffset, expression.endOffset)
    return builder.diagram(expression, delegate, messageArgument, roots)
//        .also { println(expression.dump()) }
//        .also { println(it.dump()) }
//        .also { println(expression.dumpKotlinLike()) }
//        .also { println(it.dumpKotlinLike()) }
  }

  private fun DeclarationIrBuilder.diagram(
    original: IrCall,
    delegate: FunctionDelegate,
    messageArgument: IrExpression?,
    roots: List<Node?>,
    index: Int = 0,
    arguments: List<IrExpression?> = listOf(),
    variables: List<IrTemporaryVariable> = listOf()
  ): IrExpression {
    if (index >= roots.size) {
      val prefix = buildMessagePrefix(messageArgument, roots, original)?.deepCopyWithSymbols(parent)
      val diagram = irDiagramString(file, fileSource, prefix, original, variables)
      return delegate.buildCall(this, original, arguments, diagram)
    } else {
      val root = roots[index]
      if (root == null) {
        val newArguments = arguments + original.getValueArgument(index)
        return diagram(original, delegate, messageArgument, roots, index + 1, newArguments, variables)
      } else {
        return buildDiagramNesting(root) { argument, newVariables ->
          val newArguments = arguments + argument
          diagram(original, delegate, messageArgument, roots, index + 1, newArguments, variables + newVariables)
        }
      }
    }
  }

  private fun DeclarationIrBuilder.buildMessagePrefix(
    messageArgument: IrExpression?,
    roots: List<Node?>,
    original: IrCall
  ): IrExpression? {
    return when {
      messageArgument is IrConst<*> -> messageArgument
      messageArgument is IrStringConcatenation -> messageArgument
      messageArgument != null -> irBlock {
        +messageArgument.deepCopyWithSymbols(parent)
          .asInlinable(this)
          .inline(parent)
          .patchDeclarationParents(scope.getLocalDeclarationParent())
      }.transform(ReturnableBlockTransformer(context, scope.scopeOwnerSymbol), null)
      // TODO what should the default message be?
      roots.size == 1 && original.getValueArgument(0)!!.type.isBoolean() -> irString("Assertion failed")
      else -> null
    }
  }

  private fun findDelegates(function: IrFunction): List<FunctionDelegate> {
    val values = function.valueParameters
    if (values.isEmpty()) return emptyList()

    return context.referenceFunctions(function.kotlinFqName)
      .mapNotNull { overload ->
        val parameters = overload.owner.valueParameters
        if (parameters.size !in values.size..values.size + 1) return@mapNotNull null
        if (!parameters.zip(values).all { (param, value) -> value.type.isAssignableTo(param.type) }) {
          return@mapNotNull null
        }

        val messageParameter = parameters.last()
        return@mapNotNull when {
          isStringSupertype(messageParameter.type) -> SimpleFunctionDelegate(overload)
          isStringFunction(messageParameter.type) -> LambdaFunctionDelegate(overload, messageParameter)
          else -> null
        }
      }
  }

  private fun isStringFunction(type: IrType): Boolean =
    type.isFunctionOrKFunction() && type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))

  private fun isStringSupertype(argument: IrTypeArgument): Boolean =
    argument is IrTypeProjection && isStringSupertype(argument.type)

  private fun isStringSupertype(type: IrType): Boolean =
    context.irBuiltIns.stringType.isSubtypeOf(type, irTypeSystemContext)

  private fun IrType.isAssignableTo(type: IrType): Boolean {
    if (isSubtypeOf(type, irTypeSystemContext)) return true
    val superTypes = (type.classifierOrNull as? IrTypeParameterSymbol)?.owner?.superTypes
    return superTypes != null && superTypes.all { isSubtypeOf(it, irTypeSystemContext) }
  }

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
