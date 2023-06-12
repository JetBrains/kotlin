/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly

abstract class AbstractAtomicfuIrBuilder(
    private val irBuiltIns: IrBuiltIns,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
) : IrBuilderWithScope(IrGeneratorContextBase(irBuiltIns), Scope(symbol), startOffset, endOffset) {

    abstract val atomicSymbols: AbstractAtomicSymbols

    fun irGetProperty(property: IrProperty, dispatchReceiver: IrExpression?) =
        irCall(property.getter?.symbol ?: error("Getter is not defined for the property ${property.render()}")).apply {
            this.dispatchReceiver = dispatchReceiver?.deepCopyWithSymbols()
        }

    fun irCallWithArgs(symbol: IrSimpleFunctionSymbol, dispatchReceiver: IrExpression?, extensionReceiver: IrExpression?, valueArguments: List<IrExpression?>) =
        irCall(symbol).apply {
            this.dispatchReceiver = dispatchReceiver
            this.extensionReceiver = extensionReceiver
            valueArguments.forEachIndexed { i, arg ->
                putValueArgument(i, arg)
            }
        }

    fun irVolatileField(
        name: String,
        type: IrType,
        initValue: IrExpression?,
        annotations: List<IrConstructorCall>,
        parentContainer: IrDeclarationContainer
    ): IrField =
        context.irFactory.buildField {
            this.name = Name.identifier(name)
            this.type = type
            isFinal = false
            isStatic = parentContainer is IrFile
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            initializer = initValue?.let { IrExpressionBodyImpl(it) }
            this.annotations = annotations + atomicSymbols.volatileAnnotationConstructorCall
            this.parent = parentContainer
        }

    fun buildClassInstance(
        irClass: IrClass,
        parentContainer: IrDeclarationContainer,
        isStatic: Boolean
    ): IrField =
        context.irFactory.buildField {
            this.name = Name.identifier(irClass.name.asString().decapitalizeAsciiOnly())
            type = irClass.defaultType
            isFinal = true
            this.isStatic = isStatic
            this.visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            initializer = IrExpressionBodyImpl(
                IrConstructorCallImpl.fromSymbolOwner(
                    irClass.defaultType,
                    irClass.primaryConstructor!!.symbol
                )
            )
            this.parent = parentContainer
        }

    fun IrExpression.toBoolean() = irNotEquals(this, irInt(0)) as IrCall

    fun irClassWithPrivateConstructor(
        name: String,
        parentContainer: IrDeclarationContainer
    ): IrClass = context.irFactory.buildClass {
        this.name = Name.identifier(name)
        kind = ClassKind.CLASS
    }.apply {
        val irClass = this
        this.parent = parentContainer
        parentContainer.addChild(irClass)
        thisReceiver = buildValueParameter(irClass) {
            this.name = Name.identifier("\$this")
            type = IrSimpleTypeImpl(irClass.symbol, false, emptyList(), emptyList())
        }
        irClass.visibility = DescriptorVisibilities.PRIVATE
        addConstructor {
            isPrimary = true
        }.apply {
            body = atomicSymbols.createBuilder(symbol).irBlockBody(startOffset, endOffset) {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +IrInstanceInitializerCallImpl(startOffset, endOffset, irClass.symbol, context.irBuiltIns.unitType)
            }
            this.visibility = DescriptorVisibilities.PRIVATE // constructor of the wrapper class should be private
        }
    }

    fun IrDeclarationContainer.addProperty(
        field: IrField,
        visibility: DescriptorVisibility,
        isVar: Boolean,
        isStatic: Boolean
    ): IrProperty {
        val parentContainer = this
        return context.irFactory.buildProperty {
            this.name = field.name
            this.visibility = visibility
            this.isVar = isVar
        }.apply {
            backingField = field
            field.correspondingPropertySymbol = this.symbol
            parent = parentContainer
            parentContainer.declarations.add(this)
            addGetter(isStatic, parentContainer, irBuiltIns)
            if (isVar) {
                addSetter(isStatic, parentContainer, irBuiltIns)
            }
        }
    }

    private fun IrProperty.addGetter(isStatic: Boolean, parentContainer: IrDeclarationContainer, irBuiltIns: IrBuiltIns) {
        val property = this
        val field = requireNotNull(backingField) { "BackingField of the property $property should not be null"}
        addGetter {
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            visibility = property.visibility
            returnType = field.type
        }.apply {
            dispatchReceiverParameter = if (isStatic) null else (parentContainer as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
            body = factory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        irBuiltIns.nothingType,
                        symbol,
                        IrGetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            field.symbol,
                            field.type,
                            dispatchReceiverParameter?.let {
                                IrGetValueImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    it.type,
                                    it.symbol
                                )
                            }
                        )
                    )
                )
            )
        }
    }

    private fun IrProperty.addSetter(isStatic: Boolean, parentClass: IrDeclarationContainer, irBuiltIns: IrBuiltIns) {
        val property = this
        val field = requireNotNull(property.backingField) { "BackingField of the property $property should not be null"}
        this@addSetter.addSetter {
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            visibility = property.visibility
            returnType = irBuiltIns.unitType
        }.apply {
            dispatchReceiverParameter = if (isStatic) null else (parentClass as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
            addValueParameter("value", field.type)
            val value = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueParameters[0].type, valueParameters[0].symbol)
            body = factory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        irBuiltIns.unitType,
                        symbol,
                        IrSetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            field.symbol,
                            dispatchReceiverParameter?.let {
                                IrGetValueImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    it.type,
                                    it.symbol
                                )
                            },
                            value,
                            irBuiltIns.unitType
                        )
                    )
                )
            )
        }
    }
}
