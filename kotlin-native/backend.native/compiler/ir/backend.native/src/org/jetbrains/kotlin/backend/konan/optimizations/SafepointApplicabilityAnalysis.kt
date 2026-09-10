/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.konan.NativeLoweringContext
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/**
 * `true`, when a function is simple enough to not need any safepoints (neither in the prologue, nor on loop back edges)
 */
internal var IrSimpleFunction.canOmitSafepoints: Boolean by irFlag(copyByDefault = false)

/**
 * Analyzes if a function is simple enough to skip safepoints.
 *
 * Sets [canOmitSafepoints] flag to be read by the code generator.
 */
internal class SafepointApplicabilityAnalysis(val context: NativeLoweringContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrSimpleFunction)
            return
        val canOmitSafepoints = irBody.accept(CanOmitSafepointsVisitor(context.symbols, context.irBuiltIns), null)
        if (canOmitSafepoints) {
            context.log {
                "Can omit safepoints in ${container.kotlinFqName}: ${irBody.dump()}"
            }
        }
        container.canOmitSafepoints = canOmitSafepoints
    }
}

private class CanOmitSafepointsVisitor(private val symbols: BackendNativeSymbols, private val irBuiltIns: IrBuiltIns) : IrVisitor<Boolean, Nothing?>() {
    override fun visitElement(element: IrElement, data: Nothing?): Boolean {
        // Only explicitly allowed cases below are allowed
        return false
    }

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?) = body.statements.all { it.accept(this, data) }
    override fun visitReturn(expression: IrReturn, data: Nothing?) = expression.value.accept(this, data)
    override fun visitGetField(expression: IrGetField, data: Nothing?) = expression.receiver?.accept(this, data) ?: true
    override fun visitSetField(expression: IrSetField, data: Nothing?) = listOfNotNull(expression.receiver, expression.value).all { it.accept(this, data) }
    override fun visitGetValue(expression: IrGetValue, data: Nothing?) = true
    override fun visitSetValue(expression: IrSetValue, data: Nothing?) = expression.value.accept(this, data)
    override fun visitWhen(expression: IrWhen, data: Nothing?) = expression.branches.all { it.accept(this, data) }
    override fun visitBranch(branch: IrBranch, data: Nothing?) = listOf(branch.condition, branch.result).all { it.accept(this, data) }
    override fun visitConst(expression: IrConst, data: Nothing?) = true

    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        // Calls in general are not allowed, because they may lead (indirectly) to recursion
        // But some calls (e.g. well known intrinsics), are explicitly allowed
        val allowedCalls = setOf(
                irBuiltIns.eqeqeqSymbol,
                symbols.theUnitInstance,
        )
        if (!allowedCalls.contains(expression.symbol)) {
            return false
        }
        return expression.arguments.all { it?.accept(this, data) ?: true }
    }
}