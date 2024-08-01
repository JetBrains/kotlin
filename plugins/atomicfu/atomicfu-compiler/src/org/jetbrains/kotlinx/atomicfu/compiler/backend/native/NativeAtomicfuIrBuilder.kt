/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder

class NativeAtomicfuIrBuilder(
    override val atomicSymbols: NativeAtomicSymbols,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
): AbstractAtomicfuIrBuilder(atomicSymbols.irBuiltIns, symbol, startOffset, endOffset) {

    internal fun irCallAtomicNativeIntrinsic(
        functionName: String,
        propertyRef: IrExpression,
        valueType: IrType,
        valueArguments: List<IrExpression?>
    ): IrExpression = when (functionName) {
        "<get-value>" -> atomicGetField(propertyRef, valueType)
        "<set-value>", "lazySet" -> atomicSetField(propertyRef, valueType, valueArguments[0])
        "compareAndSet" -> compareAndSetField(propertyRef, valueType, valueArguments[0], valueArguments[1])
        "getAndSet" -> getAndSetField(propertyRef, valueType, valueArguments[0])
        "getAndAdd" -> getAndAddField(propertyRef, valueType, valueArguments[0])
        "getAndIncrement" -> getAndIncrementField(propertyRef, valueType)
        "getAndDecrement" -> getAndDecrementField(propertyRef, valueType)
        "addAndGet" -> addAndGetField(propertyRef, valueType, valueArguments[0])
        "incrementAndGet" -> incrementAndGetField(propertyRef, valueType)
        "decrementAndGet" -> decrementAndGetField(propertyRef, valueType)
        else -> error("Unsupported atomic function name $functionName")
    }

    private fun atomicGetField(receiver: IrExpression?, valueType: IrType): IrCall =
        irDelegatedAtomicfuCall(
            symbol = atomicSymbols.atomicGetFieldIntrinsic,
            dispatchReceiver = null,
            extensionReceiver = receiver,
            valueArguments = emptyList(),
            receiverValueType = valueType
        )

    private fun atomicSetField(receiver: IrExpression?, valueType: IrType, newValue: IrExpression?): IrCall =
        irDelegatedAtomicfuCall(
            symbol = atomicSymbols.atomicSetFieldIntrinsic,
            dispatchReceiver = null,
            extensionReceiver = receiver,
            valueArguments = listOf(newValue),
            receiverValueType = valueType
        )

    private fun compareAndSetField(propertyRef: IrExpression, valueType: IrType, expected: IrExpression?, updated: IrExpression?) =
        callNativeAtomicIntrinsic(propertyRef, valueType, atomicSymbols.compareAndSetFieldIntrinsic, expected, updated)

    private fun getAndSetField(propertyRef: IrExpression, valueType: IrType, value: IrExpression?) =
        callNativeAtomicIntrinsic(propertyRef, valueType, atomicSymbols.getAndSetFieldIntrinsic, value)

    private fun getAndAddField(propertyRef: IrExpression, valueType: IrType, delta: IrExpression?): IrCall =
        when {
            valueType.isInt() ->
                callNativeAtomicIntrinsic(propertyRef, valueType, atomicSymbols.getAndAddIntFieldIntrinsic, delta)
            valueType.isLong() ->
                callNativeAtomicIntrinsic(
                    propertyRef,
                    valueType,
                    atomicSymbols.getAndAddLongFieldIntrinsic,
                    delta?.implicitCastTo(context.irBuiltIns.longType)
                )
            else -> error("kotlin.native.internal/getAndAddField intrinsic is not supported for values of type ${valueType.dumpKotlinLike()}")
        }

    private fun addAndGetField(propertyRef: IrExpression, valueType: IrType, delta: IrExpression?): IrCall =
        getAndAddField(propertyRef, valueType, delta).plus(delta)

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

    private fun callNativeAtomicIntrinsic(
        propertyRef: IrExpression,
        receiverType: IrType,
        symbol: IrSimpleFunctionSymbol,
        vararg valueArguments: IrExpression?
    ): IrCall = irDelegatedAtomicfuCall(
        symbol = symbol,
        dispatchReceiver = null,
        extensionReceiver = propertyRef,
        valueArguments = valueArguments.toList(),
        receiverValueType = receiverType
    )

    private fun invokePropertyGetter(refGetter: IrExpression) = irCall(atomicSymbols.invoke0Symbol).apply { dispatchReceiver = refGetter }

    /*
    inline fun <T> loop$atomicfu(refGetter: () -> KMutableProperty0<T>, action: (T) -> Unit) {
        while (true) {
            val cur = refGetter().get()
            action(cur)
        }
    }
    */
    override fun atomicfuLoopBody(valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody =
        irBlockBody {
            val refGetter = valueParameters[0]
            val action = valueParameters[1]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        atomicGetField(invokePropertyGetter(irGet(refGetter)), valueType),
                        "atomicfu\$cur", false
                    )
                    +irCall(atomicSymbols.invoke1Symbol).apply {
                        dispatchReceiver = irGet(action)
                        putValueArgument(0, irGet(cur))
                    }
                }
            }
        }

    /*
    inline fun <T> loop$atomicfu$array(atomicArray: AtomicArray<T>, index: Int, action: (T) -> Unit) {
        while (true) {
            val cur = atomicArray.get(index)
            action(cur)
        }
    }
    */

    override fun atomicfuArrayLoopBody(valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody =
        irBlockBody {
            val array = valueParameters[0]
            val index = valueParameters[1]
            val action = valueParameters[2]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        atomicGetArrayElement(valueType, irGet(array), irGet(index)),
                        "atomicfu\$cur", false
                    )
                    +irCall(atomicSymbols.invoke1Symbol).apply {
                        dispatchReceiver = irGet(action)
                        putValueArgument(0, irGet(cur))
                    }
                }
            }
        }

    /*
    inline fun update$atomicfu(refGetter: () -> KMutableProperty0<Int>, action: (Int) -> Int) {
        while (true) {
            val cur = refGetter().get()
            val upd = action(cur)
            if (refGetter().compareAndSetField(cur, upd)) return
        }
    }


    inline fun getAndUpdate$atomicfu(refGetter: () -> KMutableProperty0<Int>, action: (Int) -> Int): Int {
        while (true) {
            val cur = refGetter().get()
            val upd = action(cur)
            if (refGetter().compareAndSetField(cur, upd)) return cur
        }
    }

    inline fun getAndUpdate$atomicfu(refGetter: () -> KMutableProperty0<Int>, action: (Int) -> Int): Int {
        while (true) {
            val cur = refGetter().get()
            val upd = action(cur)
            if (refGetter().compareAndSetField(cur, upd)) return upd
        }
    }
    */

    override fun atomicfuUpdateBody(functionName: String, valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody =
        irBlockBody {
            val refGetter = valueParameters[0]
            val action = valueParameters[1]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        atomicGetField(invokePropertyGetter(irGet(refGetter)), valueType),
                        "atomicfu\$cur", false
                    )
                    val upd = createTmpVariable(
                        irCall(atomicSymbols.invoke1Symbol).apply {
                            dispatchReceiver = irGet(action)
                            putValueArgument(0, irGet(cur))
                        }, "atomicfu\$upd", false
                    )
                    +irIfThen(
                        type = atomicSymbols.irBuiltIns.unitType,
                        condition = irCallAtomicNativeIntrinsic(
                            functionName = "compareAndSet",
                            propertyRef = invokePropertyGetter(irGet(refGetter)),
                            valueType = valueType,
                            valueArguments = listOf(irGet(cur), irGet(upd))
                        ),
                        thenPart = when (functionName) {
                            "update" -> irReturnUnit()
                            "getAndUpdate" -> irReturn(irGet(cur))
                            "updateAndGet" -> irReturn(irGet(upd))
                            else -> error("Unsupported atomicfu inline loop function name: $functionName")
                        }
                    )
                }
            }
        }

    /*
    inline fun update$atomicfu$array(atomicArray: AtomicArray<T>, index: Int, action: (Int) -> Int) {
        while (true) {
            val cur = atomicArray[index]
            val upd = action(cur)
            if (atomicArray.compareAndSet(index, cur, upd)) return
        }
    }


    inline fun getAndUpdate$atomicfu$array(atomicArray: AtomicArray<T>, index: Int, action: (Int) -> Int): Int {
        while (true) {
            val cur = atomicArray[index]
            val upd = action(cur)
            if (atomicArray.compareAndSet(index, cur, upd)) return cur
        }
    }

    inline fun getAndUpdate$atomicfu$array(atomicArray: AtomicArray<T>, index: Int, action: (Int) -> Int): Int {
        while (true) {
            val cur = atomicArray[index]
            val upd = action(cur)
            if (atomicArray.compareAndSet(index, cur, upd)) return upd
        }
    }
    */

    override fun atomicfuArrayUpdateBody(
        functionName: String,
        valueType: IrType,
        valueParameters: List<IrValueParameter>
    ): IrBlockBody =
        irBlockBody {
            val array = valueParameters[0]
            val index = valueParameters[1]
            val action = valueParameters[2]
            val actionArgType = (action.type as IrSimpleType).arguments.first().typeOrNull
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        atomicGetArrayElement(valueType, irGet(array), irGet(index)),
                        "atomicfu\$cur", false
                    )
                    val upd = createTmpVariable(
                        irCall(atomicSymbols.invoke1Symbol).apply {
                            dispatchReceiver = irGet(action)
                            val getCur = if (actionArgType != null) irAs(irGet(cur), actionArgType) else irGet(cur)
                            putValueArgument(0, getCur)
                        }, "atomicfu\$upd", false
                    )
                    +irIfThen(
                        type = atomicSymbols.irBuiltIns.unitType,
                        condition = callAtomicArray(
                            functionName = "compareAndSet",
                            getAtomicArray = irGet(array),
                            index = irGet(index),
                            valueArguments = listOf(irGet(cur), irGet(upd)),
                            valueType = valueType
                        ),
                        thenPart = when (functionName) {
                            "update" -> irReturnUnit()
                            "getAndUpdate" -> irReturn(irGet(cur))
                            "updateAndGet" -> irReturn(irGet(upd))
                            else -> error("Unsupported atomicfu inline loop function name: $functionName")
                        }
                    )
                }
            }
        }

    private fun IrCall.plus(other: IrExpression?): IrCall {
        val returnType = this.symbol.owner.returnType
        val plusOperatorSymbol = when {
            returnType.isInt() -> atomicSymbols.intPlusOperator
            returnType.isLong() -> atomicSymbols.longPlusOperator
            else -> error("Return type of the function ${this.symbol.owner.dump()} is expected to be Int or Long, but found $returnType")
        }
        return irCall(plusOperatorSymbol).apply {
            dispatchReceiver = this@plus
            putValueArgument(0, other)
        }
    }

    fun irPropertyReference(property: IrProperty, classReceiver: IrExpression?): IrPropertyReferenceImpl {
        val backingField = requireNotNull(property.backingField) { "Backing field of the property $property should not be null" }
        return IrPropertyReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = atomicSymbols.buildSimpleType(context.irBuiltIns.kMutableProperty0Class, listOf(backingField.type)),
            symbol = property.symbol,
            typeArgumentsCount = 1,
            field = backingField.symbol,
            getter = property.getter?.symbol,
            setter = property.setter?.symbol
        ).apply {
            putTypeArgument(0, backingField.type)
            dispatchReceiver = classReceiver
        }
    }

    override fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        dispatchReceiver: IrExpression?
    ): IrFunctionAccessExpression =
        when (atomicArrayClass) {
            atomicSymbols.atomicIntArrayClassSymbol, atomicSymbols.atomicLongArrayClassSymbol -> {
                irCall(atomicSymbols.getAtomicArrayConstructor(atomicArrayClass)).apply {
                    putValueArgument(0, size) // size
                    this.dispatchReceiver = dispatchReceiver
                }
            }
            atomicSymbols.atomicRefArrayClassSymbol -> {
                val factoryFunction = atomicSymbols.getAtomicArrayConstructor(atomicArrayClass).owner
                irCall(factoryFunction).apply {
                    val initType = atomicSymbols.function1Type(atomicSymbols.irBuiltIns.intType, atomicSymbols.irBuiltIns.anyNType)
                    val nullLambda = IrFunctionExpressionImpl(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                        type = initType,
                        function = atomicSymbols.irBuiltIns.irFactory.buildFun {
                            name = Name.identifier("<anonymous>")
                            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
                            returnType = atomicSymbols.irBuiltIns.anyNType
                            visibility = DescriptorVisibilities.LOCAL
                        }.apply {
                            val lambda = this
                            addValueParameter("it", atomicSymbols.irBuiltIns.intType)
                            body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                                    IrReturnImpl(
                                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                        type = atomicSymbols.irBuiltIns.nothingType,
                                        returnTargetSymbol = lambda.symbol,
                                        value = IrConstImpl.constNull(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, atomicSymbols.irBuiltIns.nothingNType)
                                    )
                                ))
                            parent = factoryFunction
                        },
                        origin = IrStatementOrigin.LAMBDA
                    )
                    putValueArgument(0, size) // size
                    putValueArgument(1, nullLambda)
                    this.dispatchReceiver = dispatchReceiver
                }
            }
            else -> error("Unsupported atomic array class found: ${atomicArrayClass.owner.render()}")
        }
}
