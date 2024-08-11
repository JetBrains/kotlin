/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

// TODO replace with these constants everywhere
internal const val ATOMIC_INT = "AtomicInt"
internal const val ATOMIC_BOOLEAN = "AtomicBoolean"
internal const val ATOMIC_LONG = "AtomicLong"
internal const val ATOMIC_REF = "AtomicRef"
internal const val ATOMIC_INT_ARRAY = "AtomicIntArray"
internal const val ATOMIC_BOOLEAN_ARRAY = "AtomicBooleanArray"
internal const val ATOMIC_LONG_ARRAY = "AtomicLongArray"
internal const val ATOMIC_ARRAY = "AtomicArray"

abstract class AbstractAtomicSymbols(
    val context: IrPluginContext,
    private val moduleFragment: IrModuleFragment
) {
    val irBuiltIns: IrBuiltIns = context.irBuiltIns
    protected val irFactory: IrFactory = IrFactoryImpl

    abstract val volatileAnnotationClass: IrClass
    val volatileAnnotationConstructorCall: IrConstructorCall
        get() {
            val volatileAnnotationConstructor = volatileAnnotationClass.primaryConstructor
                ?: error("Missing constructor in Volatile annotation class")
            return IrConstructorCallImpl.fromSymbolOwner(volatileAnnotationConstructor.returnType, volatileAnnotationConstructor.symbol)
        }

    abstract val atomicIntArrayClassSymbol: IrClassSymbol
    abstract val atomicLongArrayClassSymbol: IrClassSymbol
    abstract val atomicRefArrayClassSymbol: IrClassSymbol

    private val ATOMIC_ARRAY_TYPES: Set<IrClassSymbol>
        get() = setOf(
            atomicIntArrayClassSymbol,
            atomicLongArrayClassSymbol,
            atomicRefArrayClassSymbol
        )

    fun isAtomicArrayHandlerType(valueType: IrType) = valueType.classOrNull in ATOMIC_ARRAY_TYPES

    abstract fun getAtomicArrayConstructor(atomicArrayClassSymbol: IrClassSymbol): IrFunctionSymbol

    abstract fun getAtomicHandlerTypeByAtomicfuType(atomicfuType: IrType): IrType

    fun getAtomicArrayClassByAtomicfuArrayType(atomicfuArrayType: IrType): IrClassSymbol =
        when (atomicfuArrayType.classFqName?.shortName()?.asString()) {
            "AtomicIntArray" -> atomicIntArrayClassSymbol
            "AtomicLongArray" -> atomicLongArrayClassSymbol
            "AtomicBooleanArray" -> atomicIntArrayClassSymbol
            "AtomicArray" -> atomicRefArrayClassSymbol
            else -> error("Unexpected atomicfu array type ${atomicfuArrayType.render()}.")
        }

    fun getAtomicArrayClassByValueType(valueType: IrType): IrClassSymbol =
        when {
            valueType == irBuiltIns.intType -> atomicIntArrayClassSymbol
            valueType == irBuiltIns.booleanType -> atomicIntArrayClassSymbol
            valueType == irBuiltIns.longType -> atomicLongArrayClassSymbol
            !valueType.isPrimitiveType() -> atomicRefArrayClassSymbol
            else -> error("No corresponding atomic array class found for the given value type ${valueType.render()}.")
        }

    fun getAtomicHandlerFunctionSymbol(getAtomicHandler: IrExpression, name: String): IrSimpleFunctionSymbol {
        val atomicHandlerClassSymbol = (getAtomicHandler.type as IrSimpleType).classOrNull
            ?: error("Failed to obtain the class corresponding to the array type ${getAtomicHandler.render()}.")
        return when (name) {
            "<get-value>", "getValue" -> atomicHandlerClassSymbol.getSimpleFunction("get")
            "<set-value>", "setValue", "lazySet" -> atomicHandlerClassSymbol.getSimpleFunction("set")
            else -> atomicHandlerClassSymbol.getSimpleFunction(name)
        } ?: error("No $name function found in ${atomicHandlerClassSymbol.owner.render()}")
    }

    abstract fun createBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): AbstractAtomicfuIrBuilder

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
        val ATOMICFU_GENERATED_CLASS by IrDeclarationOriginImpl.Synthetic
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

    protected fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality,
        isValueClass: Boolean = false,
    ): IrClassSymbol = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = isValueClass
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }.symbol
}
