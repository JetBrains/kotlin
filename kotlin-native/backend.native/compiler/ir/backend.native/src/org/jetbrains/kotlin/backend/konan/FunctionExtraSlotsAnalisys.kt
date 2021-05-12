/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.descriptors.isBuiltInOperator
import org.jetbrains.kotlin.backend.konan.descriptors.isTypedIntrinsic
import org.jetbrains.kotlin.backend.konan.ir.isOverridable
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.ObjectStorageKind
import org.jetbrains.kotlin.backend.konan.llvm.SlotType
import org.jetbrains.kotlin.backend.konan.llvm.storageKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class FunctionSlotsAnalysisVisitor(val context: Context, val expectedSlotsCount: MutableMap<IrFunction, Int>,
                                            val lifetimes: Map<IrElement, Lifetime>) : IrElementVisitorVoid {
    private var currentFunction: IrFunction? = null

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        expectedSlotsCount[declaration] = 0
        currentFunction = declaration
        super.visitFunction(declaration)
        currentFunction = null
    }

    private fun incrementSlotsCount() {
        if (currentFunction != null) {
            expectedSlotsCount[currentFunction!!] = expectedSlotsCount[currentFunction]!! + 1
        }
    }

    private fun visitIrFunctionAccessExpression(expression: IrFunctionAccessExpression) {
        val slotType = lifetimes.getOrElse(expression) { Lifetime.GLOBAL }.slotType
        if (slotType == SlotType.ANONYMOUS || slotType == SlotType.STACK) {
            incrementSlotsCount()
        }
    }

    override fun visitCall(expression: IrCall) {
        val calledFunction = expression.symbol.owner
        val isVirtualCall = expression.superQualifierSymbol?.owner == null && calledFunction.isOverridable
        if (isVirtualCall || calledFunction.annotations.findAnnotation(RuntimeNames.filterExceptions) != null || calledFunction.isSuspend) {
            incrementSlotsCount()
        }

        var type = calledFunction.returnType
        if (calledFunction.isFakeOverride) {
            val realFunction = calledFunction.resolveFakeOverride()
            realFunction?.returnType?.let { type = it }
        }

        val isIntrinsicWithoutAlloc = calledFunction.isTypedIntrinsic
                && getIntrinsicType(expression) != IntrinsicType.CREATE_UNINITIALIZED_INSTANCE

        if (!isIntrinsicWithoutAlloc && !calledFunction.isBuiltInOperator && !type.isVoidAsReturnType()
                && type.computePrimitiveBinaryTypeOrNull() == null) {
            visitIrFunctionAccessExpression(expression)
        }
        super.visitCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        if (!expression.type.isNothing() && expression.type.computePrimitiveBinaryTypeOrNull() == null) {
            visitIrFunctionAccessExpression(expression)
        }

        super.visitConstructorCall(expression)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        val irClass = expression.symbol.owner
        if (irClass.isUnit()) return
        val storageKind = irClass.storageKind(context)
        if (storageKind != ObjectStorageKind.PERMANENT) {
            incrementSlotsCount()
        }

        super.visitGetObjectValue(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (context.shouldContainDebugInfo() ||
                (declaration.isVar || declaration.initializer == null) && declaration.type.computePrimitiveBinaryTypeOrNull() == null) {
            incrementSlotsCount()
        }

        super.visitVariable(declaration)
    }

    override fun visitGetField(expression: IrGetField) {
        if (!expression.symbol.owner.isStatic) {
            if (!expression.symbol.owner.isFinal && expression.symbol.owner.type.computePrimitiveBinaryTypeOrNull() == null) {
                incrementSlotsCount()
            }
        } else if (expression.symbol.owner.correspondingPropertySymbol?.owner?.isConst != true && !expression.symbol.owner.isFinal) {
            incrementSlotsCount()
        }

        super.visitGetField(expression)
    }
}