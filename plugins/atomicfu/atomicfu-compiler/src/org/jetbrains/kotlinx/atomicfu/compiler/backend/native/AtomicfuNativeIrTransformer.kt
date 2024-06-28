/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer

private const val ATOMICFU = "atomicfu"
private const val REF_GETTER = "refGetter\$$ATOMICFU"
private const val ATOMIC_ARRAY = "atomicArray\$$ATOMICFU"
private const val INDEX = "index\$$ATOMICFU"

class AtomicfuNativeIrTransformer(
    pluginContext: IrPluginContext,
    override val atomicSymbols: NativeAtomicSymbols
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicPropertiesTransformer: AtomicPropertiesTransformer
        get() = NativeAtomicPropertiesTransformer()

    override val atomicFunctionsTransformer: AtomicFunctionCallTransformer
        get() = NativeAtomicFunctionCallTransformer()

    private inner class NativeAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun IrClass.addTransformedInClassAtomic(atomicProperty: IrProperty, index: Int): IrProperty =
            addVolatileProperty(atomicProperty, index)

        override fun IrDeclarationContainer.addTransformedStaticAtomic(atomicProperty: IrProperty, index: Int): IrProperty =
            addVolatileProperty(atomicProperty, index)

        private fun IrDeclarationContainer.addVolatileProperty(from: IrProperty, index: Int): IrProperty {
            /**
             * Atomic property [from] is replaced with the volatile field of the value type stored in [from]
             * which is atomically updated via atomic intrinsics (see details about intrinsics in [kotlin.concurrent.AtomicInt]).
             *
             * Volatile field has the same visibility as the original atomic property.
             *
             * internal val a = atomic(0)  --> internal @Volatile var a: Int = 0
             */
            val parentContainer = this
            with(atomicSymbols.createBuilder(from.symbol)) {
                val volatileField = buildVolatileBackingField(from, parentContainer, castBooleanFieldsToInt)
                return parentContainer.replacePropertyAtIndex(volatileField, from.visibility, isVar = true, isStatic = from.parent is IrFile, index).also {
                        atomicfuPropertyToVolatile[from] = it
                    }
            }
        }
    }

    private inner class NativeAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun transformAtomicUpdateCallOnProperty(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression =
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Atomic update call on the atomic property is replaced
                 * with the atomic intrinsic call on the corresponding volatile property reference:
                 *
                 * 1. Function call receiver is atomic property getter call.
                 *
                 * The call is replaced with atomic intrinsic call and the new receiver is
                 * the reference to the corresponding volatile property:
                 *
                 * internal val a = atomic(0)           internal @Volatile var a$volatile = 0
                 * <get-a>().compareAndSet(0, 5)  --->  (this::a$volatile).compareAndSetField(0, 5)
                 *
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension.
                 *
                 * The call is replaced with atomic intrinsic call and the new receiver is
                 * the invoked getter of the property reference that is the first parameter of the parent function:
                 *
                 * fun AtomicInt.foo(new: Int) {          fun foo$atomicfu(crossinline refGetter: () -> KMutableProperty0<Int>, new: Int) {
                 *   this.compareAndSet(value, new)  --->   refGetter().compareAndSetField(refGetter().get(), new)
                 * }                                      }
                 */
                val getPropertyReference = getVolatilePropertyReference(getPropertyReceiver, parentFunction)
                return irCallAtomicNativeIntrinsic(
                    functionName = functionName,
                    propertyRef = getPropertyReference,
                    valueType = valueType,
                    returnType = expression.type,
                    valueArguments = expression.valueArguments
                )
            }

        private fun NativeAtomicfuIrBuilder.getVolatilePropertyReference(
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression = when {
            getPropertyReceiver is IrCall -> {
                /**
                 * Function call receiver is atomic property getter call.
                 * The new receiver is the reference to the corresponding volatile property:
                 *
                 * <get-a>().compareAndSet(0, 5)  --->  (this::a$volatile).compareAndSetField(0, 5)
                 */
                val atomicProperty = getPropertyReceiver.getCorrespondingProperty()
                val volatileProperty = atomicfuPropertyToVolatile[atomicProperty]
                    ?: error("No volatile property was generated for the atomic property ${atomicProperty.render()}")
                irPropertyReference(
                    property = volatileProperty,
                    classReceiver = getPropertyReceiver.dispatchReceiver
                )
            }
            getPropertyReceiver.isThisReceiver() -> {
                /**
                 * Function call receiver is the old <this> receiver of the extension.
                 * The new receiver is the invoked getter of the property reference.
                 * that is the first parameter of the parent function:
                 *
                 * fun AtomicInt.foo(new: Int) {          fun foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, new: Int) {
                 *   this.compareAndSet(value, new)  --->   refGetter().compareAndSetField(refGetter().get(), new)
                 * }                                      }
                 */
                require(parentFunction != null && parentFunction.isTransformedAtomicExtension())
                val refGetter = parentFunction.valueParameters[0]
                require(refGetter.name.asString() == REF_GETTER && refGetter.type.classOrNull == irBuiltIns.functionN(0).symbol)
                irCall(atomicSymbols.invoke0Symbol).apply { dispatchReceiver = refGetter.capture() }
            }
            else -> error("Unsupported type of atomic receiver expression: ${getPropertyReceiver.render()}")
        }

        override fun buildSyntheticValueArgsForTransformedAtomicExtensionCall(
            expression: IrCall,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction
        ): List<IrExpression?> {
            /**
             * 1. Receiver of the atomic extension call is atomic property getter call.
             * For atomic property receiver: generate getter lambda for the property reference corresponding to the call receiver
             *
             * inline fun AtomicInt.foo(arg: Int) {..}             inline foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, arg: Int)
             * aClass._a.foo(arg)                              --> foo$atomicfu({_ -> aClass::_a}, arg)
             *
             * For array property receiver: pass atomic array and the index of the element as an argument
             *
             * inline fun AtomicInt.foo(arg: Int) {..}             inline foo$atomicfu$array(array: AtomicIntArray, index: Int, arg: Int)
             * aClass.intArr[7].foo(arg)                              --> foo$atomicfu$array(intArr, 7, arg)
             *
             * 2. Atomic extension is invoked in the body of the transformed atomic extension,
             * the call receiver is the old <this> receiver of the extension:
             * get the corresponding argument from the parent function.
             *
             * fun AtomicInt.bar() {..}               fun bar$atomicfu(refGetter: () -> KMutableProperty0<Int>) { .. }
             *
             * fun AtomicInt.foo(new: Int) {          fun foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, new: Int) {
             *   this.bar()                    --->     bar$atomicfu(refGetter)
             * }                                      }
             */
            with(atomicSymbols.createBuilder(expression.symbol)) {
                return if (isArrayReceiver) {
                    val index = getPropertyReceiver.getArrayElementIndex(parentFunction)
                    val atomicArray = getAtomicHandler(getPropertyReceiver, parentFunction)
                    listOf(atomicArray, index)
                } else {
                    listOf(buildVolatilePropertyRefGetter(getPropertyReceiver, parentFunction))
                }
            }
        }

        // This counter is used to ensure uniqueness of functions for refGetter lambdas,
        // as several functions with the same name may be created in the same scope
        private var refGetterCounter: Int = 0

        private fun NativeAtomicfuIrBuilder.buildVolatilePropertyRefGetter(
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction
        ): IrExpression {
            when {
                getPropertyReceiver is IrCall -> {
                    /**
                     * Receiver of the atomic extension call is atomic property getter call.
                     * Generate inline getter lambda of the corresponding volatile property
                     * to pass as an argument to the transformed extension call:
                     *
                     * <get-a>().foo(5)  --->  foo$atomicfu({_ -> this::a$volatile}, 5)
                     */
                    val atomicProperty = getPropertyReceiver.getCorrespondingProperty()
                    val volatileProperty = atomicfuPropertyToVolatile[atomicProperty]
                        ?: error("No volatile property was generated for the atomic property ${atomicProperty.render()}")
                    val valueType = volatileProperty.backingField!!.type
                    return IrFunctionExpressionImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        type = atomicSymbols.kMutableProperty0GetterType(valueType),
                        function = irBuiltIns.irFactory.buildFun {
                            name = Name.identifier("<${volatileProperty.name.asString()}-getter-${refGetterCounter++}>")
                            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
                            returnType = atomicSymbols.kMutableProperty0Type(valueType)
                            isInline = true
                            visibility = DescriptorVisibilities.LOCAL
                        }.apply {
                            val lambda = this
                            body = irBlockBody {
                                +irReturn(
                                    irPropertyReference(volatileProperty, getPropertyReceiver.dispatchReceiver)
                                ).apply {
                                    type = irBuiltIns.nothingType
                                    returnTargetSymbol = lambda.symbol
                                }
                            }
                            parent = parentFunction
                        },
                        origin = IrStatementOrigin.LAMBDA
                    )
                }
                getPropertyReceiver.isThisReceiver() -> {
                    /**
                     * Atomic extension is invoked in the body of the transformed atomic extension,
                     * the call receiver is the old <this> receiver of the extension.
                     * Pass the parameter of parent extension as an argument:
                     *
                     * fun AtomicInt.foo(new: Int) {          fun foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, new: Int) {
                     *   this.bar()                    --->     bar$atomicfu(refGetter)
                     * }                                      }
                     */
                    require(parentFunction.isTransformedAtomicExtension())
                    val refGetter = parentFunction.valueParameters[0]
                    require(refGetter.name.asString() == REF_GETTER && refGetter.type.classOrNull == irBuiltIns.functionN(0).symbol)
                    return refGetter.capture()
                }
                else -> error("Unsupported type of atomic receiver expression: ${getPropertyReceiver.render()}")
            }
        }

        override fun IrValueParameter.remapValueParameter(transformedExtension: IrFunction): IrValueParameter? {
            if (index < 0 && !type.isAtomicValueType()) {
                // data is a transformed function
                // index == -1 for `this` parameter
                return transformedExtension.dispatchReceiverParameter
                    ?: error("Dispatch receiver of ${transformedExtension.render()} is null" + CONSTRAINTS_MESSAGE)
            }
            if (index >= 0) {
                val shift = if (transformedExtension.name.asString().isMangledAtomicArrayExtension()) 2 else 1
                return transformedExtension.valueParameters[index + shift]
            }
            return null
        }

        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            return if (this is IrCall) this.isArrayElementGetter() else false
        }

        override fun IrFunction.checkSyntheticArrayElementExtensionParameter(): Boolean {
            if (valueParameters.size < 2) return false
            return valueParameters[0].name.asString() == ATOMIC_ARRAY && atomicSymbols.isAtomicArrayHandlerType(valueParameters[0].type) &&
                    valueParameters[1].name.asString() == INDEX && valueParameters[1].type == irBuiltIns.intType
        }

        override fun IrFunction.checkSyntheticAtomicExtensionParameters(): Boolean {
            if (valueParameters.isEmpty()) return false
            return valueParameters[0].name.asString() == REF_GETTER && valueParameters[0].type.classOrNull == irBuiltIns.functionN(0).symbol
        }

        override fun IrFunction.checkSyntheticParameterTypes(isArrayReceiver: Boolean, receiverValueType: IrType): Boolean {
            if (isArrayReceiver) {
                if (valueParameters.size < 2) return false
                val atomicArrayClassSymbol = atomicSymbols.getAtomicArrayClassByValueType(receiverValueType)
                return valueParameters[0].name.asString() == ATOMIC_ARRAY && valueParameters[0].type.classOrNull == atomicArrayClassSymbol &&
                        valueParameters[1].name.asString() == INDEX && valueParameters[1].type == irBuiltIns.intType
            } else {
                if (valueParameters.size < 1) return false
                return valueParameters[0].name.asString() == REF_GETTER && valueParameters[0].type.classOrNull == irBuiltIns.functionN(0).symbol
            }
        }
    }

    /**
     * On Native AtomicBoolean fields are transformed into Boolean volatile properties and updated via intrinsics.
     */
    override val castBooleanFieldsToInt: Boolean
        get() = false

    /**
     * Builds the signature of the transformed atomic extension:
     *
     * inline fun AtomicInt.foo(arg: Int) --> inline fun foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, arg': Int)
     *                                        inline fun foo$atomicfu$array(atomicArray: AtomicIntegerArray, index: Int, arg': Int)
     */
    override fun buildTransformedAtomicExtensionSignature(atomicExtension: IrFunction, isArrayReceiver: Boolean): IrSimpleFunction {
        val mangledName = mangleAtomicExtensionName(atomicExtension.name.asString(), isArrayReceiver)
        val atomicReceiverType = atomicExtension.extensionReceiverParameter!!.type
        val valueType = (atomicReceiverType as IrSimpleType).atomicToPrimitiveType()
        return pluginContext.irFactory.buildFun {
            name = Name.identifier(mangledName)
            isInline = true
            visibility = atomicExtension.visibility
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
        }.apply {
            extensionReceiverParameter = null
            dispatchReceiverParameter = atomicExtension.dispatchReceiverParameter?.deepCopyWithSymbols(this)
            atomicExtension.typeParameters.forEach { addTypeParameter(it.name.asString(), it.representativeUpperBound) }
            addSyntheticValueParametersToTransformedAtomicExtension(isArrayReceiver, if (valueType == irBuiltIns.anyNType) atomicReceiverType.arguments.first().typeOrNull!! else valueType)
            atomicExtension.valueParameters.forEach { addValueParameter(it.name, it.type) }
            returnType = atomicExtension.returnType
            this.parent = atomicExtension.parent
        }
    }

    /**
     * Adds synthetic value parameters to the transformed atomic extension (custom atomic extension or atomicfu inline update functions).
     */
    override fun IrFunction.addSyntheticValueParametersToTransformedAtomicExtension(isArrayReceiver: Boolean, valueType: IrType) {
        if (!isArrayReceiver) {
            addValueParameter(REF_GETTER, atomicSymbols.kMutableProperty0GetterType(valueType)).apply { isCrossinline = true }
        } else {
            addValueParameter(ATOMIC_ARRAY, atomicSymbols.getParameterizedAtomicArrayType(valueType))
            addValueParameter(INDEX, irBuiltIns.intType)
        }
    }
}
