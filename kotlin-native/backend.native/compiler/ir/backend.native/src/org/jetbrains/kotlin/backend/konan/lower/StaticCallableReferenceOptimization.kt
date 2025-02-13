/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.NativeFunctionReferenceLowering.Companion.isLoweredFunctionReference
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irConstantObject
import org.jetbrains.kotlin.ir.builders.irConstantPrimitive
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Function references are lowered into instantion of on object of some "callable" type.
 * e.g. `call(::foo)` will be lowered to something like `call(foo$FUNCTION_REFERENCE$0())`.
 *
 * In some cases the object instantiated doesn't capture any context and in fact can be replaced with a constant object.
 * That's what this optimization pass does.
 */
internal class StaticCallableReferenceOptimization(val context: Context) : FileLoweringPass {
    private val allPropertyReferenceSymbols = buildList {
        val immutableSymbols = context.ir.symbols.immutablePropertiesConstructors
        addAll(immutableSymbols.byRecieversCount)
        add(immutableSymbols.local)

        val mutableSymbols = context.ir.symbols.mutablePropertiesConstructors
        addAll(mutableSymbols.byRecieversCount)
        add(mutableSymbols.local)
    }.toSet()

    private fun IrStatement.isEmptyBlock(): Boolean = this is IrContainerExpression && statements.all { it is IrBlock && it !is IrReturnableBlock && it.isEmptyBlock() }

    private fun IrBuilderWithScope.tryConvertToConst(expression: IrExpression?): IrConstantValue? = when (expression) {
        is IrConst -> irConstantPrimitive(expression)
        is IrReturnableBlock -> null
        is IrBlock -> {
            val singleExpression = if (expression.statements.count { !it.isEmptyBlock() } != 1)
                null
            else
                expression.statements.lastOrNull() as? IrExpression
            singleExpression?.let { tryConvertToConst(it) }
        }
        is IrConstantValue -> expression
        else -> null
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val constructor = expression.symbol.owner
                val constructedClass = constructor.constructedClass

                return if ((isLoweredFunctionReference(constructedClass) && constructor.parameters.isEmpty()) || expression.symbol in allPropertyReferenceSymbols) {
                    context.createIrBuilder(
                            currentScope!!.scope.scopeOwnerSymbol,
                            expression.startOffset,
                            expression.endOffset
                    ).run {
                        val args = expression.getArgumentsWithIr().map { tryConvertToConst(it.second) ?: return expression }
                        irConstantObject(expression.symbol, args, expression.getClassTypeArguments().map { it!! })
                    }
                } else {
                    expression
                }
            }
        }, data = null)
    }
}