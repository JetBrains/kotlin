/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal val DECLARATION_ORIGIN_COROUTINE_VAR_SPILLING = IrDeclarationOriginImpl("COROUTINE_VAR_SPILLING")

internal class CoroutinesVarSpillingLowering(val generationState: NativeGenerationState) : BodyLoweringPass {
    private val context = generationState.context
    private val irFactory = context.irFactory
    private val symbols = context.ir.symbols
    private val invokeSuspendFunction = symbols.invokeSuspendFunction
    private val saveCoroutineState = symbols.saveCoroutineState
    private val restoreCoroutineState = symbols.restoreCoroutineState

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val thisReceiver = (container as? IrSimpleFunction)?.dispatchReceiverParameter
        if (thisReceiver == null || !container.overrides(invokeSuspendFunction.owner))
            return

        val coroutineClass = container.parentAsClass

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
                val liveVariables = generationState.liveVariablesAtSuspensionPoints[expression]
                        ?: generationState.visibleVariablesAtSuspensionPoints[expression]
                        ?: error("No live variables for ${container.render()} at ${expression.suspensionPointIdParameter.name}")
                expression.transformChildren(this, liveVariables)

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

internal class CoroutinesLivenessAnalysisFallback(val generationState: NativeGenerationState) : FileLoweringPass, IrElementVisitorVoid {
    private val invokeSuspendFunction = generationState.context.ir.symbols.invokeSuspendFunction

    override fun lower(irFile: IrFile) {
        if (generationState.liveVariablesAtSuspensionPoints.isEmpty())
            irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        val body = declaration.body
        if (body != null && declaration.dispatchReceiverParameter != null
                && (declaration as? IrSimpleFunction)?.overrides(invokeSuspendFunction.owner) == true
        ) {
            computeVisibleVariablesAtSuspensionPoints(body)
        }
    }

    private fun computeVisibleVariablesAtSuspensionPoints(body: IrBody) {
        body.acceptChildrenVoid(object : IrElementVisitorVoid {
            val scopeStack = mutableListOf<MutableSet<IrVariable>>(mutableSetOf())

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitContainerExpression(expression: IrContainerExpression) {
                if (!expression.isTransparentScope)
                    scopeStack.push(mutableSetOf())
                super.visitContainerExpression(expression)
                if (!expression.isTransparentScope)
                    scopeStack.pop()
            }

            override fun visitCatch(aCatch: IrCatch) {
                scopeStack.push(mutableSetOf())
                super.visitCatch(aCatch)
                scopeStack.pop()
            }

            override fun visitVariable(declaration: IrVariable) {
                super.visitVariable(declaration)
                scopeStack.peek()!!.add(declaration)
            }

            override fun visitSuspensionPoint(expression: IrSuspensionPoint) {
                // Skip suspensionPointIdParameter, because we don't want to save it.
                expression.result.acceptChildrenVoid(this)
                expression.resumeResult.acceptChildrenVoid(this)

                val visibleVariables = mutableListOf<IrVariable>()
                scopeStack.forEach { visibleVariables += it }
                generationState.visibleVariablesAtSuspensionPoints[expression] = visibleVariables
            }
        })
    }
}
