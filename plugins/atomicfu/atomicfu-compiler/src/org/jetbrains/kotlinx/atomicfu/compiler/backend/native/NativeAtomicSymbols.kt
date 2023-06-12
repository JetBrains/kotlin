/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class NativeAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
) : AbstractAtomicSymbols(context, moduleFragment) {
    override val volatileAnnotationClass: IrClass
        get() = context.referenceClass(ClassId(FqName("kotlin.concurrent"), Name.identifier("Volatile")))?.owner
            ?: error("kotlin.concurrent.Volatile class is not found")

    // AtomicInt class

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

    val kMutableProperty0Get = irBuiltIns.kMutableProperty0Class.functionByName("get")

    val kMutableProperty0Set = irBuiltIns.kMutableProperty0Class.functionByName("set")

    val intPlusOperator = context.referenceFunctions(CallableId(StandardClassIds.Int, Name.identifier("plus")))
        .single { it.owner.valueParameters[0].type.isInt() }

    val longPlusOperator = context.referenceFunctions(CallableId(StandardClassIds.Long, Name.identifier("plus")))
        .single { it.owner.valueParameters[0].type.isLong() }

    // KMutableProperty0<T>
    fun kMutableProperty0Type(typeArg: IrType): IrType =
        irSimpleType(irBuiltIns.kMutableProperty0Class, listOf(typeArg))

    // () -> KMutableProperty0<T>
    fun kMutableProperty0GetterType(typeArg: IrType): IrType = function0Type(kMutableProperty0Type(typeArg))

    override fun createBuilder(symbol: IrSymbol, startOffset: Int, endOffset: Int) =
        NativeAtomicfuIrBuilder(this, symbol, startOffset, endOffset)
}
