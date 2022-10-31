/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicIrBuilder
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicSymbols

class NativeAtomicSymbols(
    irBuiltIns: IrBuiltIns,
    moduleFragment: IrModuleFragment
) : AtomicSymbols(irBuiltIns, moduleFragment) {

    private val nativeConcurrentPackage: IrPackageFragment = createPackage("kotlin.native.concurrent")

    // kotlin.native.concurrent.AtomicInt
    val atomicIntNativeClass: IrClassSymbol =
        createClass(nativeConcurrentPackage, "AtomicInt", ClassKind.CLASS, Modality.FINAL)

    val atomicIntNativeConstructor: IrConstructorSymbol = atomicIntNativeClass.owner.addConstructor().apply {
        addValueParameter("value_", irBuiltIns.intType)
    }.symbol

    val atomicIntCompareAndSet: IrSimpleFunctionSymbol =
        atomicIntNativeClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("expect", irBuiltIns.intType)
            addValueParameter("new", irBuiltIns.intType)
        }.symbol

    val atomicIntAddAndGet: IrSimpleFunctionSymbol =
        atomicIntNativeClass.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntGet: IrSimpleFunctionSymbol =
        atomicIntNativeClass.owner.addFunction(name = "get", returnType = irBuiltIns.intType).symbol

    val atomicIntSet: IrSimpleFunctionSymbol =
        atomicIntNativeClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("new", irBuiltIns.intType)
        }.symbol

    // TODO: getAndAdd, incrementAndGet, getAndIncrement, decrementAndGet, getAndDecrement, lazySet, getAndSet

    override fun createBuilder(symbol: IrSymbol, startOffset: Int, endOffset: Int): AtomicIrBuilder =
        AtomicIrBuilder(this, symbol, startOffset, endOffset)


}
