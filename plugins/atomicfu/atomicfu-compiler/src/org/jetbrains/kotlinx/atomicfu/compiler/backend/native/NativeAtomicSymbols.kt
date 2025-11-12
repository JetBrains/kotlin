/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.K2IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class NativeAtomicSymbols(
    context: K2IrPluginContext,
    moduleFragment: IrModuleFragment,
) : AbstractAtomicSymbols(context, moduleFragment) {

    private val kotlinConcurrentPackageFqName = FqName("kotlin.concurrent")

    override val volatileAnnotationClass: IrClass
        get() = referenceClass(kotlinConcurrentPackageFqName, "Volatile").owner

    // kotlin.concurrent.AtomicIntArray
    override val atomicIntArrayClassSymbol: IrClassSymbol
        get() = referenceClass(kotlinConcurrentPackageFqName, "AtomicIntArray")

    // kotlin.concurrent.AtomicLongArray
    override val atomicLongArrayClassSymbol: IrClassSymbol
        get() = referenceClass(kotlinConcurrentPackageFqName, "AtomicLongArray")

    // kotlin.concurrent.AtomicArray
    override val atomicRefArrayClassSymbol: IrClassSymbol
        get() = referenceClass(kotlinConcurrentPackageFqName, "AtomicArray")

    // Intrinsics for atomic update of volatile properties

    val nativeAtomicGetFieldIntrinsic: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(kotlinConcurrentPackageFqName, Name.identifier("atomicGetField")))

    val nativeAtomicSetFieldIntrinsic: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(kotlinConcurrentPackageFqName, Name.identifier("atomicSetField")))

    val nativeCompareAndSetFieldIntrinsic: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(kotlinConcurrentPackageFqName, Name.identifier("compareAndSetField")))

    val nativeGetAndSetFieldIntrinsic: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(kotlinConcurrentPackageFqName, Name.identifier("getAndSetField")))

    val nativeGetAndAddIntFieldIntrinsic: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(kotlinConcurrentPackageFqName, Name.identifier("getAndAddField"))) {
            it.owner.returnType.isInt()
        }

    val nativeGetAndAddLongFieldIntrinsic: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(kotlinConcurrentPackageFqName, Name.identifier("getAndAddField"))) {
            it.owner.returnType.isLong()
        }

    val intPlusOperator: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(StandardClassIds.Int, Name.identifier("plus"))) {
            it.owner.parameters.size == 2 && it.owner.parameters.last().type.isInt()
        }

    val longPlusOperator: IrSimpleFunctionSymbol
        get() = referenceFunction(CallableId(StandardClassIds.Long, Name.identifier("plus"))) {
            it.owner.parameters.size == 2 && it.owner.parameters.last().type.isLong()
        }


    fun isVolatilePropertyReferenceGetter(type: IrType): Boolean = type.classOrNull == irBuiltIns.functionN(0).symbol

    override fun createBuilder(symbol: IrSymbol): NativeAtomicfuIrBuilder =
        NativeAtomicfuIrBuilder(this, symbol)

    private inline fun referenceFunction(
        callableId: CallableId,
        predicate: (IrSimpleFunctionSymbol) -> Boolean = { true },
    ): IrSimpleFunctionSymbol {
        return context.referenceFunctions(callableId, currentFile!!).single(predicate)
    }
}
