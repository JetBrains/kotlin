/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.AtomicfuJvmIrBuilder

abstract class AtomicSymbols(
    val irBuiltIns: IrBuiltIns,
    private val moduleFragment: IrModuleFragment
) {
    protected val irFactory: IrFactory = IrFactoryImpl

    fun function0Type(returnType: IrType) = buildFunctionSimpleType(
        irBuiltIns.functionN(0).symbol,
        listOf(returnType)
    )

    fun function1Type(argType: IrType, returnType: IrType) = buildFunctionSimpleType(
        irBuiltIns.functionN(1).symbol,
        listOf(argType, returnType)
    )

    val invoke0Symbol = irBuiltIns.functionN(0).getSimpleFunction("invoke")!!
    val invoke1Symbol = irBuiltIns.functionN(1).getSimpleFunction("invoke")!!

    protected fun buildClass(
        fqName: FqName,
        classKind: ClassKind,
        parent: IrDeclarationContainer
    ): IrClass = irFactory.buildClass {
        name = fqName.shortName()
        kind = classKind
    }.apply {
        val irClass = this
        this.parent = parent
        parent.addChild(irClass)
        thisReceiver = buildValueParameter(irClass) {
            name = Name.identifier("\$this")
            type = IrSimpleTypeImpl(irClass.symbol, false, emptyList(), emptyList())
        }
    }

    protected fun buildAnnotationConstructor(annotationClass: IrClass): IrConstructor =
        annotationClass.addConstructor { isPrimary = true }

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

    abstract fun createBuilder(symbol: IrSymbol, startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET): AtomicIrBuilder
}
