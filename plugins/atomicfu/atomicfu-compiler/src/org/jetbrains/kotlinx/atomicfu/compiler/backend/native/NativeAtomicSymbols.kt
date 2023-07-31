/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.js.referenceFunction

class NativeAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
) : AbstractAtomicSymbols(context, moduleFragment) {
    // kotlin.concurrent.Volatile annotation class
    override val volatileAnnotationClass: IrClass
        get() = context.referenceClass(ClassId(FqName("kotlin.concurrent"), Name.identifier("Volatile")))?.owner
            ?: error("kotlin.concurrent.Volatile class is not found")

    // kotlin.concurrent.AtomicIntArray
    override val atomicIntArrayClassSymbol: IrClassSymbol
        get() = context.referenceClass(ClassId(FqName("kotlin.concurrent"), Name.identifier("AtomicIntArray")))
            ?: error("kotlin.concurrent.AtomicIntArray is not found")

    // kotlin.concurrent.AtomicLongArray
    override val atomicLongArrayClassSymbol: IrClassSymbol
        get() = context.referenceClass(ClassId(FqName("kotlin.concurrent"), Name.identifier("AtomicLongArray")))
            ?: error("kotlin.concurrent.AtomicLongArray is not found")

    // kotlin.concurrent.AtomicArray
    override val atomicRefArrayClassSymbol: IrClassSymbol
        get() = context.referenceClass(ClassId(FqName("kotlin.concurrent"), Name.identifier("AtomicArray")))
            ?: error("kotlin.concurrent.AtomicArray is not found")

    override fun getAtomicArrayConstructor(atomicArrayClassSymbol: IrClassSymbol): IrFunctionSymbol =
        when (atomicArrayClassSymbol) {
            atomicIntArrayClassSymbol, atomicLongArrayClassSymbol -> {
                atomicArrayClassSymbol.owner.constructors.singleOrNull {
                    it.valueParameters.size == 1 && it.valueParameters[0].type == irBuiltIns.intType
                }?.symbol ?: error("No `public constructor(size: Int) {}` was found for ${atomicArrayClassSymbol.owner.render()}")
            }
            atomicRefArrayClassSymbol -> {
                context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("AtomicArray"))).singleOrNull()
                    ?: error("No factory function `public inline fun <reified T> AtomicArray(size: Int, noinline init: (Int) -> T)` was found for ${atomicArrayClassSymbol.owner.render()}")
            }
            else -> error("Unsupported atomic array class found: ${atomicArrayClassSymbol.owner.render()}")
        }

    // Intrinsics for atomic update of volatile properties

    val atomicGetFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("atomicGetField"))).single()

    val atomicSetFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("atomicSetField"))).single()

    val compareAndSetFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("compareAndSetField"))).single()

    val getAndSetFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("getAndSetField"))).single()

    val getAndAddIntFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("getAndAddField")))
            .single { it.owner.returnType.isInt() }

    val getAndAddLongFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.concurrent"), Name.identifier("getAndAddField")))
            .single { it.owner.returnType.isLong() }

    val intPlusOperator = context.referenceFunctions(CallableId(StandardClassIds.Int, Name.identifier("plus")))
        .single { it.owner.valueParameters[0].type.isInt() }

    val longPlusOperator = context.referenceFunctions(CallableId(StandardClassIds.Long, Name.identifier("plus")))
        .single { it.owner.valueParameters[0].type.isLong() }

    // KMutableProperty0<T>
    fun kMutableProperty0Type(typeArg: IrType): IrType =
        buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(typeArg))

    // () -> KMutableProperty0<T>
    fun kMutableProperty0GetterType(typeArg: IrType): IrType = function0Type(kMutableProperty0Type(typeArg))

    fun getParameterizedAtomicArrayType(elementType: IrType): IrType {
        val atomicArrayClassSymbol = getAtomicArrayClassByValueType(elementType)
        return if (atomicArrayClassSymbol == atomicRefArrayClassSymbol) {
            buildSimpleType(atomicArrayClassSymbol, listOf(elementType))
        } else {
            atomicArrayClassSymbol.defaultType
        }
    }

    override fun createBuilder(symbol: IrSymbol, startOffset: Int, endOffset: Int) =
        NativeAtomicfuIrBuilder(this, symbol, startOffset, endOffset)
}
