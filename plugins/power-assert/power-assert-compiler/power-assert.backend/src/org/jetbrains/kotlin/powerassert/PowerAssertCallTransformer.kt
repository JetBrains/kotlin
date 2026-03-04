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
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.parentClassId
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.powerassert.builder.call.CallBuilder
import org.jetbrains.kotlin.powerassert.builder.call.LambdaCallBuilder
import org.jetbrains.kotlin.powerassert.builder.call.SamConversionLambdaCallBuilder
import org.jetbrains.kotlin.powerassert.builder.call.SimpleCallBuilder
import org.jetbrains.kotlin.powerassert.builder.parameter.ParameterBuilder
import org.jetbrains.kotlin.powerassert.builder.parameter.StringParameterBuilder
import org.jetbrains.kotlin.powerassert.diagram.*
import org.jetbrains.kotlin.powerassert.diagram.buildTree

class PowerAssertCallTransformer(
    private val sourceFile: SourceFile,
    private val context: IrPluginContext,
    private val configuration: PowerAssertConfiguration,
) : IrElementTransformerVoidWithContext() {
    private val irTypeSystemContext = IrTypeSystemContextImpl(context.irBuiltIns)

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val function = expression.symbol.owner
        val fqName = function.kotlinFqName
        return when {
            function.parameters.isEmpty() -> expression
            fqName in configuration.functions -> buildForOverride(expression, function)
            else -> expression
        }
    }

    private fun buildForOverride(originalCall: IrCall, function: IrSimpleFunction): IrExpression {
        // Find a valid delegate function or do not translate
        // TODO better way to determine which delegate to actually use
        val callBuilders = findCallBuilders(function, originalCall)
        val callBuilder = callBuilders.maxByOrNull { builder ->
            builder.targetFunction.parameters.count { it.kind == IrParameterKind.Regular }
        }

        if (callBuilder == null) {
            val fqName = function.kotlinFqName
            val regularParameters = function.parameters.filter { it.kind == IrParameterKind.Regular }
            val valueTypesTruncated = regularParameters.subList(0, regularParameters.size - 1)
                .joinToString("") { it.type.render() + ", " }
            val valueTypesAll = regularParameters.joinToString("") { it.type.render() + ", " }
            configuration.messageCollector.warn(
                expression = originalCall,
                message = """
                  |Unable to find overload of function $fqName for power-assert transformation callable as:
                  | - $fqName(${valueTypesTruncated}String)
                  | - $fqName($valueTypesTruncated() -> String)
                  | - $fqName(${valueTypesAll}String)
                  | - $fqName($valueTypesAll() -> String)
                """.trimMargin(),
            )
            return super.visitCall(originalCall)
        }

        val roots = buildArgumentRoots(callBuilder, function, originalCall)

        // If all roots are non-visible, there are no transformable parameters
        if (roots.all { !it.isVisible() }) {
            configuration.messageCollector.info(originalCall, "Expression is constant and will not be power-assert transformed")
            return super.visitCall(originalCall)
        }

        val messageArgument = when (callBuilder.targetFunction.parameters.size) {
            function.parameters.size -> originalCall.arguments.last()
            else -> null
        }
        val parameterBuilder = StringParameterBuilder(sourceFile, originalCall, function, messageArgument)
        return buildPowerAssertCall(originalCall, callBuilder, parameterBuilder, roots)
    }

    private fun buildArgumentRoots(
        callBuilder: CallBuilder,
        function: IrSimpleFunction,
        originalCall: IrCall,
    ): List<RootNode<IrValueParameter>> {
        val argumentCount = when (callBuilder.targetFunction.parameters.size) {
            function.parameters.size -> originalCall.arguments.size - 1
            else -> originalCall.arguments.size
        }
        return (0..<argumentCount).map {
            buildTree(configuration.constTracker, sourceFile, function.parameters[it], originalCall.arguments[it])
        }
    }

    private fun buildPowerAssertCall(
        originalCall: IrCall,
        callBuilder: CallBuilder,
        parameterBuilder: ParameterBuilder,
        roots: List<RootNode<IrValueParameter>>,
    ): IrExpression {
        val symbol = currentScope!!.scope.scopeOwnerSymbol
        val builder = DeclarationIrBuilder(context, symbol, originalCall.startOffset, originalCall.endOffset)

        fun recursive(
            index: Int,
            arguments: PersistentList<IrExpression?>,
            argumentVariables: PersistentMap<IrValueParameter, List<IrDiagramVariable>>,
        ): IrExpression {
            if (index >= roots.size) {
                val diagram = parameterBuilder.build(builder, argumentVariables)
                return callBuilder.buildCall(builder, arguments, diagram)
            } else {
                val root = roots[index]
                val child = root.child
                if (child == null) {
                    val newArguments = arguments.add(originalCall.arguments[index])
                    return recursive(index + 1, newArguments, argumentVariables)
                } else {
                    return builder.buildDiagramNesting(sourceFile, child) { argument, newVariables ->
                        val newArguments = arguments.add(argument)
                        val newArgumentVariables = argumentVariables.put(root.parameter, newVariables)
                        recursive(index + 1, newArguments, newArgumentVariables)
                    }
                }
            }
        }

        return recursive(0, persistentListOf(), persistentMapOf())
    }

    private fun findCallBuilders(function: IrFunction, original: IrCall): List<CallBuilder> {
        val values = function.parameters
        if (values.isEmpty()) return emptyList()

        val finder = context.finderForSource(sourceFile.irFile)
        // Java static functions require searching by class
        val parentClassFunctions = (
                function.parentClassId
                    ?.let { finder.findClass(it) }
                    ?.functions ?: emptySequence()
                )
            .filter { it.owner.kotlinFqName == function.kotlinFqName }
            .toList()
        val possible = (finder.findFunctions(function.callableId) + parentClassFunctions)
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
            val messageType = messageParameter.type
            return@mapNotNull when {
                isStringSupertype(messageType) -> SimpleCallBuilder(overload, original)
                isStringFunction(messageType) -> LambdaCallBuilder(overload, original, messageType)
                isStringJavaSupplierFunction(messageType) -> SamConversionLambdaCallBuilder(overload, original, messageType)
                else -> null
            }
        }
    }

    private fun isStringFunction(type: IrType): Boolean =
        type.isFunctionOrKFunction() && type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))

    private fun isStringJavaSupplierFunction(type: IrType): Boolean {
        val javaSupplier = context.finderForBuiltins().findClass(ClassId.topLevel(FqName("java.util.function.Supplier")))
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
