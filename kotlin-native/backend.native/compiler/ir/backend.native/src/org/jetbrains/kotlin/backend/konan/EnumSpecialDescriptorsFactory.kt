/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.ir.addFakeOverrides
import org.jetbrains.kotlin.backend.common.ir.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module


internal object DECLARATION_ORIGIN_ENUM : IrDeclarationOriginImpl("ENUM")

/**
 * Common interface for both [InternalLoweredEnum] and [ExternalLoweredEnum]
 * that allows to work with lowered enum regardless of its location.
 */
internal interface LoweredEnumAccess {
    val valuesGetter: IrSimpleFunction
    val itemGetterSymbol: IrSimpleFunctionSymbol
    val entriesMap: Map<Name, Int>
    fun getValuesField(startOffset: Int, endOffset: Int): IrExpression
}

/**
 * Represents lowered enum from current module.
 */
internal data class InternalLoweredEnum(
        val implObject: IrClass,
        val valuesField: IrField,
        val valuesGetterWrapper: IrSimpleFunction,
        override val valuesGetter: IrSimpleFunction,
        override val itemGetterSymbol: IrSimpleFunctionSymbol,
        override val entriesMap: Map<Name, Int>
) : LoweredEnumAccess {
    private fun internalObjectGetter(startOffset: Int, endOffset: Int) =
            IrGetObjectValueImpl(startOffset, endOffset,
                    implObject.defaultType,
                    implObject.symbol
            )

    override fun getValuesField(startOffset: Int, endOffset: Int): IrExpression = IrGetFieldImpl(
            startOffset,
            endOffset,
            valuesField.symbol,
            valuesField.type,
            internalObjectGetter(startOffset, endOffset)
    )
}

/**
 * Represents lowered enum that's located in external module.
 */
internal data class ExternalLoweredEnum(
        override val valuesGetter: IrSimpleFunction,
        override val itemGetterSymbol: IrSimpleFunctionSymbol,
        override val entriesMap: Map<Name, Int>
) : LoweredEnumAccess {
    override fun getValuesField(startOffset: Int, endOffset: Int): IrExpression =
            IrCallImpl(startOffset, endOffset, valuesGetter.returnType, valuesGetter.symbol, valuesGetter.typeParameters.size, valuesGetter.valueParameters.size)
}

internal class EnumSpecialDeclarationsFactory(val context: Context) {
    private val symbols = context.ir.symbols

    private fun enumEntriesMap(enumClass: IrClass): Map<Name, Int> =
            enumClass.declarations
                    .filterIsInstance<IrEnumEntry>()
                    .sortedBy { it.name }
                    .withIndex()
                    .associate { it.value.name to it.index }
                    .toMap()

    private fun findItemGetterSymbol(): IrSimpleFunctionSymbol =
            symbols.array.functions.single { it.descriptor.name == Name.identifier("get") }

    private fun valuesArrayType(enumClass: IrClass): IrType =
            symbols.array.typeWith(enumClass.defaultType)

    // We can't move property getter to the top-level scope.
    // So add a wrapper instead.
    private fun createValuesGetterWrapper(enumClass: IrClass, isExternal: Boolean): IrSimpleFunction =
            context.irFactory.buildFun {
                name = InternalAbi.getEnumValuesAccessorName(enumClass)
                returnType = valuesArrayType(enumClass)
                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                this.isExternal = isExternal
            }.also {
                if (isExternal) {
                    context.internalAbi.reference(it, enumClass.module)
                } else {
                    context.internalAbi.declare(it, enumClass.module)
                }
            }

    fun createExternalLoweredEnum(enumClass: IrClass): ExternalLoweredEnum {
        val enumEntriesMap = enumEntriesMap(enumClass)
        val itemGetterSymbol = findItemGetterSymbol()
        val valuesGetterWrapper = createValuesGetterWrapper(enumClass, isExternal = true)
        return ExternalLoweredEnum(valuesGetterWrapper, itemGetterSymbol, enumEntriesMap)
    }

    fun createInternalLoweredEnum(enumClass: IrClass): InternalLoweredEnum {
        val startOffset = enumClass.startOffset
        val endOffset = enumClass.endOffset

        val implObject =
            IrClassImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrClassSymbolImpl(),
                    "OBJECT".synthesizedName,
                    ClassKind.OBJECT,
                    DescriptorVisibilities.PUBLIC,
                    Modality.FINAL,
                    isCompanion = false,
                    isInner = false,
                    isData = false,
                    isExternal = false,
                    isInline = false,
                    isExpect = false,
                    isFun = false
            ).apply {
                parent = enumClass
                createParameterDeclarations()
            }

        val valuesType = valuesArrayType(enumClass)
        val valuesField =
            IrFieldImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrFieldSymbolImpl(),
                    "VALUES".synthesizedName,
                    valuesType,
                    DescriptorVisibilities.PRIVATE,
                    isFinal = true,
                    isExternal = false,
                    isStatic = false,
            ).apply {
                parent = implObject
            }

        val valuesGetter =
            IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrSimpleFunctionSymbolImpl(),
                    "get-VALUES".synthesizedName,
                    DescriptorVisibilities.PUBLIC,
                    Modality.FINAL,
                    valuesType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false,
                    isInfix = false
            ).apply {
                parent = implObject
            }

        val constructorOfAny = context.irBuiltIns.anyClass.owner.constructors.first()
        implObject.addSimpleDelegatingConstructor(
                constructorOfAny,
                context.irBuiltIns,
                true // TODO: why primary?
        )

        implObject.superTypes += context.irBuiltIns.anyType
        implObject.addFakeOverrides(context.irBuiltIns)

        val itemGetterSymbol = findItemGetterSymbol()
        val enumEntriesMap = enumEntriesMap(enumClass)
        val valuesGetterWrapper = createValuesGetterWrapper(enumClass, isExternal = false)
        context.createIrBuilder(valuesGetterWrapper.symbol).run {
            valuesGetterWrapper.body = irBlockBody {
                +irReturn(irCall(valuesGetter))
            }
        }
        return InternalLoweredEnum(
                implObject,
                valuesField,
                valuesGetterWrapper,
                valuesGetter,
                itemGetterSymbol,
                enumEntriesMap)
    }
}
