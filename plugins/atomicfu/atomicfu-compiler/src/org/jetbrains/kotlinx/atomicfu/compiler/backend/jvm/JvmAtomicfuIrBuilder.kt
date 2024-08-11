/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder

// An IR builder with access to AtomicSymbols and convenience methods to build IR constructions for atomicfu JVM/IR transformation.
class JvmAtomicfuIrBuilder internal constructor(
    override val atomicSymbols: JvmAtomicSymbols,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
) : AbstractAtomicfuIrBuilder(atomicSymbols.irBuiltIns, symbol, startOffset, endOffset) {

    // a$FU.get(obj)
    private fun afuGetValue(valueType: IrType, fieldUpdater: IrExpression, obj: IrExpression): IrExpression =
        callAtomicFieldUpdater(
            functionName = "get",
            getAtomicHandler = fieldUpdater,
            valueType = valueType,
            castType = null,
            obj = obj,
            valueArguments = emptyList()
        )

    fun irJavaAtomicFieldUpdater(volatileField: IrField, parentClass: IrClass): IrField {
        // Generate an atomic field updater for the volatile backing field of the given property:
        // val a = atomic(0)
        // volatile var a: Int = 0
        // val a$FU = AtomicIntegerFieldUpdater.newUpdater(parentClass, "a")
        val fuClass = atomicSymbols.javaFUClassSymbol(volatileField.type)
        val fieldName = volatileField.name.asString()
        return context.irFactory.buildField {
            name = Name.identifier("$fieldName\$FU")
            type = fuClass.defaultType
            isFinal = true
            isStatic = true
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            initializer = irExprBody(newJavaAtomicFieldUpdater(fuClass, parentClass, atomicSymbols.irBuiltIns.anyNType, fieldName))
            parent = parentClass
        }
    }

    override fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        dispatchReceiver: IrExpression?
    ): IrFunctionAccessExpression = irCall(atomicSymbols.getAtomicArrayConstructor(atomicArrayClass)).apply {
        putValueArgument(0, size) // size
        this.dispatchReceiver = dispatchReceiver
    }

    // a$FU.compareAndSet(obj, expect, update)
    fun callAtomicFieldUpdater(
        functionName: String,
        getAtomicHandler: IrExpression,
        valueType: IrType,
        castType: IrType?,
        obj: IrExpression?,
        valueArguments: List<IrExpression?>
    ): IrExpression {
        val irCall = irDelegatedAtomicfuCall(
            symbol = atomicSymbols.getAtomicHandlerFunctionSymbol(getAtomicHandler, functionName),
            dispatchReceiver = getAtomicHandler,
            extensionReceiver = null,
            valueArguments = buildList { add(obj); addAll(valueArguments) },
            receiverValueType = valueType
        )
        return if (functionName == "<get-value>" && castType != null) irAs(irCall, castType) else irCall
    }

    // val a$FU = j.u.c.a.AtomicIntegerFieldUpdater.newUpdater(A::class, "a")
    private fun newJavaAtomicFieldUpdater(
        fieldUpdaterClass: IrClassSymbol,
        parentClass: IrClass,
        valueType: IrType,
        fieldName: String
    ) = irCall(atomicSymbols.newUpdater(fieldUpdaterClass)).apply {
        putValueArgument(0, atomicSymbols.javaClassReference(parentClass.symbol.starProjectedType)) // tclass
        if (fieldUpdaterClass == atomicSymbols.atomicRefFieldUpdaterClassSymbol) {
            putValueArgument(1, atomicSymbols.javaClassReference(valueType)) // vclass
            putValueArgument(2, irString(fieldName)) // fieldName
        } else {
            putValueArgument(1, irString(fieldName)) // fieldName
        }
    }

    /*
    inline fun <T> atomicfu$loop(dispatchReceiver: Any?, atomicfu$handler: AtomicIntegerFieldUpdater, atomicfu$action: (Int) -> Unit) {
        while (true) {
            val cur = atomicfu$handler.get()
            atomicfu$action(cur)
        }
    }
    */
    override fun atomicfuLoopBody(valueType: IrType, valueParameters: List<IrValueParameter>) =
        irBlockBody {
            val dispatchReceiver = valueParameters[0]
            val atomicHandler = valueParameters[1]
            val action = valueParameters[2]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        afuGetValue(valueType, irGet(atomicHandler), irGet(dispatchReceiver)),
                        "atomicfu\$cur", false
                    )
                    +irCall(atomicSymbols.invoke1Symbol).apply {
                        this.dispatchReceiver = irGet(action)
                        putValueArgument(0, irGet(cur))
                    }
                }
            }
        }

    /*
    inline fun <T> atomicfu$array$loop(atomicfu$handler: AtomicIntegerArray, index: Int, atomicfu$action: (Int) -> Unit) {
        while (true) {
            val cur = atomicfu$handler.get(index)
            atomicfu$action(cur)
        }
    }
    */
    override fun atomicfuArrayLoopBody(valueType: IrType, valueParameters: List<IrValueParameter>) =
        irBlockBody {
            val atomicHandler = valueParameters[0]
            val index = valueParameters[1]
            val action = valueParameters[2]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        atomicGetArrayElement(valueType, irGet(atomicHandler), irGet(index)),
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
    inline fun atomicfu$update(atomicfu$handler: AtomicIntegerFieldUpdater, atomicfu$action: (Int) -> Int, dispatchReceiver: Any?) {
        while (true) {
            val cur = atomicfu$handler.get()
            val upd = atomicfu$action(cur)
            if (atomicfu$handler.CAS(cur, upd)) return
        }
    }
    */

    /*
    inline fun atomicfu$getAndUpdate(atomicfu$handler: AtomicIntegerFieldUpdater, atomicfu$action: (Int) -> Int, dispatchReceiver: Any?) {
        while (true) {
            val cur = atomicfu$handler.get()
            val upd = atomicfu$action(cur)
            if (atomicfu$handler.CAS(cur, upd)) return cur
        }
    }
    */

    /*
    inline fun atomicfu$updateAndGet(atomicfu$handler: AtomicIntegerFieldUpdater, atomicfu$action: (Int) -> Int, dispatchReceiver: Any?) {
        while (true) {
            val cur = atomicfu$handler.get()
            val upd = atomicfu$action(cur)
            if (atomicfu$handler.CAS(cur, upd)) return upd
        }
    }
    */
    override fun atomicfuUpdateBody(functionName: String, valueType: IrType, valueParameters: List<IrValueParameter>) =
        irBlockBody {
            val dispatchReceiver = valueParameters[0]
            val atomicHandler = valueParameters[1]
            val action = valueParameters[2]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        afuGetValue(valueType, irGet(atomicHandler), irGet(dispatchReceiver)),
                        "atomicfu\$cur", false
                    )
                    val upd = createTmpVariable(
                        irCall(atomicSymbols.invoke1Symbol).apply {
                            this.dispatchReceiver = irGet(action)
                            putValueArgument(0, irGet(cur))
                        }, "atomicfu\$upd", false
                    )
                    +irIfThen(
                        type = atomicSymbols.irBuiltIns.unitType,
                        condition = callAtomicFieldUpdater(
                            functionName = "compareAndSet",
                            getAtomicHandler = irGet(atomicHandler),
                            valueType = valueType,
                            castType = null,
                            obj = irGet(dispatchReceiver),
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
    inline fun atomicfu$array$update(atomicfu$handler: AtomicIntegerArray, index: Int, atomicfu$action: (Int) -> Int) {
        while (true) {
            val cur = atomicfu$handler.get(index)
            val upd = atomicfu$action(cur)
            if (atomicfu$handler.CAS(index, cur, upd)) return
        }
    }
    */

    /*
    inline fun atomicfu$array$getAndUpdate(atomicfu$handler: AtomicIntegerArray, index: Int, atomicfu$action: (Int) -> Int) {
        while (true) {
            val cur = atomicfu$handler.get(index)
            val upd = atomicfu$action(cur)
            if (atomicfu$handler.CAS(index, cur, upd)) return cur
        }
    }
    */

    /*
    inline fun atomicfu$array$updateAndGet(atomicfu$handler: AtomicIntegerArray, index: Int, atomicfu$action: (Int) -> Int) {
        while (true) {
            val cur = atomicfu$handler.get(index)
            val upd = atomicfu$action(cur)
            if (atomicfu$handler.CAS(index, cur, upd)) return upd
        }
    }
    */
    override fun atomicfuArrayUpdateBody(functionName: String, valueType: IrType, valueParameters: List<IrValueParameter>) =
        irBlockBody {
            val atomicHandler = valueParameters[0]
            val index = valueParameters[1]
            val action = valueParameters[2]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        atomicGetArrayElement(valueType, irGet(atomicHandler), irGet(index)),
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
                        condition = callAtomicArray(
                            functionName = "compareAndSet",
                            getAtomicArray = irGet(atomicHandler),
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
}
