/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.getSimpleFunction
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
            val volatileAnnotationConstructor = buildAnnotationConstructor(volatileAnnotationClass)
            return IrConstructorCallImpl.fromSymbolOwner(volatileAnnotationConstructor.returnType, volatileAnnotationConstructor.symbol)
        }

    abstract fun createBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): AbstractAtomicfuIrBuilder

    val invoke1Symbol = irBuiltIns.functionN(1).getSimpleFunction("invoke")!!

    fun function1Type(argType: IrType, returnType: IrType) = buildSimpleType(
        irBuiltIns.functionN(1).symbol,
        listOf(argType, returnType)
    )

    object ATOMICFU_GENERATED_CLASS : IrDeclarationOriginImpl("ATOMICFU_GENERATED_CLASS", isSynthetic = true)
    object ATOMICFU_GENERATED_FUNCTION : IrDeclarationOriginImpl("ATOMICFU_GENERATED_FUNCTION", isSynthetic = true)
    object ATOMICFU_GENERATED_FIELD : IrDeclarationOriginImpl("ATOMICFU_GENERATED_FIELD", isSynthetic = true)
    object ATOMICFU_GENERATED_PROPERTY : IrDeclarationOriginImpl("ATOMICFU_GENERATED_PROPERTY", isSynthetic = true)
    object ATOMICFU_GENERATED_PROPERTY_ACCESSOR : IrDeclarationOriginImpl("ATOMICFU_GENERATED_PROPERTY_ACCESSOR", isSynthetic = true)

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

    private fun buildAnnotationConstructor(annotationClass: IrClass): IrConstructor =
        annotationClass.addConstructor { isPrimary = true }

    private fun buildSimpleType(
        symbol: IrClassifierSymbol,
        typeParameters: List<IrType>
    ): IrSimpleType =
        IrSimpleTypeImpl(
            classifier = symbol,
            hasQuestionMark = false,
            arguments = typeParameters.map { makeTypeProjection(it, Variance.INVARIANT) },
            annotations = emptyList()
        )
}
