/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer
import kotlin.collections.set

private const val ATOMICFU = "atomicfu"
private const val DISPATCH_RECEIVER = "dispatchReceiver\$$ATOMICFU"
private const val ATOMIC_HANDLER = "handler\$$ATOMICFU"
private const val INDEX = "index\$$ATOMICFU"

class AtomicfuJvmIrTransformer(
    pluginContext: IrPluginContext,
    override val atomicSymbols: JvmAtomicSymbols
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicPropertiesTransformer: AtomicPropertiesTransformer
        get() = JvmAtomicPropertiesTransformer()

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
                val volatileField = buildVolatileBackingField(from, parentClass, castBooleanFieldsToInt)
                val volatileProperty = if (volatileField.parent == from.parent) {
                    // The index is relevant only if the property belongs to the same class as the original atomic property (not the generated wrapper).
                    parentClass.replacePropertyAtIndex(volatileField, DescriptorVisibilities.PRIVATE, isVar = true, isStatic = false, index)
                } else {
                    parentClass.addProperty(volatileField, DescriptorVisibilities.PRIVATE, isVar = true, isStatic = false)
                }
                atomicfuPropertyToVolatile[from] = volatileProperty
                val atomicUpdaterField = irJavaAtomicFieldUpdater(volatileField, parentClass)
                parentClass.addProperty(atomicUpdaterField, from.getMinVisibility(), isVar = false, isStatic = true).also {
                    atomicfuPropertyToAtomicHandler[from] = it
                }
                return volatileProperty
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

        override fun buildSyntheticValueArgsForTransformedAtomicExtensionCall(
            expression: IrCall,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction
        ): List<IrExpression?> {
            val dispatchReceiver = getDispatchReceiver(getPropertyReceiver, parentFunction)
            val getAtomicHandler = getAtomicHandler(getPropertyReceiver, parentFunction)
            return if (isArrayReceiver) {
                val index = getPropertyReceiver.getArrayElementIndex(parentFunction)
                listOf(getAtomicHandler, index)
            } else {
                listOf(dispatchReceiver, getAtomicHandler)
            }
        }

        override fun IrValueParameter.remapValueParameter(transformedExtension: IrFunction): IrValueParameter? {
            if (index < 0 && !type.isAtomicValueType()) {
                // data is a transformed function
                // index == -1 for `this` parameter
                return transformedExtension.dispatchReceiverParameter
                    ?: error("During remapping of value parameters from the original atomic extension ${this.parent.render()} to the transformed one ${transformedExtension}:" +
                                     "dispatchReceiver parameter (index == -1) was not found at the transformed extension." + CONSTRAINTS_MESSAGE)
            }
            if (index >= 0) {
                val shift = 2 // 2 synthetic value parameters
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
                else -> error("Unexpected type of atomic function call receiver: ${atomicCallReceiver.render()}." + CONSTRAINTS_MESSAGE)
            }

        private fun IrCall.getDispatchReceiver(): IrExpression? {
            val isArrayReceiver = isArrayElementGetter()
            val getAtomicProperty = if (isArrayReceiver) dispatchReceiver as IrCall else this
            val atomicProperty = getAtomicProperty.getCorrespondingProperty()
            val dispatchReceiver = getAtomicProperty.dispatchReceiver
            // top-level atomics
            if (!isArrayReceiver && (dispatchReceiver == null || dispatchReceiver.type.isObject())) {
                val volatileProperty = atomicfuPropertyToVolatile[atomicProperty]!!
                return getStaticVolatileWrapperInstance(volatileProperty.parentAsClass)
            }
            return dispatchReceiver
        }

        override fun IrFunction.checkSyntheticArrayElementExtensionParameter(): Boolean {
            if (valueParameters.size < 2) return false
            return valueParameters[0].name.asString() == ATOMIC_HANDLER && atomicSymbols.isAtomicArrayHandlerType(valueParameters[0].type) &&
                    valueParameters[1].name.asString() == INDEX && valueParameters[1].type == irBuiltIns.intType
        }

        override fun IrFunction.checkSyntheticAtomicExtensionParameters(): Boolean {
            if (valueParameters.size < 2) return false
            return valueParameters[0].name.asString() == DISPATCH_RECEIVER && valueParameters[0].type == irBuiltIns.anyNType &&
                    valueParameters[1].name.asString() == ATOMIC_HANDLER && atomicSymbols.isAtomicFieldUpdaterType(valueParameters[1].type)
        }

        override fun IrFunction.checkSyntheticParameterTypes(isArrayReceiver: Boolean, receiverValueType: IrType): Boolean {
            if (isArrayReceiver) {
                if (valueParameters.size < 2) return false
                val atomicArrayType = atomicSymbols.getAtomicArrayClassByValueType(receiverValueType).defaultType
                return valueParameters[0].name.asString() == ATOMIC_HANDLER && valueParameters[0].type == atomicArrayType &&
                        valueParameters[1].name.asString() == INDEX && valueParameters[1].type == irBuiltIns.intType
            } else {
                if (valueParameters.size < 2) return false
                val atomicUpdaterType = atomicSymbols.getFieldUpdaterType(receiverValueType)
                return valueParameters[0].name.asString() == DISPATCH_RECEIVER && valueParameters[0].type == irBuiltIns.anyNType &&
                        valueParameters[1].name.asString() == ATOMIC_HANDLER && valueParameters[1].type == atomicUpdaterType
            }
        }
    }

    /**
     * On JVM all Boolean fields are cast to Int, as they only can be updated via AtomicIntegerFieldUpdaters
     */
    override val castBooleanFieldsToInt: Boolean
        get() = true

    /**
     * Builds the signature of the transformed atomic extension:
     *
     * inline fun AtomicInt.foo(arg: Int) --> inline fun foo$atomicfu(dispatchReceiver: Any?, atomicHandler: AtomicIntegerFieldUpdater, arg': Int)
     *                                        inline fun foo$atomicfu$array(atomicArray: AtomicIntegerArray, index: Int, arg': Int)
     */
    override fun buildTransformedAtomicExtensionSignature(atomicExtension: IrFunction, isArrayReceiver: Boolean): IrSimpleFunction {
        val mangledName = mangleAtomicExtensionName(atomicExtension.name.asString(), isArrayReceiver)
        val valueType = (atomicExtension.extensionReceiverParameter!!.type as IrSimpleType).atomicToPrimitiveType()
        return pluginContext.irFactory.buildFun {
            name = Name.identifier(mangledName)
            isInline = true
            visibility = atomicExtension.visibility
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
        }.apply {
            extensionReceiverParameter = null
            dispatchReceiverParameter = atomicExtension.dispatchReceiverParameter?.deepCopyWithSymbols(this)
            atomicExtension.typeParameters.forEach { addTypeParameter(it.name.asString(), it.representativeUpperBound) }
            addSyntheticValueParametersToTransformedAtomicExtension(isArrayReceiver, valueType)
            atomicExtension.valueParameters.forEach { addValueParameter(it.name, it.type) }
            returnType = atomicExtension.returnType
            this.parent = atomicExtension.parent
        }
    }

    /**
     * Adds synthetic value parameters to the transformed atomic extension (custom atomic extension or atomicfu inline update functions).
     */
    override fun IrFunction.addSyntheticValueParametersToTransformedAtomicExtension(isArrayReceiver: Boolean, valueType: IrType) {
        if (isArrayReceiver) {
            addValueParameter(ATOMIC_HANDLER, atomicSymbols.getAtomicArrayClassByValueType(valueType).defaultType)
            addValueParameter(INDEX, irBuiltIns.intType)
        } else {
            addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
            addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
        }
    }
}
