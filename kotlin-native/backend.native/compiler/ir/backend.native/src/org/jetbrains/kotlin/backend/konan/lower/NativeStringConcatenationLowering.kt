/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.FlattenStringConcatenationLowering
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.shallowCopy
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.atMostOne

/**
 * Flattens, folds, and lowers string concatenations in one traversal.
 *
 * In optimized compilations, calls that accept [Any] are immediately narrowed to calls accepting [String].
 * This makes `toString()` calls explicit for later devirtualization and escape analysis. Manually written
 * `StringBuilder.append(Any?)` calls are narrowed by the same traversal.
 */
internal class NativeStringConcatenationLowering(private val nativeContext: Context) :
        FileLoweringPass,
        IrBuildingTransformer(nativeContext) {

    private val optimizationsEnabled = nativeContext.shouldOptimize()
    private val irBuiltIns = nativeContext.irBuiltIns
    private val symbols = nativeContext.symbols

    private val typesWithSpecialAppendFunction = irBuiltIns.primitiveIrTypes + irBuiltIns.stringType

    private val nameAppend = Name.identifier("append")
    private val namePlusImpl = Name.identifier("plusImpl")

    private val string = irBuiltIns.stringClass.owner
    private val stringBuilder = symbols.stringBuilder.owner

    // TODO: Calculate and pass the resulting string length to the constructor.
    private val constructor = stringBuilder.constructors.single {
        it.hasShape()
    }

    private val defaultAppendFunction = stringBuilder.functions.single {
        it.name == nameAppend &&
                it.hasShape(
                        dispatchReceiver = true,
                        regularParameters = 1,
                        parameterTypes = listOf(null, irBuiltIns.anyNType)
                )
    }

    private val appendNullableStringFunction = if (optimizationsEnabled) {
        stringBuilder.functions.single {
            it.name == nameAppend &&
                    it.hasShape(
                            dispatchReceiver = true,
                            regularParameters = 1,
                            parameterTypes = listOf(stringBuilder.typeWith(), irBuiltIns.stringType.makeNullable())
                    )
        }
    } else null

    private val appendFunctions: Map<IrType, IrSimpleFunction?> =
            typesWithSpecialAppendFunction.associateWith { type ->
                stringBuilder.functions.toList().atMostOne {
                    it.name == nameAppend &&
                            it.hasShape(dispatchReceiver = true, regularParameters = 1, parameterTypes = listOf(null, type))
                }
            }

    private val plusImplFunction = if (optimizationsEnabled) {
        string.functions.single {
            it.name == namePlusImpl &&
                    it.hasShape(
                            dispatchReceiver = true,
                            regularParameters = 1,
                            parameterTypes = listOf(irBuiltIns.stringType, irBuiltIns.stringType)
                    )
        }
    } else null

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction =
            appendFunctions[type] ?: defaultAppendFunction

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitExpression(expression: IrExpression): IrExpression {
        val transformed = FlattenStringConcatenationLowering.flattenExpression(nativeContext, expression)
        transformed.transformChildrenVoid(this)
        return if (transformed is IrStringConcatenation) lowerStringConcatenation(transformed) else transformed
    }

    private fun lowerStringConcatenation(expression: IrStringConcatenation): IrExpression {
        builder.at(expression)
        val arguments = expression.arguments
        return when {
            arguments.isEmpty() -> builder.irString("")

            arguments.size == 1 -> {
                val argument = arguments[0]
                val functionSymbol =
                        if (argument.type.isNullable()) symbols.extensionToString
                        else symbols.memberToString
                builder.irCall(functionSymbol).apply {
                    this.arguments[0] = argument
                }
            }

            arguments.size == 2 && arguments[0].type.isStringClassType() ->
                lowerStringPlus(arguments[0], arguments[1])

            else -> builder.irBlock(expression) {
                val stringBuilderImpl = createTmpVariable(irCall(constructor))
                arguments.forEach { argument ->
                    val appendFunction = typeToAppendFunction(argument.type)
                    val narrowAppend = optimizationsEnabled && appendFunction == defaultAppendFunction
                    +irCall(if (narrowAppend) appendNullableStringFunction!! else appendFunction).apply {
                        this.arguments[0] = irGet(stringBuilderImpl)
                        this.arguments[1] = if (narrowAppend) buildArgForAppend(argument) else argument
                    }
                }
                +irCall(symbols.memberToString).apply {
                    this.arguments[0] = irGet(stringBuilderImpl)
                }
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val transformed = super.visitCall(expression)
        if (!optimizationsEnabled || transformed !== expression || expression.symbol != defaultAppendFunction.symbol) return transformed

        builder.at(expression)
        return buildConcatenationCall(
                appendNullableStringFunction!!,
                expression.arguments[0]!!,
                buildArgForAppend(expression.arguments[1]!!)
        )
    }

    private fun lowerStringPlus(receiver: IrExpression, argument: IrExpression): IrExpression {
        if (!optimizationsEnabled) {
            val functionSymbol =
                    if (receiver.type.isNullable()) symbols.extensionStringPlus
                    else symbols.memberStringPlus
            return builder.irCall(functionSymbol).apply {
                arguments[0] = receiver
                arguments[1] = argument
            }
        }

        return buildConcatenationCall(
                plusImplFunction!!,
                if (receiver.type.isNullable()) buildNullableArgToString(receiver) else receiver,
                buildNullableArgToString(argument)
        )
    }

    private fun buildConcatenationCall(
            function: IrSimpleFunction,
            receiver: IrExpression,
            argument: IrExpression,
    ): IrExpression = builder.irCall(function.symbol, function.returnType, typeArgumentsCount = 0).apply {
        arguments[0] = receiver
        arguments[1] = argument
    }

    /** Builds `if (argument == null) "null" else argument.toString()` for nullable arguments. */
    private fun buildNullableArgToString(argument: IrExpression): IrExpression =
            if (argument.type.isNullable()) {
                builder.irBlock {
                    nullableArgToStringType(argument, irBuiltIns.stringType, irString("null"))
                }
            } else {
                buildNonNullableArgToString(argument)
            }

    /** Builds `if (argument == null) null else argument.toString()` for nullable arguments. */
    private fun buildArgForAppend(argument: IrExpression): IrExpression =
            if (argument.type.isNullable()) {
                builder.irBlock {
                    nullableArgToStringType(argument, irBuiltIns.stringType.makeNullable(), irNull())
                }
            } else {
                buildNonNullableArgToString(argument)
            }

    private fun IrBlockBuilder.nullableArgToStringType(argument: IrExpression, stringType: IrType, ifNull: IrExpression) {
        val [firstExpression, secondExpression] = twoExpressionsForSubsequentUsages(argument)
        +irIfThenElse(
                stringType,
                condition = irEqeqeq(firstExpression, irNull()),
                thenPart = ifNull,
                elsePart = buildNonNullableArgToString(secondExpression),
                origin = null
        )
    }

    private fun buildNonNullableArgToString(argument: IrExpression): IrExpression {
        if (argument.type.isString() || argument.type.isNullableString()) return argument

        val callee = argument.type.classOrNull?.owner?.functions?.singleOrNull {
            it.name == OperatorNameConventions.TO_STRING && it.nonDispatchParameters.isEmpty()
        }?.symbol ?: symbols.memberToString
        return builder.irCall(callee, callee.owner.returnType, typeArgumentsCount = 0).apply {
            arguments[0] = argument
        }
    }

    private fun IrBlockBuilder.twoExpressionsForSubsequentUsages(argument: IrExpression): Pair<IrExpression, IrExpression> =
            if (argument is IrGetValue) {
                Pair(argument, argument.shallowCopy())
            } else {
                createTmpVariable(argument).let { Pair(irGet(it), irGet(it)) }
            }
}
