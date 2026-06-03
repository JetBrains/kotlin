/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isStringClassType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.Name

/** Index-driven Native fork of `StringConcatenationLowering`. */
internal class NativeStringConcatenationLowering(
        val generationState: NativeGenerationState,
) : FileLoweringPass {

    private val context = generationState.context

    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.symbols

    private val typesWithSpecialAppendFunction = irBuiltIns.primitiveIrTypes + irBuiltIns.stringType
    private val nameAppend = Name.identifier("append")
    private val stringBuilder = context.symbols.stringBuilder.owner

    private val constructor = stringBuilder.constructors.single {
        it.hasShape()
    }

    private val defaultAppendFunction = stringBuilder.functions.single {
        it.name == nameAppend &&
                it.hasShape(
                        dispatchReceiver = true,
                        regularParameters = 1,
                        parameterTypes = listOf(null, context.irBuiltIns.anyType.makeNullable())
                )
    }

    private val appendFunctions: Map<IrType, IrSimpleFunction?> =
            typesWithSpecialAppendFunction.associate { type ->
                type to stringBuilder.functions.toList().atMostOne {
                    it.name == nameAppend && it.hasShape(dispatchReceiver = true, regularParameters = 1, parameterTypes = listOf(null, type))
                }
            }

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction {
        return appendFunctions[type] ?: defaultAppendFunction
    }

    override fun lower(irFile: IrFile) {
        val index = generationState.fileLowerState.irElementIndex

        index.forEach<IrStringConcatenation> { expression ->
            val scope = index.scopeOf(expression) ?: return@forEach
            val builder = context.createIrBuilder(scope, expression.startOffset, expression.endOffset)

            val arguments = expression.arguments
            val replacement: IrExpression = when {
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

                arguments.size == 2 && arguments[0].type.isStringClassType() -> {
                    val functionSymbol =
                            if (arguments[0].type.isNullable()) symbols.extensionStringPlus
                            else symbols.memberStringPlus
                    builder.irCall(functionSymbol).apply {
                        this.arguments[0] = arguments[0]
                        this.arguments[1] = arguments[1]
                    }
                }

                else -> builder.irBlock(expression) {
                    val stringBuilderImpl = createTmpVariable(irCall(constructor))
                    expression.arguments.forEach { arg ->
                        val appendFunction = typeToAppendFunction(arg.type)
                        +irCall(appendFunction).apply {
                            this.arguments[0] = irGet(stringBuilderImpl)
                            this.arguments[1] = arg
                        }
                    }
                    +irCall(symbols.memberToString).apply {
                        this.arguments[0] = irGet(stringBuilderImpl)
                    }
                }
            }

            index.spliceIfNeeded(expression, replacement, reindexNewSubtree = true)
        }
    }
}
