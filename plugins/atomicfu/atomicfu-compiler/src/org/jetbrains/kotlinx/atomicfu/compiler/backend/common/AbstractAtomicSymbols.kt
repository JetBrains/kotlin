/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isPrimitiveType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
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

    protected val ATOMIC_ARRAY_TYPES: Set<IrClassSymbol>
        get() = setOf(
            atomicIntArrayClassSymbol,
            atomicLongArrayClassSymbol,
            atomicRefArrayClassSymbol
        )

    fun isAtomicArrayHandlerType(valueType: IrType) = valueType.classOrNull in ATOMIC_ARRAY_TYPES

    abstract fun getAtomicArrayConstructor(atomicArrayClassSymbol: IrClassSymbol): IrFunctionSymbol

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

    fun getAtomicHandlerFunctionSymbol(atomicHandlerClass: IrClassSymbol, name: String): IrSimpleFunctionSymbol =
        when (name) {
            "<get-value>", "getValue" -> atomicHandlerClass.getSimpleFunction("get")
            "<set-value>", "setValue", "lazySet" -> atomicHandlerClass.getSimpleFunction("set")
            else -> atomicHandlerClass.getSimpleFunction(name)
        } ?: error("No $name function found in ${atomicHandlerClass.owner.render()}")

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
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
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
