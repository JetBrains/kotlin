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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
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

    // atomicArr.get(index)
    fun atomicGetArrayElement(valueType: IrType, receiver: IrExpression, index: IrExpression): IrCall =
        irDelegatedAtomicfuCall(
            symbol = atomicSymbols.getAtomicHandlerFunctionSymbol(receiver, "get"),
            dispatchReceiver = receiver,
            extensionReceiver = null,
            valueArguments = listOf(index),
            receiverValueType = valueType
        )

    /**
     * This function is used to invoke delegates of the *atomicfu library* functions:
     * - functions on the corresponding atomic handlers for atomics and atomic arrays
     * - transformed loop/update/getAndUpdate/updateAndGetFunctions
     *
     * This is the only place where Bool <-> Int conversion happens for AtomicBoolean properties.
     * AtomicBoolean property is represented with a volatile Int field + atomic handlers.
     * There are 2 conversion cases:
     * 1. The function that returns the current / updated value of the atomic should return a Boolean (get/getAndSet/updateAndGet/getAndUpdate):
     *   val b: AtomicBoolean = atomic(false)         @Volatile b$volatile: Int = 0 // it's handled with the AtomicIntegerFieldUpdater
     *   val res: Boolean = b.get()            -----> val res: Boolean = b$FU.get().toBoolean() // the return value should be casted to Boolean
     * 2. Arguments passed to atomicfu functions are Boolean, these invocations are delegated to atomic handlers that update the volatile Int field,
     *    hence Boolean arguments should be casted to Int:
     *    b.compareAndSet(false, true) -----> b$FU.compareAndSet(0, 1) // field updaters for JVM
     *                                        ::b$volatile.compareAndSetField(0, 1) // atomic intrinsics for K/N
     *
     *  Example with `loop` function:
     *  val res: Boolean = b.loop { cur ->  -----> fun loop$atomicfu$boolean(atomicHandler: Any?, action: (Boolean) -> Boolean) // transformed loop function
     *      if (!cur) return                       loop$atomicfu$boolean(b$FU, action: (Boolean) -> Boolean) {
     *      b.compareAndSet(cur, false)                while(true) {
     *  }                                                  val cur: Boolean = b$FU.get().toBoolean()
     *                                                     val upd: Boolean = action(cur)
     *                                                     if (b$FU.compareAndSet(cur.toInt(), upd.toInt())) return
     *                                                 }
     *                                             }
     */
    fun irDelegatedAtomicfuCall(
        symbol: IrSimpleFunctionSymbol,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArguments: List<IrExpression?>,
        receiverValueType: IrType,
    ): IrCall {
        val volatileFieldType = if (receiverValueType.isBoolean()) irBuiltIns.intType else receiverValueType
        val ownerTypeParameter = symbol.owner.typeParameters.firstOrNull()?.defaultType
        val irCall = irCall(symbol).apply {
            this.dispatchReceiver = dispatchReceiver
            this.extensionReceiver = extensionReceiver
            // NOTE: in case of parameterized K/N intrinsic (atomicGetField<T>, compareAndSet<T>..) compare parameter types with a type argument.
            if (symbol.owner.typeParameters.isNotEmpty()) {
                require(symbol.owner.typeParameters.size == 1) { "Only K/N atomic intrinsics are parameterized with a type of the updated volatile field. A function with more type parameters is being invoked: ${symbol.owner.render()}" }
                putTypeArgument(0, volatileFieldType)
            }
            valueArguments.forEachIndexed { i, arg ->
                putValueArgument(i, arg?.let {
                    val expectedParameterType = symbol.owner.valueParameters[i].type
                    if (receiverValueType.isBoolean() && !arg.type.isInt() &&
                        (expectedParameterType.isInt() || expectedParameterType == ownerTypeParameter)
                    ) toInt(it) else it
                })
            }
        }
        return if (receiverValueType.isBoolean() &&
            (symbol.owner.returnType.isInt() || symbol.owner.returnType == ownerTypeParameter)
        ) toBoolean(irCall) else irCall
    }

    fun irVolatileField(
        name: String,
        valueType: IrType,
        initValue: IrExpression?,
        annotations: List<IrConstructorCall>,
        parentContainer: IrDeclarationContainer,
    ): IrField {
        // AtomicBoolean property is replaced with a volatile Int field + atomic handlers for both JVM and K/N.
        val castBooleanToInt = valueType.isBoolean()
        return context.irFactory.buildField {
            this.name = Name.identifier(name)
            this.type = if (castBooleanToInt) irBuiltIns.intType else valueType // for a boolean value, create int volatile field
            isFinal = false
            isStatic = parentContainer is IrFile
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            // Cast the Boolean initValue to Int
            val castedInitValue = if (castBooleanToInt && initValue != null) toInt(initValue) else initValue
            initializer = castedInitValue?.let(context.irFactory::createExpressionBody)
            this.annotations = annotations + atomicSymbols.volatileAnnotationConstructorCall
            this.parent = parentContainer
        }
    }

    fun irAtomicArrayField(
        name: Name,
        arrayClass: IrClassSymbol,
        isStatic: Boolean,
        annotations: List<IrConstructorCall>,
        size: IrExpression,
        dispatchReceiver: IrExpression?,
        parentContainer: IrDeclarationContainer
    ): IrField =
        context.irFactory.buildField {
            this.name = name
            type = arrayClass.defaultType
            this.isFinal = true
            this.isStatic = isStatic
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            this.initializer = context.irFactory.createExpressionBody(
                newAtomicArray(arrayClass, size, dispatchReceiver)
            )
            this.annotations = annotations
            this.parent = parentContainer
        }

    abstract fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        dispatchReceiver: IrExpression?
    ): IrFunctionAccessExpression

    // atomicArr.compareAndSet(index, expect, update)
    fun callAtomicArray(
        functionName: String,
        getAtomicArray: IrExpression,
        index: IrExpression,
        valueArguments: List<IrExpression?>,
        valueType: IrType
    ): IrCall = irDelegatedAtomicfuCall(
        symbol = atomicSymbols.getAtomicHandlerFunctionSymbol(getAtomicArray, functionName),
        dispatchReceiver = getAtomicArray,
        extensionReceiver = null,
        valueArguments = buildList { add(index); addAll(valueArguments) },
        receiverValueType = valueType
    )

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
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            initializer = context.irFactory.createExpressionBody(
                IrConstructorCallImpl.fromSymbolOwner(
                    irClass.defaultType,
                    irClass.primaryConstructor!!.symbol
                )
            )
            this.parent = parentContainer
        }

    fun toBoolean(irExpr: IrExpression) = irEquals(irExpr, irInt(1)) as IrCall
    fun toInt(irExpr: IrExpression) = irIfThenElse(irBuiltIns.intType, irExpr, irInt(1), irInt(0))

    fun irClassWithPrivateConstructor(
        name: String,
        visibility: DescriptorVisibility,
        parentContainer: IrDeclarationContainer
    ): IrClass = context.irFactory.buildClass {
        this.name = Name.identifier(name)
        kind = ClassKind.CLASS
        origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_CLASS
    }.apply {
        val irClass = this
        this.parent = parentContainer
        parentContainer.addChild(irClass)
        thisReceiver = buildValueParameter(irClass) {
            this.name = Name.identifier("\$this")
            type = IrSimpleTypeImpl(irClass.symbol, false, emptyList(), emptyList())
        }
        irClass.visibility = visibility
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

    fun IrDeclarationContainer.replacePropertyAtIndex(
        field: IrField,
        visibility: DescriptorVisibility,
        isVar: Boolean,
        isStatic: Boolean,
        index: Int
    ): IrProperty = buildPropertyWithAccessors(field, visibility, isVar, isStatic, this).also { declarations[index] = it }

    fun IrDeclarationContainer.addProperty(
        field: IrField,
        visibility: DescriptorVisibility,
        isVar: Boolean,
        isStatic: Boolean
    ): IrProperty = buildPropertyWithAccessors(field, visibility, isVar, isStatic, this).also { declarations.add(it) }

    private fun buildPropertyWithAccessors(
        field: IrField,
        visibility: DescriptorVisibility,
        isVar: Boolean,
        isStatic: Boolean,
        parentContainer: IrDeclarationContainer
    ) = context.irFactory.buildProperty {
        this.name = field.name
        this.visibility = visibility
        this.isVar = isVar
        origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_PROPERTY
    }.apply {
        backingField = field
        field.correspondingPropertySymbol = this.symbol
        parent = parentContainer
        addGetter(isStatic, parentContainer, irBuiltIns)
        if (isVar) {
            addSetter(isStatic, parentContainer, irBuiltIns)
        }
    }

    private fun IrProperty.addGetter(isStatic: Boolean, parentContainer: IrDeclarationContainer, irBuiltIns: IrBuiltIns) {
        val property = this
        val field = requireNotNull(backingField) { "The backing field of the property $property should not be null." }
        addGetter {
            visibility = property.visibility
            returnType = field.type
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_PROPERTY_ACCESSOR
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
        val field = requireNotNull(property.backingField) { "The backing field of the property $property should not be null." }
        this@addSetter.addSetter {
            visibility = property.visibility
            returnType = irBuiltIns.unitType
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_PROPERTY_ACCESSOR
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

    abstract fun atomicfuLoopBody(valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody
    abstract fun atomicfuArrayLoopBody(valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody
    abstract fun atomicfuUpdateBody(functionName: String, valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody
    abstract fun atomicfuArrayUpdateBody(functionName: String, valueType: IrType, valueParameters: List<IrValueParameter>): IrBlockBody
}
