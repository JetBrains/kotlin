/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
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
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.js.buildSimpleType


private const val ATOMICFU = "atomicfu"
private const val LOOP = "loop"
private const val UPDATE = "update"
private const val ACTION = "action\$$ATOMICFU"
private const val REF_GETTER = "refGetter\$$ATOMICFU"

class AtomicfuNativeIrTransformer(
    pluginContext: IrPluginContext,
    override val atomicSymbols: NativeAtomicSymbols
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicPropertiesTransformer: AtomicPropertiesTransformer
        get() = NativeAtomicPropertiesTransformer()

    override val atomicExtensionsTransformer: AtomicExtensionTransformer
        get() = NativeAtomicExtensionTransformer()

    override val atomicFunctionsTransformer: AtomicFunctionCallTransformer
        get() = NativeAtomicFunctionCallTransformer()

    private inner class NativeAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun IrClass.addTransformedInClassAtomic(atomicProperty: IrProperty): IrProperty =
            addVolatileProperty(atomicProperty)

        override fun IrDeclarationContainer.addTransformedStaticAtomic(atomicProperty: IrProperty): IrProperty =
            addVolatileProperty(atomicProperty)

        override fun IrDeclarationContainer.addTransformedAtomicArray(atomicProperty: IrProperty): IrProperty? {
            // todo: just skip them (as a box) and do not transform any subsequent calls on this array
            // todo: design API for atomic array intrinsics
            return null
        }

        private fun IrDeclarationContainer.addVolatileProperty(from: IrProperty): IrProperty {
            val parentContainer = this
            with(atomicSymbols.createBuilder(from.symbol)) {
                val volatileField = buildVolatileBackingField(from, parentContainer, false)
                return parentContainer.addProperty(volatileField, from.visibility, isVar = true, isStatic = from.getter!!.dispatchReceiverParameter == null).also {
                    atomicPropertyToVolatile[from] = it
                }
            }
        }
    }

    private inner class NativeAtomicExtensionTransformer : AtomicExtensionTransformer() {

        override fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrSimpleFunction && it.isAtomicExtension() }.forEach {
                declarations.add((it as IrSimpleFunction).deepCopyWithSymbols(this).transformAtomicExtension())
                // TODO: while arrays are not supported, non-transformed atomic extensions are not removed
                //declarations.remove(it)
            }
        }

        // inline fun AtomicInt.foo(arg: Int) -> foo(crossinline refGetter: () -> KMutableProperty0<Int>, arg: Int)
        private fun IrSimpleFunction.transformAtomicExtension(): IrFunction {
            val mangledName = mangleAtomicExtensionName(this.name.asString(), false)
            val valueType = extensionReceiverParameter!!.type.atomicToPrimitiveType()
            this.name = Name.identifier(mangledName)
            val refGetterLambda = buildValueParameter(this) {
                name = Name.identifier("refGetter\$atomicfu")
                index = 0
                type = atomicSymbols.kMutableProperty0GetterType(valueType)
                isCrossInline = true
            }
            valueParameters = listOf(refGetterLambda) + valueParameters
            extensionReceiverParameter = null
            return this
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
                // Transformation of the original atomic extension body should be skipped,
                // because invocations on atomic array elements are left untransformed.
                val containingFunction =
                    (if (parentFunction?.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) parentFunction.parent else parentFunction) as? IrSimpleFunction
                if (containingFunction != null && containingFunction.isAtomicExtension()) {
                    return expression
                }
                /**
                 * Atomic update call on the atomic property is replaced
                 * with the atomic intrinsic call on the corresponding volatile property reference:
                 *
                 * 1. Function call receiver is atomic property getter call.
                 *
                 * The call is replaced with atomic intrinsic call and the new receiver is
                 * the reference to the corresponding volatile property:
                 *
                 * val a = atomic(0)                    @Volatile var a$volatile = 0
                 * <get-a>().compareAndSet(0, 5)  --->  (this::a$volatile).compareAndSetField(0, 5)
                 *
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension.
                 *
                 * The call is replaced with atomic intrinsic call and the new receiver is
                 * the invoked getter of the property reference
                 * that is the first parameter of the parent function:
                 *
                 * fun AtomicInt.foo(new: Int) {          fun foo$atomicfu(crossinline refGetter: () -> KMutableProperty0<Int>, new: Int) {
                 *   this.compareAndSet(value, new)  --->   refGetter().compareAndSetField(refGetter().get(), new)
                 * }                                      }
                 */
                requireNotNull(parentFunction) { "Parent function of the call ${expression.render()} is null" }
                val getPropertyReference = buildVolatilePropertyReference(getPropertyReceiver, parentFunction)
                return irCallAtomicNativeIntrinsic(
                    functionName = functionName,
                    propertyRef = getPropertyReference,
                    valueType = valueType,
                    valueArguments = expression.valueArguments
                )
            }

        override fun transformAtomicUpdateCallOnArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            // invocations with array element receiver are left untransformed
            return expression
        }

        override fun transformAtomicExtensionCall(
            expression: IrCall,
            originalAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall =
            with(atomicSymbols.createBuilder(expression.symbol)) {
                // Transformation of the original atomic extension body should be skipped,
                // because invocations on atomic array elements are left untransformed.
                val containingFunction =
                    (if (parentFunction?.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) parentFunction.parent else parentFunction) as? IrSimpleFunction
                if (isArrayReceiver || (containingFunction != null && containingFunction.isAtomicExtension())) {
                    return expression
                }
                /**
                 * Call of the atomic extension on the atomic property is replaced
                 * with the transformed atomic extension call, passing getter of the corresponding volatile property as an argument:
                 *
                 *
                 * 1. Receiver of the atomic extension call is atomic property getter call.
                 *
                 * Atomic extension call is replaced with the transformed extension invocation and
                 * inline getter of the corresponding volatile property is passed as an argument:
                 *
                 * val a = atomic(0)                              @Volatile var a$volatile = 0
                 * fun AtomicInt.foo(new: Int) {...}              fun foo$atomicfu(crossinline refGetter: () -> KMutableProperty0<Int>, new: Int)
                 *
                 * <get-a>().foo(5)                     --->      foo$atomicfu({_ -> this::a$volatile}, 5)
                 *
                 *
                 * 2. Atomic extension is invoked in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension.
                 *
                 * This call is replaced with the transformed extension call
                 * and takes the refGetter value parameter of the parent function as an argument.
                 *
                 * fun AtomicInt.bar() {..}               fun bar$atomicfu(refGetter: () -> KMutableProperty0<Int>) { .. }
                 *
                 * fun AtomicInt.foo(new: Int) {          fun foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, new: Int) {
                 *   this.bar()                    --->     bar$atomicfu(refGetter)
                 * }                                      }
                 */
                requireNotNull(parentFunction) { "Parent function of the call ${expression.render()} is null" }
                val parent = originalAtomicExtension.parent as IrDeclarationContainer
                val transformedAtomicExtension = parent.getTransformedAtomicExtension(originalAtomicExtension, isArrayReceiver)
                val volatilePropertyGetter = buildVolatilePropertyGetter(getPropertyReceiver, parentFunction) ?: return expression // 13107 // 13215
                return irCallWithArgs(
                    symbol = transformedAtomicExtension.symbol,
                    dispatchReceiver = expression.dispatchReceiver,
                    extensionReceiver = null,
                    valueArguments = listOf(volatilePropertyGetter) + expression.valueArguments
                )
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
                // Transformation of the original atomic extension body should be skipped,
                // because invocations on atomic array elements are left untransformed.
                val containingFunction =
                    (if (parentFunction?.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) parentFunction.parent else parentFunction) as? IrSimpleFunction
                if (isArrayReceiver || (containingFunction != null && containingFunction.isAtomicExtension())) {
                    return expression
                }
                requireNotNull(parentFunction) { "Parent function of the call ${expression.render()} is null" }
                val loopFunc = parentFunction.parentDeclarationContainer.getOrBuildInlineLoopFunction(
                    functionName = functionName,
                    valueType = if (valueType.isBoolean()) irBuiltIns.intType else valueType,
                    isArrayReceiver = isArrayReceiver
                )
                val action = (expression.getValueArgument(0) as IrFunctionExpression).apply {
                    function.body?.transform(this@NativeAtomicFunctionCallTransformer, parentFunction)
                    // todo check for type in extension receiver here
                    // todo: we don't have to do this here, that's only for JVM
                    if (function.valueParameters[0].type.isBoolean()) {
                        function.valueParameters[0].type = irBuiltIns.intType
                        function.returnType = irBuiltIns.intType
                    }
                }
                val volatilePropertyGetter = buildVolatilePropertyGetter(getPropertyReceiver, parentFunction) ?: return expression
                return irCallWithArgs(
                    symbol = loopFunc.symbol,
                    dispatchReceiver = parentFunction.containingFunction.dispatchReceiverParameter?.capture(),
                    extensionReceiver = null,
                    valueArguments = listOf(volatilePropertyGetter, action)
                )
            }
        }

        override fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction = findDeclaration {
            it.name.asString() == mangleAtomicExtensionName(declaration.name.asString(), isArrayReceiver) &&
                    it.isTransformedAtomicExtension()
        }
            ?: error("Could not find corresponding transformed declaration for the atomic extension ${declaration.render()} ${if (isArrayReceiver) "for array element receiver" else ""}")

        override fun IrValueParameter.remapValueParameter(transformedExtension: IrFunction): IrValueParameter? {
            if (index < 0 && !type.isAtomicValueType()) {
                // index == -1 for `this` parameter
                return transformedExtension.dispatchReceiverParameter
                    ?: error { "Dispatch receiver of ${transformedExtension.render()} is null" }
            }
            if (index >= 0) {
                return transformedExtension.valueParameters[index]
            }
            return null
        }

        private fun IrDeclarationContainer.getOrBuildInlineLoopFunction(
            functionName: String,
            valueType: IrType,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            val parent = this
            val mangledName = mangleAtomicExtensionName(functionName, isArrayReceiver)
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangledName
                // todo put back all the checks
//                        it.valueParameters.isNotEmpty() &&
//                        it.valueParameters[0].type == irBuiltIns.functionN(0) &&
//                        (it.valueParameters[0].type as IrSimpleType).arguments.firstOrNull() == irBuiltIns.kMutableProperty0Class
            }?.let { return it }
            println(valueType)
            /**
             * inline fun update$atomicfu(refGetter: () -> KMutableProperty0<Int>, action: (Int) -> Int) {
             *   while (true) {
             *     val cur = refGetter().get()
             *     val upd = action(cur)
             *     if (refGetter().compareAndSetField(cur, upd)) return
             *   }
             * }
             */
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                with(atomicSymbols.createBuilder(symbol)) {
                    dispatchReceiverParameter = (parent as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this@apply)
                    addTypeParameter("T", irBuiltIns.anyNType)
                    val type = buildSimpleType(typeParameters[0].symbol, emptyList())
                    addValueParameter(REF_GETTER, atomicSymbols.kMutableProperty0GetterType(type))
                    if (functionName == LOOP) {
                        addValueParameter(ACTION, atomicSymbols.function1Type(type, irBuiltIns.unitType))
                        body = atomicfuLoopBody(valueParameters[0], valueParameters[1])
                        returnType = irBuiltIns.unitType
                    } else {
                        addValueParameter(ACTION, atomicSymbols.function1Type(type, type))
                        body = atomicfuUpdateBody(functionName, type, valueParameters[0], valueParameters[1])
                        returnType = if (functionName == UPDATE) irBuiltIns.unitType else type
                    }
                }
                this.parent = parent
                parent.declarations.add(this)
            }
        }

        private fun IrFunction.isAtomicExtension(): Boolean =
            extensionReceiverParameter != null && isInline && extensionReceiverParameter!!.type.isAtomicValueType()

        override fun IrFunction.isTransformedAtomicExtension(): Boolean =
            extensionReceiverParameter == null && valueParameters.isNotEmpty() && valueParameters[0].name.asString() == "refGetter\$atomicfu"

        private fun NativeAtomicfuIrBuilder.buildVolatilePropertyReference(
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction
        ): IrExpression = when {
            getPropertyReceiver is IrCall -> {
                /**
                 * Function call receiver is atomic property getter call.
                 * The new receiver is the reference to the corresponding volatile property:
                 *
                 * <get-a>().compareAndSet(0, 5)  --->  (this::a$volatile).compareAndSetField(0, 5)
                 */
                val atomicProperty = getPropertyReceiver.getCorrespondingProperty()
                val volatileProperty = atomicPropertyToVolatile[atomicProperty]
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
                require(parentFunction.isTransformedAtomicExtension())
                val refGetter = parentFunction.valueParameters[0].capture()
                irCall(atomicSymbols.invoke0Symbol).apply { dispatchReceiver = refGetter }
            }
            else -> error("Unsupported type of atomic receiver expression: ${getPropertyReceiver.render()}")
        }

        private fun NativeAtomicfuIrBuilder.buildVolatilePropertyGetter(
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction
        ): IrExpression? {
            when {
                getPropertyReceiver is IrCall -> {
                    /**
                     * Receiver of the atomic extension call is atomic property getter call.
                     * Generate inline getter lambda of the corresponding volatile property
                     * to pass as an argument to the transformed extension call:
                     *
                     * <get-a>().foo(5)  --->  foo$atomicfu({_ -> this::a$volatile}, 5)
                     */
                    val isArrayReceiver = getPropertyReceiver.isArrayElementGetter()
                    // leave invocations on array elements untransformed for now
                    if (isArrayReceiver) return null
                    val atomicProperty = getPropertyReceiver.getCorrespondingProperty()
                    val volatileProperty = atomicPropertyToVolatile[atomicProperty]
                        ?: error("No volatile property was generated for the atomic property ${atomicProperty.render()}")
                    val valueType = volatileProperty.backingField!!.type
                    return IrFunctionExpressionImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        type = atomicSymbols.kMutableProperty0GetterType(valueType),
                        function = irBuiltIns.irFactory.buildFun {
                            name = Name.identifier("<${volatileProperty.name.asString()}-getter>")
                            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
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
                    return parentFunction.valueParameters[0].capture()
                }
                else -> error("Unsupported type of atomic receiver expression: ${getPropertyReceiver.render()}")
            }
        }

        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            return if (this is IrCall) this.isArrayElementGetter() else false
        }
    }
}
