/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicHandlerType
import org.jetbrains.kotlinx.atomicfu.compiler.backend.atomicfuRender
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer.Companion.VOLATILE
import org.jetbrains.kotlinx.atomicfu.compiler.diagnostic.AtomicfuErrorMessages.CONSTRAINTS_MESSAGE
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractAtomicfuIrBuilder(
    protected val irBuiltIns: IrBuiltIns,
    symbol: IrSymbol
) : IrBuilderWithScope(IrGeneratorContextBase(irBuiltIns), Scope(symbol), UNDEFINED_OFFSET, UNDEFINED_OFFSET) {

    abstract val atomicfuSymbols: AbstractAtomicSymbols

    /**
     * This is the only place where Bool <-> Int conversion happens for AtomicBoolean properties.
     * On JVM:
     * - AtomicBoolean property is replaced with a volatile Int field + AtomicIntegerFieldUpdater
     * - AtomicBooleanArray is replaced with AtomicIntegerArray
     * On K/N:
     * - AtomicBoolean property is replaced with a volatile Boolean field + atomic intrinsics parameterized with Boolean -> no conversions
     * - AtomicBooleanArray is replaced with AtomicIntArray
     *
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
    abstract fun irCallFunction (
        symbol: IrSimpleFunctionSymbol,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArguments: List<IrExpression?>,
        valueType: IrType
    ): IrCall

    protected fun invokeFunctionOnAtomicHandlerClass(
        getAtomicHandler: IrExpression,
        functionName: String,
        valueArguments: List<IrExpression?>,
        valueType: IrType,
    ): IrCall {
        val atomicHandlerClassSymbol = (getAtomicHandler.type as IrSimpleType).classOrNull
            ?: error("Failed to obtain the ClassSymbol of the type ${getAtomicHandler.render()}.")
        val functionSymbol = when (functionName) {
            "get", "<get-value>", "getValue" -> atomicHandlerClassSymbol.getSimpleFunction("get")
            "set", "<set-value>", "setValue", "lazySet" -> atomicHandlerClassSymbol.getSimpleFunction("set")
            else -> atomicHandlerClassSymbol.getSimpleFunction(functionName)
        } ?: error("No $functionName function found in ${atomicHandlerClassSymbol.owner.render()}")
        return irCallFunction(
            functionSymbol,
            dispatchReceiver = getAtomicHandler,
            extensionReceiver = null,
            valueArguments,
            valueType
        )
    }

    abstract fun invokeFunctionOnAtomicHandler(
        atomicHandlerType: AtomicHandlerType,
        getAtomicHandler: IrExpression,
        functionName: String,
        valueArguments: List<IrExpression?>,
        valueType: IrType,
    ): IrCall

    fun irGetProperty(property: IrProperty, dispatchReceiver: IrExpression?) =
        irCall(property.getter?.symbol ?: error("Getter is not defined for the property ${property.atomicfuRender()}")).apply {
            this.dispatchReceiver = dispatchReceiver?.deepCopyWithSymbols()
        }

    protected fun irVolatileField(
        name: String,
        valueType: IrType,
        annotations: List<IrConstructorCall>,
        parentContainer: IrDeclarationContainer,
    ): IrField {
        return context.irFactory.buildField {
            this.name = Name.identifier(name + VOLATILE)
            this.type = valueType
            isFinal = false
            isStatic = parentContainer is IrFile
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            this.annotations = annotations + atomicfuSymbols.volatileAnnotationConstructorCall
            this.parent = parentContainer
        }
    }

    fun buildVolatileField(
        atomicfuProperty: IrProperty,
        parentContainer: IrDeclarationContainer
    ): IrField {
        val atomicfuField = requireNotNull(atomicfuProperty.backingField) {
            "The backing field of the atomic property ${atomicfuProperty.atomicfuRender()} declared in ${parentContainer.render()} should not be null." + CONSTRAINTS_MESSAGE
        }
        return buildAndInitializeNewField(atomicfuField, parentContainer) { atomicFactoryCall: IrExpression ->
            val valueType = atomicfuSymbols.atomicToPrimitiveType(atomicfuField.type as IrSimpleType)
            val initValue = atomicFactoryCall.getAtomicFactoryValueArgument()
            buildVolatileFieldOfType(atomicfuProperty.name.asString(), valueType, atomicfuField.annotations, initValue, parentContainer)
        }
    }

    // atomic(value = 0) -> 0
    internal fun IrExpression.getAtomicFactoryValueArgument(): IrExpression {
        require(this is IrCall) { "Expected atomic factory invocation but found: ${this.render()}" }
        return getValueArgument(0)?.deepCopyWithSymbols()
            ?: error("Atomic factory should take at least one argument: ${this.render()}" + CONSTRAINTS_MESSAGE)
    }

    abstract fun buildVolatileFieldOfType(
        name: String,
        valueType: IrType,
        annotations: List<IrConstructorCall>,
        initExpr: IrExpression?,
        parentContainer: IrDeclarationContainer,
    ): IrField

    fun irAtomicArrayField(
        atomicfuProperty: IrProperty,
        parentContainer: IrDeclarationContainer
    ): IrField {
        val atomicfuArrayField = requireNotNull(atomicfuProperty.backingField) {
            "The backing field of the atomic array [${atomicfuProperty.atomicfuRender()}] should not be null." + CONSTRAINTS_MESSAGE
        }
        return buildAndInitializeNewField(atomicfuArrayField, parentContainer) { atomicFactoryCall: IrExpression ->
            val size = atomicFactoryCall.getArraySizeArgument()
            val arrayClass = atomicfuSymbols.getAtomicArrayHanlderType(atomicfuArrayField.type)
            val valueType = atomicfuSymbols.atomicArrayToPrimitiveType(atomicfuArrayField.type)
            context.irFactory.buildField {
                this.name = atomicfuArrayField.name
                type = arrayClass.defaultType
                this.isFinal = true
                this.isStatic = atomicfuArrayField.isStatic
                visibility = DescriptorVisibilities.PRIVATE
                origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
            }.apply {
                this.initializer = context.irFactory.createExpressionBody(
                    newAtomicArray(arrayClass, size, valueType, (atomicFactoryCall as IrFunctionAccessExpression).dispatchReceiver)
                )
                this.annotations = annotations
                this.parent = parentContainer
            }
        }
    }

    // AtomicIntArray(size = 10) -> 10
    private fun IrExpression.getArraySizeArgument(): IrExpression {
        require(this is IrFunctionAccessExpression) {
            "Expected atomic array factory invocation, but found: ${this.render()}."
        }
        return getValueArgument(0)?.deepCopyWithSymbols()
            ?: error("Atomic array factory should take at least one argument: ${this.render()}" + CONSTRAINTS_MESSAGE)
    }

    protected fun buildAndInitializeNewField(
        oldAtomicField: IrField,
        parentContainer: IrDeclarationContainer,
        newFieldBuilder: (IrExpression) -> IrField
    ): IrField {
        val initializer = oldAtomicField.initializer?.expression
        return if (initializer == null) {
            // replace field initialization in the init block
            val (initBlock, initExprWithIndex) = oldAtomicField.getInitBlockWithIndexedInitExpr(parentContainer)
            val atomicFactoryCall = initExprWithIndex.value.value
            val initExprIndex = initExprWithIndex.index
            newFieldBuilder(atomicFactoryCall).also { newField ->
                val initExpr = newField.initializer?.expression
                    ?: error("The generated field [${newField.render()}] should've already be initialized.")
                newField.initializer = null
                updateFieldInitialization(initBlock, newField.symbol, initExpr, initExprIndex)
            }
        } else {
            newFieldBuilder(initializer)
        }
    }

    private fun updateFieldInitialization(
        initBlock: IrAnonymousInitializer,
        volatileFieldSymbol: IrFieldSymbol,
        initExpr: IrExpression,
        index: Int
    ) {
        val oldIrSetField = initBlock.body.statements[index] as IrSetField
        // save the order of field initialization in init block
        initBlock.body.statements[index] = irSetField(oldIrSetField.receiver, volatileFieldSymbol.owner, initExpr)
    }

    private fun IrField.getInitBlockWithIndexedInitExpr(parentContainer: IrDeclarationContainer): Pair<IrAnonymousInitializer, IndexedValue<IrSetField>> {
        for (declaration in parentContainer.declarations) {
            if (declaration is IrAnonymousInitializer) {
                declaration.body.statements.withIndex().singleOrNull { it.value is IrSetField && (it.value as IrSetField).symbol == this.symbol }?.let {
                    @Suppress("UNCHECKED_CAST")
                    return declaration to it as IndexedValue<IrSetField>
                }
            }
        }
        error(
            "Failed to find initialization of the property [${this.correspondingPropertySymbol?.owner?.render()}] in the init block of the class [${this.parent.render()}].\n" +
                    "Please avoid complex data flow in property initialization, e.g. instead of this:\n" +
                    "```\n" +
                    "val a: AtomicInt\n" +
                    "init {\n" +
                    "  if (foo()) {\n" +
                    "    a = atomic(0)\n" +
                    "  } else { \n" +
                    "    a = atomic(1)\n" +
                    "  }\n" +
                    "}\n" +
                    "use simple direct assignment expression to initialize the property:\n" +
                    "```\n" +
                    "val a: AtomicInt\n" +
                    "init {\n" +
                    "  val initValue = if (foo()) 0 else 1\n" +
                    "  a = atomic(initValue)\n" +
                    "}\n" +
                    "```\n" + CONSTRAINTS_MESSAGE
        )
    }

    private fun IrClassSymbol.getSingleArgCtorOrNull(predicate: (IrType) -> Boolean): IrConstructorSymbol? =
        constructors.filter { it.owner.valueParameters.size == 1 && predicate(it.owner.valueParameters[0].type) }.singleOrNull()

    protected fun callArraySizeConstructor(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        dispatchReceiver: IrExpression?,
    ): IrFunctionAccessExpression? =
        atomicArrayClass.getSingleArgCtorOrNull{ argType -> argType.isInt() }?.let { cons ->
            return irCall(cons).apply {
                putValueArgument(0, size)
                this.dispatchReceiver = dispatchReceiver
            }
        }

    protected fun callArraySizeAndInitConstructor(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?,
    ): IrFunctionAccessExpression? =
        atomicArrayClass.getSingleArgCtorOrNull { argType -> argType.isArray() }?.let { cons ->
            return irCall(cons).apply {
                val arrayOfNulls = irCall(atomicfuSymbols.arrayOfNulls).apply {
                    typeArguments[0] = valueType
                    putValueArgument(0, size)
                }
                typeArguments[0] = valueType
                putValueArgument(0, arrayOfNulls)
                this.dispatchReceiver = dispatchReceiver
            }
        }

    abstract fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?,
    ): IrFunctionAccessExpression

    fun irPropertyReference(property: IrProperty, classReceiver: IrExpression?): IrPropertyReferenceImpl {
        val backingField = requireNotNull(property.backingField) { "Backing field of the property $property should not be null" }
        return IrPropertyReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = backingField.type,
            symbol = property.symbol,
            typeArgumentsCount = 0,
            field = backingField.symbol,
            getter = property.getter?.symbol,
            setter = property.setter?.symbol
        ).apply {
            dispatchReceiver = classReceiver
        }
    }

    fun irVolatilePropertyRefGetter(
        irPropertyReference: IrExpression,
        propertyName: String,
        parentFunction: IrFunction
    ): IrExpression =
        IrFunctionExpressionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = atomicfuSymbols.function0Type(irPropertyReference.type),
            function = irBuiltIns.irFactory.buildFun {
                name = Name.identifier("<$propertyName-getter-${nextGetterCounterId()}>")
                origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
                returnType = irPropertyReference.type
                isInline = true
                visibility = DescriptorVisibilities.LOCAL
            }.apply {
                val lambda = this
                body = irBlockBody {
                    +irReturn(
                        irPropertyReference
                    ).apply {
                        type = irBuiltIns.nothingType
                        returnTargetSymbol = lambda.symbol
                    }
                }
                this.parent = parentFunction
            },
            origin = IrStatementOrigin.LAMBDA
        )

    fun invokePropertyGetter(refGetter: IrExpression) = irCall(atomicfuSymbols.invoke0Symbol).apply { dispatchReceiver = refGetter }
    fun toBoolean(irExpr: IrExpression) = irEquals(irExpr, irInt(1)) as IrCall
    fun toInt(irExpr: IrExpression) = irIfThenElse(irBuiltIns.intType, irExpr, irInt(1), irInt(0))

    fun buildPropertyWithAccessors(
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

    private fun IrValueParameter.getAtomicHandler(atomicHandlerType: AtomicHandlerType) =
        if (atomicHandlerType == AtomicHandlerType.NATIVE_PROPERTY_REF) invokePropertyGetter(irGet(this)) else irGet(this)

    /**
     * K/N for a property reference atomic handler:
     * inline fun loop$atomicfu(refGetter: () -> KMutableProperty0<Int>, action: (Int) -> Unit) {
     *     while (true) {
     *         val cur = refGetter().get()
               action(cur)
     *     }
     * }
     *
     * JVM:
     * inline fun loop$atomicfu(atomicFieldUpdater: AtomicIntegerFieldUpdater, obj: Any, action: (Int) -> Unit) {
     *     while (true) {
     *         val cur = atomicFieldUpdater.get(obj)
     *         action(cur)
     *     }
     * }
     *
     * inline fun loop$atomicfu(boxedAtomic: AtomicInteger, action: (Int) -> Unit) {
     *     while (true) {
     *         val cur = boxedAtomic.get()
     *         action(cur)
     *     }
     * }
     *
     * inline fun loop$atomicfu(atomicArray: AtomicIntegerArray, index: Int, action: (Int) -> Unit) {
     *     while (true) {
     *         val cur = atomicArray.get(index)
     *         action(cur)
     *     }
     * }
     */
    fun generateLoopBody(atomicHandlerType: AtomicHandlerType, valueType: IrType, valueParameters: List<IrValueParameter>) =
        irBlockBody {
            val atomicHandlerParam = valueParameters[0] // ATOMIC_HANDLER
            val hasExtraArg = atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY || atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER
            val extraArg = if (hasExtraArg) valueParameters[1] else null
            val action = if (hasExtraArg) valueParameters[2] else valueParameters[1]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        irExpression = invokeFunctionOnAtomicHandler(
                            atomicHandlerType = atomicHandlerType,
                            getAtomicHandler = atomicHandlerParam.getAtomicHandler(atomicHandlerType),
                            functionName = "get",
                            valueArguments = if (extraArg != null) listOf(irGet(extraArg)) else emptyList(),
                            valueType = valueType
                        ),
                        nameHint = "atomicfu\$cur", false
                    )
                    +irCall(atomicfuSymbols.invoke1Symbol).apply {
                        this.dispatchReceiver = irGet(action)
                        putValueArgument(0, irGet(cur))
                    }
                }
            }
        }

    /**
     * K/N:
     * inline fun getAndUpdate$atomicfu(refGetter: () -> KMutableProperty0<Int>, action: (Int) -> Int): Int {
     *     while (true) {
     *         val cur = refGetter().get()
     *.        val upd = action(cur)
     *         if (refGetter().compareAndSetField(cur, upd)) return cur
     *     }
     * }
     *
     * JVM:
     * inline fun getAndUpdate$atomicfu(atomicFieldUpdater: AtomicIntegerFieldUpdater, obj: Any, action: (Int) -> Int): Int {
     *     while (true) {
     *         val cur = atomicFieldUpdater.get(obj)
     *         val upd = action(cur)
     *         if (atomicFieldUpdater.compareAndSet(cur, upd)) return cur
     *     }
     * }
     *
     * inline fun getAndUpdate$atomicfu(boxedAtomic: AtomicInteger, action: (Int) -> Int): Int {
     *     while (true) {
     *         val cur = boxedAtomic.get()
     *         val upd = action(cur)
     *         if (boxedAtomic.compareAndSet(cur, upd)) return cur
     *     }
     * }
     *
     * inline fun getAndUpdate$atomicfu(atomicArray: AtomicIntegerArray, index: Int, action: (Int) -> Int): Int {
     *     while (true) {
     *         val cur = atomicArray.get(index)
     *         val upd = action(cur)
     *         if (atomicArray.compareAndSet(index, cur, upd)) return cur
     *     }
     * }
     */
    fun generateUpdateBody(atomicHandlerType: AtomicHandlerType, valueType: IrType, valueParameters: List<IrValueParameter>, functionName: String) =
        irBlockBody {
            val atomicHandlerParam = valueParameters[0] // ATOMIC_HANDLER
            val getAtomicHandler = atomicHandlerParam.getAtomicHandler(atomicHandlerType)
            val hasExtraArg = atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY || atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER
            val extraArg = if (hasExtraArg) valueParameters[1] else null
            val action = if (hasExtraArg) valueParameters[2] else valueParameters[1]
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        irExpression = invokeFunctionOnAtomicHandler(
                            atomicHandlerType = atomicHandlerType,
                            getAtomicHandler = getAtomicHandler.deepCopyWithSymbols(),
                            functionName = "get",
                            valueArguments = if (extraArg != null) listOf(irGet(extraArg)) else emptyList(),
                            valueType = valueType
                        ),
                        nameHint = "atomicfu\$cur", false
                    )
                    val upd = createTmpVariable(
                        irCall(atomicfuSymbols.invoke1Symbol).apply {
                            dispatchReceiver = irGet(action)
                            putValueArgument(0, irGet(cur))
                        }, "atomicfu\$upd", false
                    )
                    +irIfThen(
                        type = irBuiltIns.unitType,
                        condition = invokeFunctionOnAtomicHandler(
                            atomicHandlerType = atomicHandlerType,
                            getAtomicHandler = getAtomicHandler.deepCopyWithSymbols(),
                            functionName = "compareAndSet",
                            valueArguments = buildList { if (extraArg != null) add(irGet(extraArg)); add(irGet(cur)); add(irGet(upd)) },
                            valueType = valueType
                        ),
                        thenPart = when (functionName) {
                            "update" -> irReturnUnit()
                            "getAndUpdate" -> irReturn(irGet(cur))
                            "updateAndGet" -> irReturn(irGet(upd))
                            else -> error("Unsupported atomicfu inline loop function name: $functionName")
                        }
                    )
                }
            }
        }

    companion object {
        // This counter is used to ensure uniqueness of functions for refGetter lambdas,
        // as several functions with the same name may be created in the same scope
        private var refGetterCounter = AtomicInteger(0)

        private fun nextGetterCounterId() = refGetterCounter.getAndIncrement()
    }
}
