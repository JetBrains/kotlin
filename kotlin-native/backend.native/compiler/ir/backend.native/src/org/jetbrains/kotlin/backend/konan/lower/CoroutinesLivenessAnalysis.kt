/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.isTransparentScope
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal var IrSuspensionPoint.liveVariablesAtSuspensionPoint: List<IrVariable>? by irAttribute(copyByDefault = false)
internal var IrSuspensionPoint.visibleVariablesAtSuspensionPoint: List<IrVariable>? by irAttribute(copyByDefault = false)

/**
 * Computes live variables at suspension points.
 */
@PhasePrerequisites(NativeSuspendFunctionsLowering::class)
internal class CoroutinesLivenessAnalysis(val context: NativeGenerationState) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        LivenessAnalysis.run(irBody) { it is IrSuspensionPoint }
                .forEach { [irElement, liveVariables] ->
                    (irElement as IrSuspensionPoint).liveVariablesAtSuspensionPoint = liveVariables
                }
        context.coroutinesLivenessAnalysisPhasePerformed = true
    }
}

/**
 * Computes visible variables at suspension points.
 */
@PhasePrerequisites(NativeSuspendFunctionsLowering::class)
internal class CoroutinesLivenessAnalysisFallback(val generationState: NativeGenerationState) : BodyLoweringPass {
    private val invokeSuspendFunction = generationState.context.symbols.invokeSuspendFunction

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (generationState.coroutinesLivenessAnalysisPhasePerformed)
            return

        val thisReceiver = (container as? IrSimpleFunction)?.dispatchReceiverParameter
        if (thisReceiver == null || !container.overrides(invokeSuspendFunction.owner))
            return

        computeVisibleVariablesAtSuspensionPoints(irBody)
    }

    private fun computeVisibleVariablesAtSuspensionPoints(body: IrBody) {
        body.acceptChildrenVoid(object : IrVisitorVoid() {
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
                expression.visibleVariablesAtSuspensionPoint = visibleVariables
            }
        })
    }
}
