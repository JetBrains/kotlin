/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAtomicSymbols(
    val context: IrPluginContext,
    private val moduleFragment: IrModuleFragment
) {
    val irBuiltIns: IrBuiltIns = context.irBuiltIns
    protected val irFactory: IrFactory = context.irFactory

    abstract val volatileAnnotationClass: IrClass

    abstract val atomicIntArrayClassSymbol: IrClassSymbol
    abstract val atomicLongArrayClassSymbol: IrClassSymbol
    abstract val atomicRefArrayClassSymbol: IrClassSymbol

    val volatileAnnotationConstructorCall: IrConstructorCall
        get() {
            val volatileAnnotationConstructor = volatileAnnotationClass.primaryConstructor
                ?: error("Missing constructor in Volatile annotation class")
            return IrConstructorCallImpl.fromSymbolOwner(volatileAnnotationConstructor.returnType, volatileAnnotationConstructor.symbol)
        }

    val invoke0Symbol = irBuiltIns.functionN(0).getSimpleFunction("invoke")!!
    val invoke1Symbol = irBuiltIns.functionN(1).getSimpleFunction("invoke")!!

    fun function0Type(returnType: IrType) = buildSimpleType(
        irBuiltIns.functionN(0).symbol,
        listOf(returnType)
    )

    fun function1Type(argType: IrType, returnType: IrType) = buildSimpleType(
        irBuiltIns.functionN(1).symbol,
        listOf(argType, returnType)
    )

    val arrayOfNulls by lazy {
        context.referenceFunctions(CallableId(FqName("kotlin"), Name.identifier("arrayOfNulls"))).first()
    }

    private val ATOMIC_ARRAY_TYPES: Set<IrClassSymbol> by lazy {
        setOf(
            atomicIntArrayClassSymbol,
            atomicLongArrayClassSymbol,
            atomicRefArrayClassSymbol
        )
    }

    fun isAtomicArrayHandlerType(type: IrType) = type.classOrNull in ATOMIC_ARRAY_TYPES

    // KMutableProperty0<T>
    fun kMutableProperty0Type(typeArg: IrType): IrType =
        buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(typeArg))

    // () -> KMutableProperty0<T>
    fun kMutableProperty0GetterType(typeArg: IrType): IrType = function0Type(kMutableProperty0Type(typeArg))

    fun buildSimpleType(
        symbol: IrClassifierSymbol,
        typeParameters: List<IrType>
    ): IrSimpleType =
        IrSimpleTypeImpl(
            classifier = symbol,
            hasQuestionMark = false,
            arguments = typeParameters.map { makeTypeProjection(it, Variance.INVARIANT) },
            annotations = emptyList()
        )

    companion object {
        val ATOMICFU_GENERATED_FUNCTION by IrDeclarationOriginImpl.Synthetic
        val ATOMICFU_GENERATED_FIELD by IrDeclarationOriginImpl.Synthetic
        val ATOMICFU_GENERATED_PROPERTY by IrDeclarationOriginImpl.Synthetic
        val ATOMICFU_GENERATED_PROPERTY_ACCESSOR by IrDeclarationOriginImpl.Synthetic
    }

    protected fun createPackage(packageName: String): IrPackageFragment =
        createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    fun getAtomicArrayHanlderType(atomicfuArrayType: IrType): IrClassSymbol =
        when (atomicfuArrayType.classFqName?.shortName()?.asString()) {
            "AtomicIntArray", "AtomicBooleanArray"-> atomicIntArrayClassSymbol
            "AtomicLongArray" -> atomicLongArrayClassSymbol
            "AtomicArray" -> atomicRefArrayClassSymbol
            else -> error("Unexpected atomicfu array type ${atomicfuArrayType.render()}.")
        }

    fun getAtomicArrayClassByValueType(valueType: IrType): IrClassSymbol =
        when {
            valueType == irBuiltIns.intType || valueType == irBuiltIns.booleanType -> atomicIntArrayClassSymbol
            valueType == irBuiltIns.longType -> atomicLongArrayClassSymbol
            !valueType.isPrimitiveType() -> atomicRefArrayClassSymbol
            else -> error("No corresponding atomic array class found for the given value type ${valueType.render()}.")
        }

    fun atomicToPrimitiveType(atomicPropertyType: IrType): IrType =
        when (atomicPropertyType.classFqName?.shortName()?.asString()) {
            "AtomicInt" -> irBuiltIns.intType
            "AtomicLong" -> irBuiltIns.longType
            "AtomicBoolean" -> irBuiltIns.booleanType
            "AtomicRef" -> irBuiltIns.anyNType
            else -> error("Expected kotlinx.atomicfu.(AtomicInt|AtomicLong|AtomicBoolean|AtomicRef) type, but found ${atomicPropertyType.render()}")
        }

    fun atomicArrayToPrimitiveType(atomicPropertyType: IrType): IrType =
        when (atomicPropertyType.classFqName?.shortName()?.asString()) {
            "AtomicIntArray", "AtomicBooleanArray" -> irBuiltIns.intType
            "AtomicLongArray" -> irBuiltIns.longType
            "AtomicArray" -> irBuiltIns.anyNType
            else -> error("Expected kotlinx.atomicfu.(AtomicIntArray|AtomicBooleanArray|AtomicLongArray|AtomicArray) type, but found ${atomicPropertyType.render()}")
        }

    abstract fun createBuilder(symbol: IrSymbol): AbstractAtomicfuIrBuilder
}
