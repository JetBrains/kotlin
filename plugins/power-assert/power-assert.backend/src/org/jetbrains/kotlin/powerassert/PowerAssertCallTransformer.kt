/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.parentClassId
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.powerassert.delegate.FunctionDelegate
import org.jetbrains.kotlin.powerassert.delegate.LambdaFunctionDelegate
import org.jetbrains.kotlin.powerassert.delegate.SamConversionLambdaFunctionDelegate
import org.jetbrains.kotlin.powerassert.delegate.SimpleFunctionDelegate
import org.jetbrains.kotlin.powerassert.diagram.*

class PowerAssertCallTransformer(
    private val sourceFile: SourceFile,
    private val context: IrPluginContext,
    private val configuration: PowerAssertConfiguration,
) : IrElementTransformerVoidWithContext() {
    private val irTypeSystemContext = IrTypeSystemContextImpl(context.irBuiltIns)

    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
        val fqName = function.kotlinFqName
        if (function.parameters.isEmpty() || configuration.functions.none { fqName == it }) {
            return super.visitCall(expression)
        }

        // Find a valid delegate function or do not translate
        // TODO better way to determine which delegate to actually use
        val delegates = findDelegates(function)
        val delegate = delegates.maxByOrNull { delegate ->
            delegate.function.parameters.count { it.kind == IrParameterKind.Regular }
        }
        if (delegate == null) {
            val regularParameters = function.parameters.filter { it.kind == IrParameterKind.Regular }
            val valueTypesTruncated = regularParameters.subList(0, regularParameters.size - 1)
                .joinToString("") { it.type.render() + ", " }
            val valueTypesAll = regularParameters.joinToString("") { it.type.render() + ", " }
            configuration.messageCollector.warn(
                expression = expression,
                message = """
                  |Unable to find overload of function $fqName for power-assert transformation callable as:
                  | - $fqName(${valueTypesTruncated}String)
                  | - $fqName($valueTypesTruncated() -> String)
                  | - $fqName(${valueTypesAll}String)
                  | - $fqName($valueTypesAll() -> String)
                """.trimMargin(),
            )
            return super.visitCall(expression)
        }

        val messageArgument: IrExpression?
        val roots: List<Node?>
        if (delegate.function.parameters.size == function.parameters.size) {
            messageArgument = expression.arguments.last()
            roots = expression.arguments
                .subList(fromIndex = 0, toIndex = expression.arguments.lastIndex) // Exclude message argument.
                .map { buildTree(it) }
        } else {
            messageArgument = null
            roots = expression.arguments
                .map { buildTree(it) }
        }

        // If all roots are null, there are no transformable parameters
        if (roots.all { it == null }) {
            configuration.messageCollector.info(expression, "Expression is constant and will not be power-assert transformed")
            return super.visitCall(expression)
        }

        val symbol = currentScope!!.scope.scopeOwnerSymbol
        val builder = DeclarationIrBuilder(context, symbol, expression.startOffset, expression.endOffset)
        return builder.diagram(
            call = expression,
            delegate = delegate,
            messageArgument = messageArgument,
            roots = roots,
        )
    }

    private fun buildTree(expression: IrExpression?): Node? {
        if (expression == null) return null
        return buildTree(configuration.constTracker, sourceFile, expression)
    }

    private fun DeclarationIrBuilder.diagram(
        call: IrCall,
        delegate: FunctionDelegate,
        messageArgument: IrExpression?,
        roots: List<Node?>,
    ): IrExpression {
        fun recursive(
            index: Int,
            arguments: PersistentList<IrExpression?>,
            variables: PersistentList<IrTemporaryVariable>,
        ): IrExpression {
            if (index >= roots.size) {
                val prefix = buildMessagePrefix(messageArgument, delegate.messageParameter)
                    ?.deepCopyWithSymbols(parent)
                val diagram = irDiagramString(sourceFile, prefix, call, variables)
                return delegate.buildCall(this, call, arguments, diagram)
            } else {
                val root = roots[index]
                if (root == null) {
                    val newArguments = arguments.add(call.arguments[index])
                    return recursive(index + 1, newArguments, variables)
                } else {
                    return buildDiagramNesting(sourceFile, root, variables) { argument, newVariables ->
                        val newArguments = arguments.add(argument)
                        recursive(index + 1, newArguments, newVariables)
                    }
                }
            }
        }

        return recursive(0, persistentListOf(), persistentListOf())
    }

    private fun DeclarationIrBuilder.buildMessagePrefix(
        messageArgument: IrExpression?,
        messageParameter: IrValueParameter,
    ): IrExpression? {
        return when (messageArgument) {
            is IrConst -> messageArgument
            is IrStringConcatenation -> messageArgument
            is IrGetValue -> {
                if (messageArgument.type.isAssignableTo(context.irBuiltIns.stringType)) {
                    return messageArgument
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

    private fun findDelegates(function: IrFunction): List<FunctionDelegate> {
        val values = function.parameters
        if (values.isEmpty()) return emptyList()

        // Java static functions require searching by class
        val parentClassFunctions = (
                function.parentClassId
                    ?.let { context.referenceClass(it) }
                    ?.functions ?: emptySequence()
                )
            .filter { it.owner.kotlinFqName == function.kotlinFqName }
            .toList()
        val possible = (context.referenceFunctions(function.callableId) + parentClassFunctions)
            .distinct()

        return possible.mapNotNull { overload ->
            // Type parameters must be compatible.
            if (function.typeParameters.size != overload.owner.typeParameters.size) {
                return@mapNotNull null
            }
            for (i in function.typeParameters.indices) {
                val functionSuperTypes = function.typeParameters[i].superTypes
                val overloadDefaultType = overload.owner.typeParameters[i].defaultType
                if (functionSuperTypes.any { !it.isAssignableTo(overloadDefaultType) }) {
                    return@mapNotNull null
                }
            }

            fun IrType.remap(): IrType = remapTypeParameters(overload.owner, function)

            // Value parameters must be compatible.
            val parameters = overload.owner.parameters
            if (parameters.size !in values.size..values.size + 1) return@mapNotNull null
            for (i in values.indices) {
                val value = values[i]
                val parameter = parameters[i]
                if (value.kind != parameter.kind) return@mapNotNull null

                when (value.kind) {
                    // Regular parameters may only be assignable.
                    IrParameterKind.Regular,
                        -> if (!value.type.isAssignableTo(parameter.type.remap())) return@mapNotNull null

                    // All other parameter kinds must match exactly.
                    IrParameterKind.DispatchReceiver,
                    IrParameterKind.Context,
                    IrParameterKind.ExtensionReceiver,
                        -> if (value.type != parameter.type.remap()) return@mapNotNull null
                }
            }

            val messageParameter = parameters.last()
            if (messageParameter.kind != IrParameterKind.Regular) return@mapNotNull null
            return@mapNotNull when {
                isStringSupertype(messageParameter.type) -> SimpleFunctionDelegate(overload, messageParameter)
                isStringFunction(messageParameter.type) -> LambdaFunctionDelegate(overload, messageParameter)
                isStringJavaSupplierFunction(messageParameter.type) ->
                    SamConversionLambdaFunctionDelegate(overload, messageParameter)
                else -> null
            }
        }
    }

    private fun isStringFunction(type: IrType): Boolean =
        type.isFunctionOrKFunction() && type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))

    private fun isStringJavaSupplierFunction(type: IrType): Boolean {
        val javaSupplier = context.referenceClass(ClassId.topLevel(FqName("java.util.function.Supplier")))
        return javaSupplier != null && type.isSubtypeOfClass(javaSupplier) &&
                type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))
    }

    private fun isStringSupertype(argument: IrTypeArgument): Boolean =
        argument is IrTypeProjection && isStringSupertype(argument.type)

    private fun isStringSupertype(type: IrType): Boolean =
        context.irBuiltIns.stringType.isSubtypeOf(type, irTypeSystemContext)

    private fun IrType?.isAssignableTo(type: IrType?): Boolean {
        if (this != null && type != null) {
            if (isSubtypeOf(type, irTypeSystemContext)) return true
            val superTypes = (type.classifierOrNull as? IrTypeParameterSymbol)?.owner?.superTypes
            return superTypes != null && superTypes.all { isSubtypeOf(it, irTypeSystemContext) }
        } else {
            return this == null && type == null
        }
    }

    private fun MessageCollector.info(expression: IrElement, message: String) {
        report(expression, CompilerMessageSeverity.INFO, message)
    }

    private fun MessageCollector.warn(expression: IrElement, message: String) {
        report(expression, CompilerMessageSeverity.WARNING, message)
    }

    private fun MessageCollector.report(expression: IrElement, severity: CompilerMessageSeverity, message: String) {
        report(severity, message, sourceFile.getCompilerMessageLocation(expression))
    }
}

val IrFunction.callableId: CallableId
    get() {
        val parentClass = parent as? IrClass
        val classId = parentClass?.classId
        return if (classId != null && !parentClass.isFileClass) {
            CallableId(classId, name)
        } else {
            CallableId(parent.kotlinFqName, name)
        }
    }
