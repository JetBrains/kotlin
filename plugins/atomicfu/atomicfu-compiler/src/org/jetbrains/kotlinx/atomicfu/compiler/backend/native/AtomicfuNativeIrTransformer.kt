/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer

class AtomicfuNativeIrTransformer(
    override val atomicfuSymbols: NativeAtomicSymbols,
    pluginContext: IrPluginContext
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicfuPropertyTransformer: AtomicPropertiesTransformer = NativeAtomicPropertiesTransformer()
    override val atomicfuFunctionCallTransformer: AtomicFunctionCallTransformer = NativeAtomicFunctionCallTransformer()
    override val atomicfuExtensionsTransformer: AtomicExtensionTransformer = NativeAtomicExtensionsTransformer()

    private inner class NativeAtomicExtensionsTransformer : AtomicExtensionTransformer() {

        override fun transformedExtensionsForAllAtomicHandlers(atomicExtension: IrFunction): List<IrSimpleFunction> = listOf(
            generateExtensionForAtomicHandler(AtomicHandlerType.NATIVE_PROPERTY_REF, atomicExtension),
            generateExtensionForAtomicHandler(AtomicHandlerType.ATOMIC_ARRAY, atomicExtension)
        )
    }

    private inner class NativeAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun createAtomicHandler(
            atomicfuProperty: IrProperty,
            parentContainer: IrDeclarationContainer
        ): AtomicHandler<IrProperty>? =
            when {
                atomicfuProperty.isNotDelegatedAtomic() -> {
                    /**
                     * Creates an [VolatilePropertyReference] updater to replace an atomicfu property on Native:
                     * on Native all atomic operations on atomicfu properties are delegated to atomic intrinsics
                     * invoked on the volatile property reference (declared in kotlin.concurrent package), e.g.:
                     * ```
                     * private val a = atomic(0)
                     * a.compareAndSet(0, 56)
                     * ```
                     * is replaced with:
                     * ```
                     * @Volatile var a: Int = 0
                     * ::a.compareAndSetField
                     *```
                     */
                    createVolatileProperty(atomicfuProperty, parentContainer)
                }
                atomicfuProperty.isAtomicArray() -> {
                    createAtomicArray(atomicfuProperty, parentContainer)
                }
                else -> null
            }

        override fun IrProperty.delegateToTransformedProperty(originalDelegate: IrProperty) {
            val volatileProperty = atomicfuPropertyToVolatile[originalDelegate]
            requireNotNull(volatileProperty) { "The property ${originalDelegate.atomicfuRender()} is expected to be already replaced with a corresponding volatile property, but none was found." }
            delegateToVolatilePropertyAccessors(volatileProperty)
        }
    }

    private inner class NativeAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun AtomicHandler<*>.getAtomicHandlerExtraArg(
            dispatchReceiver: IrExpression?,
            propertyGetterCall: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression? = when(this) {
            is AtomicArray -> getAtomicArrayElementIndex(propertyGetterCall)
            is AtomicArrayValueParameter -> getAtomicArrayElementIndex(parentFunction)
            else -> null
        }

        private fun AbstractAtomicfuIrBuilder.getAtomicHandlerReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression = when(atomicHandler) {
            is AtomicArray -> irGetProperty((atomicHandler.declaration), dispatchReceiver)
            is AtomicArrayValueParameter -> (atomicHandler.declaration).capture()
            is VolatilePropertyReference -> irPropertyReference(atomicHandler.declaration, dispatchReceiver)
            is VolatilePropertyReferenceGetterValueParameter -> atomicHandler.declaration.capture() // { () -> ::a }
            else -> error("Unexpected atomic handler type for Native backend: ${atomicHandler.javaClass.simpleName}")
        }

        override fun AbstractAtomicfuIrBuilder.getAtomicHandlerCallReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression {
            val atomicHandlerReceiver = getAtomicHandlerReceiver(atomicHandler, dispatchReceiver)
            return if (atomicHandler is VolatilePropertyReferenceGetterValueParameter) {
                invokePropertyGetter(atomicHandlerReceiver)
            } else {
                atomicHandlerReceiver
            }
        }

        override fun AbstractAtomicfuIrBuilder.getAtomicHandlerValueParameterReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            parentFunction: IrFunction
        ): IrExpression {
            val atomicHandlerReceiver = getAtomicHandlerReceiver(atomicHandler, dispatchReceiver)
            return if (atomicHandler is VolatilePropertyReference) {
                // Atomic intrinsics can only be invoked on only compile-time known IrProperties ->
                // to pass a property reference to a function, generate a getter, which returns this property reference:
                // { ::a }
                irVolatilePropertyRefGetter(atomicHandlerReceiver, atomicHandler.declaration.name.asString(), parentFunction)
            } else atomicHandlerReceiver
        }

        override fun valueParameterToAtomicHandler(valueParameter: IrValueParameter): AtomicHandler<*> =
            when {
                atomicfuSymbols.isAtomicArrayHandlerType(valueParameter.type) -> AtomicArrayValueParameter(valueParameter)
                atomicfuSymbols.isVolatilePropertyReferenceGetter(valueParameter.type) -> VolatilePropertyReferenceGetterValueParameter(
                    valueParameter
                )
                else -> error("The type of the given valueParameter=${valueParameter.render()} does not match any of the Native AtomicHandler types.")
            }

    }

    override fun IrFunction.checkAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType): Boolean =
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_ARRAY -> {
                val arrayClassSymbol = atomicfuSymbols.getAtomicArrayClassByValueType(valueType)
                val type = if (arrayClassSymbol.owner.typeParameters.isNotEmpty()) {
                    arrayClassSymbol.typeWith(valueType)
                } else {
                    arrayClassSymbol.defaultType
                }
                valueParameters.size > 2 &&
                        valueParameters.holdsAt(0, ATOMIC_HANDLER, type) &&
                        valueParameters.holdsAt(1, INDEX, irBuiltIns.intType)
            }
            AtomicHandlerType.NATIVE_PROPERTY_REF -> {
                valueParameters.size > 1 &&
                        valueParameters.holdsAt(0, ATOMIC_HANDLER, atomicfuSymbols.kMutableProperty0GetterType(valueType))
            }
            else -> error("Unexpected atomic handler type for Native backend: $atomicHandlerType")
        }


    override fun IrFunction.addAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType) {
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_ARRAY -> {
                val arrayClassSymbol = atomicfuSymbols.getAtomicArrayClassByValueType(valueType)
                val type = if (arrayClassSymbol.owner.typeParameters.isNotEmpty()) {
                    arrayClassSymbol.typeWith(valueType)
                } else {
                    arrayClassSymbol.defaultType
                }
                addValueParameter(ATOMIC_HANDLER, type)
                addValueParameter(INDEX, irBuiltIns.intType)
            }
            AtomicHandlerType.NATIVE_PROPERTY_REF -> {
                addValueParameter(ATOMIC_HANDLER, atomicfuSymbols.kMutableProperty0GetterType(valueType)).apply { isCrossinline = true }
            }
            else -> error("Unexpected atomic handler type for Native backend: $atomicHandlerType")
        }
    }
}

