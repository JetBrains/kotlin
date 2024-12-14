/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicHandlerType
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder

class NativeAtomicfuIrBuilder(
    override val atomicfuSymbols: NativeAtomicSymbols,
    symbol: IrSymbol,
): AbstractAtomicfuIrBuilder(atomicfuSymbols.irBuiltIns, symbol) {

    override fun irCallFunction(
        symbol: IrSimpleFunctionSymbol,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArguments: List<IrExpression?>,
        valueType: IrType
    ): IrCall = irCall(symbol).apply {
        val isAtomicArrayHandler = dispatchReceiver != null && atomicfuSymbols.isAtomicArrayHandlerType(dispatchReceiver.type)
        val irCall = irCall(symbol).apply {
            this.dispatchReceiver = dispatchReceiver
            this.extensionReceiver = extensionReceiver
            if (symbol.owner.typeParameters.isNotEmpty()) {
                require(symbol.owner.typeParameters.size == 1) { "Only K/N atomic intrinsics are parameterized with a type of the updated volatile field. A function with more type parameters is being invoked: ${symbol.owner.render()}" }
                typeArguments[0] = valueType
            }
            valueArguments.forEachIndexed { i, arg ->
                if (isAtomicArrayHandler && valueType.isBoolean() && i != 0) {
                    putValueArgument(i, arg?.let { toInt(it) })
                } else {
                    putValueArgument(i, arg)
                }
            }
        }
        return if (isAtomicArrayHandler && valueType.isBoolean() && symbol.owner.returnType.isInt()) toBoolean(irCall) else irCall
    }

    override fun invokeFunctionOnAtomicHandler(
        atomicHandlerType: AtomicHandlerType,
        getAtomicHandler: IrExpression,
        functionName: String,
        valueArguments: List<IrExpression?>,
        valueType: IrType,
    ): IrCall =
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_ARRAY -> {
                invokeFunctionOnAtomicHandlerClass(getAtomicHandler, functionName, valueArguments, valueType)
            }
            AtomicHandlerType.NATIVE_PROPERTY_REF -> {
                when (functionName) {
                    "get", "<get-value>" -> atomicGetField(getAtomicHandler, valueType)
                    "set", "<set-value>", "lazySet" -> atomicSetField(getAtomicHandler, valueType, valueArguments[0])
                    "compareAndSet" -> compareAndSetField(getAtomicHandler, valueType, valueArguments[0], valueArguments[1])
                    "getAndSet" -> getAndSetField(getAtomicHandler, valueType, valueArguments[0])
                    "getAndAdd" -> getAndAddField(getAtomicHandler, valueType, valueArguments[0])
                    "getAndIncrement" -> getAndIncrementField(getAtomicHandler, valueType)
                    "getAndDecrement" -> getAndDecrementField(getAtomicHandler, valueType)
                    "addAndGet" -> addAndGetField(getAtomicHandler, valueType, valueArguments[0])
                    "incrementAndGet" -> incrementAndGetField(getAtomicHandler, valueType)
                    "decrementAndGet" -> decrementAndGetField(getAtomicHandler, valueType)
                    else -> error("Unsupported atomic function name $functionName")
                }
            }
            else -> error("Unexpected atomic handler type: $atomicHandlerType for the Native backend.")
        }

    override fun buildVolatileFieldOfType(
        name: String,
        valueType: IrType,
        annotations: List<IrConstructorCall>,
        initExpr: IrExpression?,
        parentContainer: IrDeclarationContainer
    ): IrField =
        // On K/N a volatile Boolean field is generated instead of an AtomicBoolean property
        irVolatileField(name, valueType, annotations, parentContainer).apply {
            if (initExpr != null) {
                this.initializer = context.irFactory.createExpressionBody(initExpr)
            }
        }

    private fun atomicGetField(propertyRef: IrExpression, valueType: IrType): IrCall =
        callNativeAtomicIntrinsic(
            propertyRef = propertyRef,
            receiverType = valueType,
            symbol = atomicfuSymbols.nativeAtomicGetFieldIntrinsic
        )

    private fun atomicSetField(propertyRef: IrExpression, valueType: IrType, newValue: IrExpression?): IrCall =
        callNativeAtomicIntrinsic(
            propertyRef = propertyRef,
            receiverType = valueType,
            symbol = atomicfuSymbols.nativeAtomicSetFieldIntrinsic,
            newValue
        )

    private fun compareAndSetField(propertyRef: IrExpression, valueType: IrType, expected: IrExpression?, updated: IrExpression?) =
        callNativeAtomicIntrinsic(propertyRef, valueType, atomicfuSymbols.nativeCompareAndSetFieldIntrinsic, expected, updated)

    private fun getAndSetField(propertyRef: IrExpression, valueType: IrType, value: IrExpression?) =
        callNativeAtomicIntrinsic(propertyRef, valueType, atomicfuSymbols.nativeGetAndSetFieldIntrinsic, value)

    private fun getAndAddField(propertyRef: IrExpression, valueType: IrType, delta: IrExpression?): IrCall =
        when {
            valueType.isInt() ->
                callNativeAtomicIntrinsic(propertyRef, valueType, atomicfuSymbols.nativeGetAndAddIntFieldIntrinsic, delta)
            valueType.isLong() ->
                callNativeAtomicIntrinsic(
                    propertyRef,
                    valueType,
                    atomicfuSymbols.nativeGetAndAddLongFieldIntrinsic,
                    delta?.implicitCastTo(context.irBuiltIns.longType)
                )
            else -> error("kotlin.native.internal/getAndAddField intrinsic is not supported for values of type ${valueType.dumpKotlinLike()}")
        }

    private fun addAndGetField(propertyRef: IrExpression, valueType: IrType, delta: IrExpression?): IrCall =
        getAndAddField(propertyRef, valueType, delta).plus(delta?.deepCopyWithoutPatchingParents())

    private fun getAndIncrementField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(1) else irLong(1)
        return getAndAddField(propertyRef, valueType, delta)
    }

    private fun getAndDecrementField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(-1) else irLong(-1)
        return getAndAddField(propertyRef, valueType, delta)
    }

    private fun incrementAndGetField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(1) else irLong(1)
        return addAndGetField(propertyRef, valueType, delta)
    }

    private fun decrementAndGetField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(-1) else irLong(-1)
        return addAndGetField(propertyRef, valueType, delta)
    }

    fun IrCall.plus(other: IrExpression?): IrCall {
        val returnType = this.symbol.owner.returnType
        val plusOperatorSymbol = when {
            returnType.isInt() -> atomicfuSymbols.intPlusOperator
            returnType.isLong() -> atomicfuSymbols.longPlusOperator
            else -> error("Return type of the function ${this.symbol.owner.dump()} is expected to be Int or Long, but found $returnType")
        }
        return irCall(plusOperatorSymbol).apply {
            dispatchReceiver = this@plus
            putValueArgument(0, other)
        }
    }

    private fun callNativeAtomicIntrinsic(
        propertyRef: IrExpression,
        receiverType: IrType,
        symbol: IrSimpleFunctionSymbol,
        vararg valueArguments: IrExpression?
    ): IrCall = irCallFunction(
        symbol = symbol,
        dispatchReceiver = null,
        extensionReceiver = propertyRef,
        valueArguments = valueArguments.toList(),
        valueType = receiverType
    )

    override fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?
    ): IrFunctionAccessExpression {
        if (valueType.isPrimitiveType()) {
            callArraySizeConstructor(atomicArrayClass, size, dispatchReceiver)
        } else {
            callArraySizeAndInitConstructor(atomicArrayClass, size, valueType, dispatchReceiver)
        }?.let { return it }
        error("Failed to find a constructor for the the given atomic array type ${atomicArrayClass.defaultType.render()}.")
    }

}