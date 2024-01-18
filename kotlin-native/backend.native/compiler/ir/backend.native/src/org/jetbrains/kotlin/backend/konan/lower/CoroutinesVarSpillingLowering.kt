/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal val DECLARATION_ORIGIN_COROUTINE_VAR_SPILLING = IrDeclarationOriginImpl("COROUTINE_VAR_SPILLING")

internal class CoroutinesVarSpillingLowering(val context: Context) : BodyLoweringPass {
    private val irFactory = context.irFactory
    private val symbols = context.ir.symbols
    private val saveCoroutineState = symbols.saveCoroutineState
    private val restoreCoroutineState = symbols.restoreCoroutineState

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val thisReceiver = (container as? IrSimpleFunction)?.dispatchReceiverParameter
        if (thisReceiver == null || !container.overrides(context.ir.symbols.invokeSuspendFunction.owner))
            return

        val coroutineClass = container.parentAsClass
        val liveLocals = LivenessAnalysis.run(irBody) { it is IrSuspensionPoint }

        // TODO: optimize by using the same property for different locals.
        val localToPropertyMap = mutableMapOf<IrVariableSymbol, IrField>()
        fun getFieldForSpilling(variable: IrVariable) = localToPropertyMap.getOrPut(variable.symbol) {
            variable.isVar = true // Make variables mutable in order to save/restore them.
            irFactory.buildField {
                startOffset = coroutineClass.startOffset
                endOffset = coroutineClass.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_VAR_SPILLING
                name = variable.name
                type = variable.type
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = false
            }.apply {
                coroutineClass.addChild(this)
            }
        }

        // Save/restore state at suspension points.
        val irBuilder = context.createIrBuilder(container.symbol, container.startOffset, container.endOffset)
        irBody.transformChildren(object : IrElementTransformer<List<IrVariable>> {
            override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: List<IrVariable>): IrExpression {
                expression.transformChildren(this, liveLocals[expression]!!)

                return expression
            }

            override fun visitCall(expression: IrCall, data: List<IrVariable>): IrExpression {
                expression.transformChildren(this, data)

                return when (expression.symbol) {
                    saveCoroutineState -> irBuilder.run {
                        irBlock(expression) {
                            for (variable in data) {
                                val field = getFieldForSpilling(variable)
                                +irSetField(irGet(thisReceiver), field, irGet(variable))
                            }
                        }
                    }
                    restoreCoroutineState -> irBuilder.run {
                        irBlock(expression) {
                            for (variable in data) {
                                val field = getFieldForSpilling(variable)
                                +irSet(variable, irGetField(irGet(thisReceiver), field))
                            }
                        }
                    }
                    else -> expression
                }
            }
        }, data = emptyList())
    }
}