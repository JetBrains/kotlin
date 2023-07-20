/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer
import kotlin.collections.set

private const val ATOMICFU = "atomicfu"
private const val DISPATCH_RECEIVER = "dispatchReceiver\$$ATOMICFU"
private const val ATOMIC_HANDLER = "handler\$$ATOMICFU"
private const val ACTION = "action\$$ATOMICFU"
private const val INDEX = "index\$$ATOMICFU"
private const val LOOP = "loop"
private const val UPDATE = "update"

class AtomicfuJvmIrTransformer(
    pluginContext: IrPluginContext,
    override val atomicSymbols: JvmAtomicSymbols
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicPropertiesTransformer: AtomicPropertiesTransformer
        get() = JvmAtomicPropertiesTransformer()

    override val atomicExtensionsTransformer: AtomicExtensionTransformer
        get() = JvmAtomicExtensionTransformer()

    override val atomicFunctionsTransformer: AtomicFunctionCallTransformer
        get() = JvmAtomicFunctionCallTransformer()

    private inner class JvmAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun IrClass.addTransformedInClassAtomic(atomicProperty: IrProperty, index: Int): IrProperty {
            /**
             * Atomic property is replaced with the private volatile field that is atomically updated via
             * java.util.concurrent.Atomic*FieldUpdater class.
             * Volatile field is private and the atomic updater has the minimal visibility level
             * among the visibility of the atomic property and the visibility of the containing class.
             *
             * private val a = atomic(0)  --> private @Volatile var a: Int = 0
             *                                private static val a$FU = AtomicIntegerFieldUpdater.newUpdater(parentClass, "a")
             */
            return addVolatilePropertyWithAtomicUpdater(atomicProperty, index)
        }

        override fun IrDeclarationContainer.addTransformedStaticAtomic(atomicProperty: IrProperty, index: Int): IrProperty {
            /**
             * Atomic property is replaced with the private volatile field that is atomically updated via
             * java.util.concurrent.Atomic*FieldUpdater class. Atomic*FieldUpdater can only update a field that is a member of a class.
             * For this reason, all volatile fields are placed inside the generated `VolatileWrapper` class.
             * VolatileWrapper class and it's instance have the minimal visibility level
             * among the visibility of the atomic property and the visibility of the containing object (if the atomic property is member of the object).
             *
             * One wrapper class contains all properties of the same visibility, e.g.:
             *
             * internal class AVolatileWrapper$internal {
             *   private @Volatile var a: Int = 0
             *
             *   companion object {
             *     internal static val a$FU = AtomicIntegerFieldUpdater.newUpdater(AVolatileWrapper::class, "a")
             *   }
             * }
             * internal val wrapperClassInstance = AVolatileWrapper$internal()
             */
            val wrapperClass = getOrBuildVolatileWrapper(atomicProperty)
            return wrapperClass.addVolatilePropertyWithAtomicUpdater(atomicProperty, index)
        }

        override fun IrDeclarationContainer.addTransformedAtomicArray(atomicProperty: IrProperty, index: Int): IrProperty {
            /**
             * Atomic arrays are replaced with corresponding java.util.concurrent.Atomic*Array:
             *
             * val intArr = kotlinx.atomicfu.AtomicIntArray(45)  -->   val intArr = java.util.concurrent.AtomicIntegerArray(45)
             */
            val parentContainer = this
            with(atomicSymbols.createBuilder(atomicProperty.symbol)) {
                val javaAtomicArrayField = buildJavaAtomicArrayField(atomicProperty, parentContainer)
                return parentContainer.replacePropertyAtIndex(
                    javaAtomicArrayField,
                    atomicProperty.visibility,
                    isVar = false,
                    isStatic = parentContainer is IrFile,
                    index
                ).also {
                    propertyToAtomicHandler[atomicProperty] = it
                }
            }
        }

        private fun IrClass.addVolatilePropertyWithAtomicUpdater(from: IrProperty, index: Int): IrProperty {
            /**
             * Generates a volatile property and an atomic updater for this property,
             * adds both to the parent class and returns the volatile property.
             */
            val parentClass = this
            with(atomicSymbols.createBuilder(from.symbol)) {
                /**
                 * Property initialization order matters,
                 * the new volatile property should be inserted in parent declarations in place of the original property.
                 * Consider the case when the transformed property is added to the end of parent.declarations:
                 *
                 * class A {                           class A {
                 *   private val _a = atomic(5)          // _a is removed
                 *   init {                       --->   init {
                 *     assertEquals(5, _a.value)           assertEquals(5, _a$volatile$FU.get(this)) // FAILS, _a$volatile$FU.get(this) == 0
                 *   }                                   }
                 *                                       // transformed volatile property + updater are added to the end of class declarations ->
                 *                                       // wrong order of initialization
                 *                                       @Volatile var _a$volatile = 5
                 *                                       companion object {
                 *                                         val _a$volatile$FU = AtomicIntegerFieldUpdater.newUpdater(A::class.java, "_a$volatile")
                 *                                       }
                 * }                                   }
                 */
                val volatileField = buildVolatileBackingField(from, parentClass, true)
                val volatileProperty = if (volatileField.parent == from.parent) {
                    // The index is relevant only if the property belongs to the same class as the original atomic property (not the generated wrapper).
                    parentClass.replacePropertyAtIndex(volatileField, DescriptorVisibilities.PRIVATE, isVar = true, isStatic = false, index)
                } else {
                    parentClass.addProperty(volatileField, DescriptorVisibilities.PRIVATE, isVar = true, isStatic = false)
                }
                atomicPropertyToVolatile[from] = volatileProperty
                val atomicUpdaterField = irJavaAtomicFieldUpdater(volatileField, parentClass)
                parentClass.addProperty(atomicUpdaterField, from.getMinVisibility(), isVar = false, isStatic = true).also {
                    propertyToAtomicHandler[from] = it
                }
                return volatileProperty
            }
        }

        private fun JvmAtomicfuIrBuilder.buildJavaAtomicArrayField(
            atomicProperty: IrProperty,
            parentContainer: IrDeclarationContainer
        ): IrField {
            val atomicArrayField =
                requireNotNull(atomicProperty.backingField) { "BackingField of atomic array $atomicProperty should not be null" }
            val initializer = atomicArrayField.initializer?.expression
            if (initializer == null) {
                // replace field initialization in the init block
                val initBlock = atomicArrayField.getInitBlockForField(parentContainer)
                // property initialization order in the init block matters -> transformed initializer should be placed at the same position
                val initExprWithIndex = initBlock.getInitExprWithIndexFromInitBlock(atomicArrayField.symbol)
                    ?: error("Expected atomic array ${atomicProperty.render()} initialization in init block ${initBlock.render()}" + CONSTRAINTS_MESSAGE)
                val atomicFactoryCall = initExprWithIndex.value.value
                val initExprIndex = initExprWithIndex.index
                val arraySize = atomicFactoryCall.getArraySizeArgument()
                return irJavaAtomicArrayField(
                    atomicArrayField.name,
                    atomicSymbols.getAtomicArrayClassByAtomicfuArrayType(atomicArrayField.type),
                    atomicArrayField.isStatic,
                    atomicArrayField.annotations,
                    arraySize,
                    (atomicFactoryCall as IrFunctionAccessExpression).dispatchReceiver,
                    parentContainer
                ).also {
                    val initExpr = it.initializer?.expression ?: error("Initializer of the generated field ${it.render()} can not be null" + CONSTRAINTS_MESSAGE)
                    it.initializer = null
                    initBlock.updateFieldInitialization(atomicArrayField.symbol, it.symbol, initExpr, initExprIndex)
                }
            } else {
                val arraySize = initializer.getArraySizeArgument()
                return irJavaAtomicArrayField(
                    atomicArrayField.name,
                    atomicSymbols.getAtomicArrayClassByAtomicfuArrayType(atomicArrayField.type),
                    atomicArrayField.isStatic,
                    atomicArrayField.annotations,
                    arraySize,
                    (initializer as IrFunctionAccessExpression).dispatchReceiver,
                    parentContainer
                )
            }
        }

        private fun IrDeclarationContainer.getOrBuildVolatileWrapper(atomicProperty: IrProperty): IrClass {
            val visibility = atomicProperty.getMinVisibility()
            findDeclaration<IrClass> { it.isVolatileWrapper(visibility) && it.visibility == visibility }?.let { return it }
            val parentContainer = this
            return with(atomicSymbols.createBuilder((this as IrSymbolOwner).symbol)) {
                irClassWithPrivateConstructor(
                    mangleVolatileWrapperClassName(parentContainer) + "\$${visibility.name}",
                    visibility,
                    parentContainer
                ).also {
                    val wrapperInstance = buildClassInstance(it, parentContainer, true)
                    addProperty(wrapperInstance, atomicProperty.getMinVisibility(), isVar = false, isStatic = true)
                }
            }
        }
    }

    private inner class JvmAtomicExtensionTransformer : AtomicExtensionTransformer() {

        override fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.add(transformAtomicExtension(atomicExtension, this, false))
                declarations.add(transformAtomicExtension(atomicExtension, this, true))
                // the original atomic extension is removed
                declarations.remove(atomicExtension)
            }
        }

        private fun transformAtomicExtension(atomicExtension: IrFunction, parent: IrDeclarationContainer, isArrayReceiver: Boolean): IrFunction {
            /**
             * At this step, only signature of the atomic extension is changed,
             * the body is just copied and will be transformed at the next step by JvmAtomicFunctionCallTransformer.
             *
             * Two different signatures are generated for the case of atomic property receiver
             * and for the case of atomic array element receiver, due to different atomic updaters.
             *
             * inline fun AtomicInt.foo(arg: Int) --> inline fun foo$atomicfu(dispatchReceiver: Any?, atomicHandler: AtomicIntegerFieldUpdater, arg': Int)
             *                                        inline fun foo$atomicfu$array(dispatchReceiver: Any?, atomicHandler: AtomicIntegerArray, index: Int, arg': Int)
             */
            return buildTransformedAtomicExtensionDeclaration(atomicExtension, isArrayReceiver).apply {
                // the body will be transformed later by `AtomicFUTransformer`
                body = atomicExtension.body?.deepCopyWithSymbols(this)
                body?.transform(
                    object : IrElementTransformerVoid() {
                        override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
                            if (expression.returnTargetSymbol == atomicExtension.symbol) {
                                with(atomicSymbols.createBuilder(this@apply.symbol)) {
                                    irReturn(expression.value)
                                }
                            } else {
                                expression
                            }
                        )
                    }, null
                )
                this.parent = parent
            }
        }
    }

    private inner class JvmAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun transformAtomicUpdateCallOnProperty(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Atomic update call on the atomic property is replaced
                 * with the call on the AtomicFieldUpdater field:
                 *
                 * 1. Function call receiver is atomic property getter call.
                 *
                 * The call is replaced with the call on the corresponding field updater:
                 *
                 * val a = atomic(0)                    @Volatile var a$volatile = 0
                 * <get-a>().compareAndSet(0, 5)  --->  a$volatile$FU.compareAndSetField(dispatchReceiver, 0, 5)
                 *
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension:
                 *
                 * inline fun AtomicInt.foo(new: Int) {          inline fun foo$atomicfu(dispatchReceiver: Any?, atomicHandler: AtomicIntegerFieldUpdater, new: Int) {
                 *   this.compareAndSet(value, new)       --->     atomicHandler.compareAndSet(dispatchReceiver, atomicHandler.get(dispatchReceiver), new)
                 * }                                             }
                 */
                return callFieldUpdater(
                    fieldUpdaterSymbol = atomicSymbols.getJucaAFUClass(valueType),
                    functionName = functionName,
                    getAtomicHandler = getAtomicHandler(getPropertyReceiver, parentFunction),
                    classInstanceContainingField = getDispatchReceiver(getPropertyReceiver, parentFunction),
                    valueArguments = expression.valueArguments,
                    castType = castType,
                    isBooleanReceiver = valueType.isBoolean()
                )
            }
        }

        override fun transformAtomicUpdateCallOnArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Atomic update call on the atomic array element is replaced
                 * with the call on the j.u.c.a.Atomic*Array field:
                 *
                 * 1. Function call receiver is atomic property getter call.
                 *
                 * The call is replaced with the call on the corresponding field updater:
                 *
                 * val intArr = AtomicIntArray(10)              val intArr = AtomicIntegerArray(10)
                 * <get-intArr>()[5].compareAndSet(0, 5)  --->  intArr.compareAndSet(5, 0, 5)
                 *
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension:
                 *
                 * inline fun AtomicInt.foo(new: Int) {          inline fun foo$atomicfu$array(dispatchReceiver: Any?, atomicHandler: AtomicIntegerArray, index: Int, arg': Int)
                 *   this.getAndSet(value, new)            --->    atomicHandler.getAndSet(index, new)
                 * }                                             }
                 */
                val getJavaAtomicArray = getAtomicHandler(getPropertyReceiver, parentFunction)
                return callAtomicArray(
                    arrayClassSymbol = getJavaAtomicArray.type.classOrNull!!,
                    functionName = functionName,
                    dispatchReceiver = getJavaAtomicArray,
                    index = getPropertyReceiver.getArrayElementIndex(parentFunction),
                    valueArguments = expression.valueArguments,
                    isBooleanReceiver = valueType.isBoolean()
                )
            }
        }

        override fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                val dispatchReceiver = getDispatchReceiver(getPropertyReceiver, parentFunction)
                val getAtomicHandler = getAtomicHandler(getPropertyReceiver, parentFunction)
                /**
                 * a.loop { value -> a.compareAndSet(value, 777) } -->
                 *
                 * inline fun <T> atomicfu$loop(atomicfu$handler: AtomicIntegerFieldUpdater, atomicfu$action: (Int) -> Unit, dispatchReceiver: Any?) {
                 *  while (true) {
                 *    val cur = atomicfu$handler.get()
                 *    atomicfu$action(cur)
                 *   }
                 * }
                 *
                 * a.atomicfu$loop(dispatchReceiver, atomicHandler) { ... }
                 */
                requireNotNull(parentFunction) { "Parent function of this call ${expression.render()} is null" + CONSTRAINTS_MESSAGE }
                val loopFunc = parentFunction.parentDeclarationContainer.getOrBuildInlineLoopFunction(
                    functionName = functionName,
                    valueType = if (valueType.isBoolean()) irBuiltIns.intType else valueType,
                    isArrayReceiver = isArrayReceiver
                )
                val action = (expression.getValueArgument(0) as IrFunctionExpression).apply {
                    function.body?.transform(this@JvmAtomicFunctionCallTransformer, parentFunction)
                    if (function.valueParameters[0].type.isBoolean()) {
                        function.valueParameters[0].type = irBuiltIns.intType
                        function.returnType = irBuiltIns.intType
                    }
                }
                return irCallWithArgs(
                    symbol = loopFunc.symbol,
                    dispatchReceiver = parentFunction.containingFunction.dispatchReceiverParameter?.capture(),
                    extensionReceiver = null,
                    valueArguments = if (isArrayReceiver) {
                        val index = getPropertyReceiver.getArrayElementIndex(parentFunction)
                        listOf(getAtomicHandler, index, action)
                    } else {
                        listOf(getAtomicHandler, action, dispatchReceiver)
                    }
                )
            }
        }

        override fun transformAtomicExtensionCall(
            expression: IrCall,
            originalAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Atomic extension call is replaced with the call to transformed atomic extension:
                 *
                 * 1. Function call receiver is atomic property getter call.
                 * Transformation variant of the atomic extension is chosen accorfin to the type of receiver
                 * (atomic property -> foo$atomicfu, atomic array element -> foo$atomicfu$array)
                 *
                 * inline fun AtomicInt.foo(atg: Int) {..}
                 * aClass._a.foo(arg)                              --> foo$atomicfu(aClass, _a$volatile$FU, arg)
                 *
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension.
                 * In this case value parameters captured from the parent function are passed as arguments.
                 *
                 * inline fun AtomicInt.bar(new: Int)            inline fun bar$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int)
                 * inline fun AtomicInt.foo(new: Int) {          inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int)
                 *   bar(new)                              --->    bar$atomicfu(dispatchReceiver, handler, new)
                 * }                                             }
                 */
                val parent = originalAtomicExtension.parent as IrDeclarationContainer
                val transformedAtomicExtension = parent.getTransformedAtomicExtension(originalAtomicExtension, isArrayReceiver)
                val dispatchReceiver = getDispatchReceiver(getPropertyReceiver, parentFunction)
                val getAtomicHandler = getAtomicHandler(getPropertyReceiver, parentFunction)
                return callAtomicExtension(
                    symbol = transformedAtomicExtension.symbol,
                    dispatchReceiver = expression.dispatchReceiver,
                    syntheticValueArguments = if (isArrayReceiver) {
                        listOf(dispatchReceiver, getAtomicHandler, getPropertyReceiver.getArrayElementIndex(parentFunction))
                    } else {
                        listOf(dispatchReceiver, getAtomicHandler)
                    },
                    valueArguments = expression.valueArguments
                )
            }
        }

        override fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            // Try find the transformed atomic extension in the parent container
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangleAtomicExtensionName(declaration.name.asString(), isArrayReceiver) &&
                        it.isTransformedAtomicExtension()
            }?.let { return it }
            /**
             * If the transformed declaration is not found then the call may be performed from another module
             * which depends on the module where declarations are generated from untransformed metadata (real transformed declarations are not there).
             * This happens if the call is performed from the test module or in case of incremental compilation.
             *
             * We build a fake declaration here: it's signature equals the one of the real transformed declaration,
             * it doesn't have body and won't be generated. It is placed in the call site and
             * during compilation this fake declaration will be resolved to the real transformed declaration.
             */
            return buildTransformedAtomicExtensionDeclaration(declaration, isArrayReceiver)
        }

        override fun IrFunction.isTransformedAtomicExtension(): Boolean {
            val isArrayReceiver = name.asString().isMangledAtomicArrayExtension()
            return if (isArrayReceiver) checkSyntheticArrayElementExtensionParameter() else checkSyntheticAtomicExtensionParameters()
        }

        override fun IrValueParameter.remapValueParameter(transformedExtension: IrFunction): IrValueParameter? {
            if (index < 0 && !type.isAtomicValueType()) {
                // data is a transformed function
                // index == -1 for `this` parameter
                return transformedExtension.dispatchReceiverParameter ?: error("Dispatch receiver of ${transformedExtension.render()} is null" + CONSTRAINTS_MESSAGE)
            }
            if (index >= 0) {
                val shift = if (transformedExtension.name.asString().isMangledAtomicArrayExtension()) 3 else 2
                return transformedExtension.valueParameters[index + shift]
            }
            return null
        }

        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            val receiver = this
            return when {
                receiver is IrCall -> {
                    receiver.isArrayElementGetter()
                }
                receiver.isThisReceiver() -> {
                    if (parentFunction != null && parentFunction.isTransformedAtomicExtension()) {
                        val atomicHandler = parentFunction.valueParameters[1].capture()
                        atomicSymbols.isAtomicArrayHandlerType(atomicHandler.type)
                    } else false
                }
                else -> false
            }
        }

        private fun getDispatchReceiver(atomicCallReceiver: IrExpression, parentFunction: IrFunction?) =
            when {
                atomicCallReceiver is IrCall -> atomicCallReceiver.getDispatchReceiver()
                atomicCallReceiver.isThisReceiver() -> {
                    if (parentFunction != null && parentFunction.isTransformedAtomicExtension()) {
                        parentFunction.valueParameters[0].capture()
                    } else null
                }
                else -> error("Unexpected type of atomic function call receiver: ${atomicCallReceiver.render()}" + CONSTRAINTS_MESSAGE)
            }

        private fun getAtomicHandler(atomicCallReceiver: IrExpression, parentFunction: IrFunction?): IrExpression =
            when {
                atomicCallReceiver is IrCall -> {
                    val isArrayReceiver = atomicCallReceiver.isArrayElementGetter()
                    val getAtomicProperty = if (isArrayReceiver) atomicCallReceiver.dispatchReceiver as IrCall else atomicCallReceiver
                    val atomicProperty = getAtomicProperty.getCorrespondingProperty()
                    val atomicHandlerProperty = propertyToAtomicHandler[atomicProperty]
                        ?: error("No atomic handler found for the atomic property ${atomicProperty.render()}, \n" +
                                         "these properties were registered: ${
                                             propertyToAtomicHandler.keys.toList().joinToString("\n") { it.render() }
                                         }" + CONSTRAINTS_MESSAGE)
                    with(atomicSymbols.createBuilder(atomicCallReceiver.symbol)) {
                        // dispatchReceiver for get-a$FU() is null, because a$FU is a static property
                        // dispatchReceiver for get-arr'() is equal to the dispatchReceiver of the original getter
                        irGetProperty(atomicHandlerProperty, if (isArrayReceiver) getAtomicProperty.dispatchReceiver else null)
                    }
                }
                atomicCallReceiver.isThisReceiver() -> {
                    requireNotNull(parentFunction) { "Containing function of the atomic call with <this> receiver should not be null" + CONSTRAINTS_MESSAGE}
                    require(parentFunction.isTransformedAtomicExtension())
                    parentFunction.valueParameters[1].capture()
                }
                else -> error("Unexpected type of atomic function call receiver: ${atomicCallReceiver.render()} \n" + CONSTRAINTS_MESSAGE)
            }

        private fun IrCall.getDispatchReceiver(): IrExpression? {
            val isArrayReceiver = isArrayElementGetter()
            val getAtomicProperty = if (isArrayReceiver) dispatchReceiver as IrCall else this
            val atomicProperty = getAtomicProperty.getCorrespondingProperty()
            val dispatchReceiver = getAtomicProperty.dispatchReceiver
            // top-level atomics
            if (!isArrayReceiver && (dispatchReceiver == null || dispatchReceiver.type.isObject())) {
                val volatileProperty = atomicPropertyToVolatile[atomicProperty]!!
                return getStaticVolatileWrapperInstance(volatileProperty.parentAsClass)
            }
            return dispatchReceiver
        }

        private fun IrExpression.getArrayElementIndex(parentFunction: IrFunction?): IrExpression =
            when {
                this is IrCall -> getValueArgument(0)!!
                this.isThisReceiver() -> {
                    require(parentFunction != null)
                    parentFunction.valueParameters[2].capture()
                }
                else -> error("Unexpected type of atomic receiver expression: ${this.render()} \n" + CONSTRAINTS_MESSAGE)
            }

        private fun IrFunction.checkSyntheticArrayElementExtensionParameter(): Boolean {
            if (valueParameters.size < 3) return false
            return valueParameters[0].name.asString() == DISPATCH_RECEIVER && valueParameters[0].type == irBuiltIns.anyNType &&
                    valueParameters[1].name.asString() == ATOMIC_HANDLER && atomicSymbols.isAtomicArrayHandlerType(valueParameters[1].type) &&
                    valueParameters[2].name.asString() == INDEX && valueParameters[2].type == irBuiltIns.intType
        }

        private fun IrFunction.checkSyntheticAtomicExtensionParameters(): Boolean {
            if (valueParameters.size < 2) return false
            return valueParameters[0].name.asString() == DISPATCH_RECEIVER && valueParameters[0].type == irBuiltIns.anyNType &&
                    valueParameters[1].name.asString() == ATOMIC_HANDLER && atomicSymbols.isAtomicFieldUpdaterType(valueParameters[1].type)
        }

        private fun IrDeclarationContainer.getOrBuildInlineLoopFunction(
            functionName: String,
            valueType: IrType,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            val parent = this
            val mangledName = mangleAtomicExtensionName(functionName, isArrayReceiver)
            val updaterType =
                if (isArrayReceiver) atomicSymbols.getAtomicArrayType(valueType) else atomicSymbols.getFieldUpdaterType(valueType)
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangledName && it.valueParameters[0].type == updaterType
            }?.let { return it }
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = DescriptorVisibilities.PRIVATE
                origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
            }.apply {
                dispatchReceiverParameter = (parent as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
                if (functionName == LOOP) {
                    if (isArrayReceiver) generateAtomicfuArrayLoop(valueType) else generateAtomicfuLoop(valueType)
                } else {
                    if (isArrayReceiver) generateAtomicfuArrayUpdate(functionName, valueType) else generateAtomicfuUpdate(functionName, valueType)
                }
                this.parent = parent
                parent.declarations.add(this)
            }
        }

        private fun IrSimpleFunction.generateAtomicfuLoop(valueType: IrType) {
            addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
            addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuLoopBody(valueType, valueParameters)
            }
            returnType = irBuiltIns.unitType
        }

        private fun IrSimpleFunction.generateAtomicfuArrayLoop(valueType: IrType) {
            val atomicfuArrayClass = atomicSymbols.getAtomicArrayClassByValueType(valueType)
            addValueParameter(ATOMIC_HANDLER, atomicfuArrayClass.defaultType)
            addValueParameter(INDEX, irBuiltIns.intType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuArrayLoopBody(atomicfuArrayClass, valueParameters)
            }
            returnType = irBuiltIns.unitType
        }

        private fun IrSimpleFunction.generateAtomicfuUpdate(functionName: String, valueType: IrType) {
            addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, valueType))
            addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuUpdateBody(functionName, valueParameters, valueType)
            }
            returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
        }

        private fun IrSimpleFunction.generateAtomicfuArrayUpdate(functionName: String, valueType: IrType) {
            val atomicfuArrayClass = atomicSymbols.getAtomicArrayClassByValueType(valueType)
            addValueParameter(ATOMIC_HANDLER, atomicfuArrayClass.defaultType)
            addValueParameter(INDEX, irBuiltIns.intType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, valueType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuArrayUpdateBody(functionName, atomicfuArrayClass, valueParameters)
            }
            returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
        }
    }

    private fun buildTransformedAtomicExtensionDeclaration(atomicExtension: IrFunction, isArrayReceiver: Boolean): IrSimpleFunction {
        val mangledName = mangleAtomicExtensionName(atomicExtension.name.asString(), isArrayReceiver)
        val valueType = atomicExtension.extensionReceiverParameter!!.type.atomicToPrimitiveType()
        return pluginContext.irFactory.buildFun {
            name = Name.identifier(mangledName)
            isInline = true
            visibility = atomicExtension.visibility
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
        }.apply {
            extensionReceiverParameter = null
            dispatchReceiverParameter = atomicExtension.dispatchReceiverParameter?.deepCopyWithSymbols(this)
            if (isArrayReceiver) {
                addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
                addValueParameter(ATOMIC_HANDLER, atomicSymbols.getAtomicArrayClassByValueType(valueType).defaultType)
                addValueParameter(INDEX, irBuiltIns.intType)
            } else {
                addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
                addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
            }
            atomicExtension.valueParameters.forEach { addValueParameter(it.name, it.type) }
            returnType = atomicExtension.returnType
            this.parent = atomicExtension.parent
        }
    }
}
