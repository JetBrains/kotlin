/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class NativeAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
) : AbstractAtomicSymbols(context, moduleFragment) {

    private val kotlinConcurrentPackageFqName = FqName("kotlin.concurrent")

    override val volatileAnnotationClass: IrClass
        get() = context.referenceClass(ClassId(kotlinConcurrentPackageFqName, Name.identifier("Volatile")))?.owner
            ?: error("kotlin.concurrent.Volatile class is not found")

    // kotlin.concurrent.AtomicIntArray
    override val atomicIntArrayClassSymbol: IrClassSymbol by lazy {
        context.referenceClass(ClassId(kotlinConcurrentPackageFqName, Name.identifier("AtomicIntArray")))
            ?: error("kotlin.concurrent.AtomicIntArray is not found")
    }

    // kotlin.concurrent.AtomicLongArray
    override val atomicLongArrayClassSymbol: IrClassSymbol by lazy {
        context.referenceClass(ClassId(kotlinConcurrentPackageFqName, Name.identifier("AtomicLongArray")))
            ?: error("kotlin.concurrent.AtomicLongArray is not found")
    }

    // kotlin.concurrent.AtomicArray
    override val atomicRefArrayClassSymbol: IrClassSymbol by lazy {
        context.referenceClass(ClassId(kotlinConcurrentPackageFqName, Name.identifier("AtomicArray")))
            ?: error("kotlin.concurrent.AtomicArray is not found")
    }

    // Intrinsics for atomic update of volatile properties

    val nativeAtomicGetFieldIntrinsic by lazy {
        context.referenceFunctions(CallableId(kotlinConcurrentPackageFqName, Name.identifier("atomicGetField"))).single()
    }
    val nativeAtomicSetFieldIntrinsic by lazy {
        context.referenceFunctions(CallableId(kotlinConcurrentPackageFqName, Name.identifier("atomicSetField"))).single()
    }
    val nativeCompareAndSetFieldIntrinsic by lazy {
        context.referenceFunctions(CallableId(kotlinConcurrentPackageFqName, Name.identifier("compareAndSetField"))).single()
    }
    val nativeGetAndSetFieldIntrinsic by lazy {
        context.referenceFunctions(CallableId(kotlinConcurrentPackageFqName, Name.identifier("getAndSetField"))).single()
    }
    val nativeGetAndAddIntFieldIntrinsic by lazy {
        context.referenceFunctions(CallableId(kotlinConcurrentPackageFqName, Name.identifier("getAndAddField")))
            .single { it.owner.returnType.isInt() }
    }
    val nativeGetAndAddLongFieldIntrinsic by lazy {
        context.referenceFunctions(CallableId(kotlinConcurrentPackageFqName, Name.identifier("getAndAddField")))
            .single { it.owner.returnType.isLong() }
    }
    val intPlusOperator by lazy {
        context.referenceFunctions(CallableId(StandardClassIds.Int, Name.identifier("plus")))
            .single { it.owner.valueParameters[0].type.isInt() }
    }
    val longPlusOperator by lazy {
        context.referenceFunctions(CallableId(StandardClassIds.Long, Name.identifier("plus")))
            .single { it.owner.valueParameters[0].type.isLong() }
    }

    fun isVolatilePropertyReferenceGetter(type: IrType) = type.classOrNull == irBuiltIns.functionN(0).symbol

    override fun createBuilder(symbol: IrSymbol) =
        NativeAtomicfuIrBuilder(this, symbol)
}