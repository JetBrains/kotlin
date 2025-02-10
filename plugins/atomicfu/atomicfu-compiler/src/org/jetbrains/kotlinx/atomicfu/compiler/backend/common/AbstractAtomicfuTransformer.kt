/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.diagnostic.AtomicfuErrorMessages.CONSTRAINTS_MESSAGE

abstract class AbstractAtomicfuTransformer(
    val pluginContext: IrPluginContext,
) {
    companion object {
        internal const val VOLATILE = "\$volatile"
        internal const val ATOMICFU = "atomicfu"
        internal const val AFU_PKG = "kotlinx.atomicfu"
        internal const val TRACE_BASE_TYPE = "TraceBase"
        internal const val ATOMIC_VALUE_FACTORY = "atomic"
        internal const val INVOKE = "invoke"
        internal const val APPEND = "append"
        internal const val GET = "get"
        internal const val LOOP = "loop"
        internal const val ACTION = "action\$$ATOMICFU"
        internal const val INDEX = "index\$$ATOMICFU"
        internal const val UPDATE = "update"
        internal const val OBJ = "obj\$$ATOMICFU"
        internal const val ATOMIC_HANDLER = "handler\$$ATOMICFU"

        private val ATOMICFU_LOOP_FUNCTIONS = setOf("loop", "update", "getAndUpdate", "updateAndGet")
        private val ATOMIC_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")
        private val ATOMIC_ARRAY_TYPES = setOf("AtomicIntArray", "AtomicLongArray", "AtomicBooleanArray", "AtomicArray")
    }

    abstract val atomicfuSymbols: AbstractAtomicSymbols
    protected val irBuiltIns = pluginContext.irBuiltIns

    // Maps atomicfu atomic property to the corresponding volatile property, used by the delegated properties transformer.
    protected val atomicfuPropertyToVolatile = mutableMapOf<IrProperty, IrProperty>()

    // Maps atomicfu property to the atomic handler (field updater/atomic array).
    protected val atomicfuPropertyToAtomicHandler = mutableMapOf<IrProperty, AtomicHandler<*>>()

    abstract val atomicfuExtensionsTransformer: AtomicExtensionTransformer
    abstract val atomicfuPropertyTransformer: AtomicPropertiesTransformer
    abstract val atomicfuFunctionCallTransformer: AtomicFunctionCallTransformer

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicProperties(moduleFragment)
        transformAtomicExtensions(moduleFragment)
        transformAtomicFunctions(moduleFragment)
        remapValueParameters(moduleFragment)
        finalTransformationCheck(moduleFragment)
        for (irFile in moduleFragment.files) {
            irFile.patchDeclarationParents()
        }
    }

    private fun transformAtomicProperties(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicfuPropertyTransformer, null)
        }
    }

    private fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicfuExtensionsTransformer, null)
        }
    }

    private fun transformAtomicFunctions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicfuFunctionCallTransformer, null)
        }
    }

    private fun remapValueParameters(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(RemapValueParameters(), null)
        }
    }

    private fun finalTransformationCheck(moduleFragment: IrModuleFragment) {
        val finalTransformationChecker = FinalTransformationChecker()
        for (irFile in moduleFragment.files) {
            irFile.accept(finalTransformationChecker, null)
        }
    }

    abstract inner class AtomicPropertiesTransformer : IrTransformer<IrFunction?>() {

        override fun visitClass(declaration: IrClass, data: IrFunction?): IrStatement {
            val declarationsToBeRemoved = mutableListOf<IrDeclaration>()
            declaration.declarations.withIndex().filter { it.value.isAtomicfuTypeProperty() }.forEach {
                transformAtomicProperty(it.value as IrProperty, it.index, declarationsToBeRemoved)
            }
            declaration.declarations.removeAll(declarationsToBeRemoved)
            return super.visitClass(declaration, data)
        }

        override fun visitFile(declaration: IrFile, data: IrFunction?): IrFile {
            val declarationsToBeRemoved = mutableListOf<IrDeclaration>()
            declaration.declarations.withIndex().filter { it.value.isAtomicfuTypeProperty() }.forEach {
                transformAtomicProperty(it.value as IrProperty, it.index, declarationsToBeRemoved)
            }
            declaration.declarations.removeAll(declarationsToBeRemoved)
            return super.visitFile(declaration, data)
        }

        private fun transformAtomicProperty(atomicfuProperty: IrProperty, index: Int, declarationsToBeRemoved: MutableList<IrDeclaration>) {
            val parentContainer = atomicfuProperty.parents.firstIsInstance<IrDeclarationContainer>()
            val atomicHandler = createAtomicHandler(atomicfuProperty, parentContainer)?.also {
                registerAtomicHandler(atomicfuProperty, it, index, parentContainer)
                declarationsToBeRemoved.add(atomicfuProperty)
            }
            if (atomicHandler == null) {
                if (atomicfuProperty.isDelegatedToAtomic()) {
                    parentContainer.transformDelegatedAtomic(atomicfuProperty)
                }
                if (atomicfuProperty.isTrace()) {
                    declarationsToBeRemoved.add(atomicfuProperty)
                }
            }
        }

        /**
         * This function creates an [AtomicHandler] for a given atomicfu property.
         * [AtomicHandler] is only created for:
         * - Atomics: properties of type kotlinx.atomicfu.Atomic(Int|Long|Ref|Boolean)
         * - AtomicArrays: properties of type kotlinx.atomicfu.Atomic(Int|Long|*)Array
         * Otherwise, this function returns null.
         */
        abstract fun createAtomicHandler(atomicfuProperty: IrProperty, parentContainer: IrDeclarationContainer): AtomicHandler<IrProperty>?

        /**
         * Creates an [AtomicArray] to replace an atomicfu array:
         * On JVM: builds a Java atomic array: java.util.concurrent.atomic.Atomic(Integer|Long|Reference)Array.
         *
         * On Native: builds a Kotlin Native array: kotlin.concurrent.Atomic(Int|Long|*)Array.
         *
         * Generated only for JVM and Native.
         */
        protected fun createAtomicArray(atomicfuProperty: IrProperty, parentContainer: IrDeclarationContainer): AtomicArray {
            with(atomicfuSymbols.createBuilder(atomicfuProperty.symbol)) {
                val atomicArrayField = irAtomicArrayField(atomicfuProperty, parentContainer)
                val atomicArrayProperty = buildPropertyWithAccessors(
                    atomicArrayField,
                    atomicfuProperty.visibility,
                    isVar = false,
                    isStatic = parentContainer is IrFile,
                    parentContainer
                )
                return AtomicArray(atomicArrayProperty)
            }
        }

        protected fun createVolatileProperty(atomicfuProperty: IrProperty, parentContainer: IrDeclarationContainer): VolatilePropertyReference {
            with(atomicfuSymbols.createBuilder(atomicfuProperty.symbol)) {
                val volatileField = buildVolatileField(atomicfuProperty, parentContainer)
                val volatileProperty = buildPropertyWithAccessors(
                    volatileField,
                    atomicfuProperty.visibility,
                    isVar = true,
                    isStatic = false,
                    parentContainer
                )
                return VolatilePropertyReference(volatileProperty)
            }
        }

        private fun registerAtomicHandler(
            atomicfuProperty: IrProperty,
            atomicHandler: AtomicHandler<IrProperty>,
            index: Int,
            parentContainer: IrDeclarationContainer
        ) {
            when (atomicHandler) {
                is AtomicFieldUpdater -> {
                    // register the volatile property corresponding to the given AtomicFieldUpdater
                    registerAtomicHandler(atomicfuProperty, atomicHandler.volatileProperty, index, parentContainer)
                    parentContainer.declarations.add(atomicHandler.declaration)
                    atomicfuPropertyToAtomicHandler[atomicfuProperty] = atomicHandler
                }
                is BoxedAtomic, is AtomicArray -> {
                    parentContainer.replacePropertyAtIndex(index, atomicHandler.declaration)
                    atomicfuPropertyToAtomicHandler[atomicfuProperty] = atomicHandler
                }
                is VolatilePropertyReference -> {
                    parentContainer.replacePropertyAtIndex(index, atomicHandler.declaration)
                    atomicfuPropertyToVolatile[atomicfuProperty] = atomicHandler.declaration
                    atomicfuPropertyToAtomicHandler[atomicfuProperty] = atomicHandler
                }
                else -> error("Trying to register the atomic handler of an unexpected type: $atomicHandler")
            }
        }

        /**
         * Transforms the given property delegated to the atomic property:
         * delegates accessors to the volatile property that was generated instead of the atomic property.
         *
         * NOTE: Delegation to atomic factory and mutable delegation properties will soon be deprecated: https://github.com/Kotlin/kotlinx-atomicfu/issues/463
         */
        private fun IrDeclarationContainer.transformDelegatedAtomic(atomicProperty: IrProperty) {
            val parentContainer = this
            val getDelegate = atomicProperty.backingField?.initializer?.expression
            require(getDelegate is IrCall) {
                "Unexpected initializer of the delegated property ${atomicProperty.atomicfuRender()}: " +
                        "expected invocation of the delegate atomic property getter, but found ${getDelegate?.render()}." + CONSTRAINTS_MESSAGE
            }
            when {
                /**
                 * 1. A property is delegated to atomic factory:
                 *
                 * var a by atomic(0) ------> @Volatile var a$volatile: Int = 0
                 */
                getDelegate.isAtomicFactoryCall() -> {
                    val delegateVolatileField = with(atomicfuSymbols.createBuilder(atomicProperty.symbol)) {
                        buildVolatileField(atomicProperty, parentContainer).also {
                            declarations.add(it)
                        }
                    }
                    atomicProperty.getter?.delegateToVolatileAccessors(delegateVolatileField)
                    atomicProperty.setter?.delegateToVolatileAccessors(delegateVolatileField)
                }
                getDelegate.symbol.owner.isGetter -> {
                    /**
                     * 2. Property delegated to another atomic property:
                     * it's accessors should get/set the value of the delegate (that is already transformed to the atomically updated volatile property).
                     *
                     * private val _a = atomic(0)       @Volatile var _a$volatile = 0
                     * var a by _a                 -->  @Volatile var a = 0
                     *                                    get() = _a$volatile
                     *                                    set(value: Int) { _a$volatile = value }
                     */
                    val delegate = getDelegate.getCorrespondingProperty()
                    check(delegate.parent == atomicProperty.parent) {
                        "The delegated property [${atomicProperty.atomicfuRender()}] declared in [${atomicProperty.parent.render()}] should be declared in the same scope " +
                                "as the corresponding atomic property [${delegate.render()}] declared in [${delegate.parent.render()}]" + CONSTRAINTS_MESSAGE
                    }
                    atomicProperty.delegateToTransformedProperty(delegate)
                }
                else -> error("Unexpected initializer of the delegated property ${getDelegate.render()}" + CONSTRAINTS_MESSAGE)
            }
            atomicProperty.backingField = null
        }

        abstract fun IrProperty.delegateToTransformedProperty(originalDelegate: IrProperty)

        protected fun IrProperty.delegateToVolatilePropertyAccessors(volatileProperty: IrProperty) {
            // If a property is delegated to an in-class atomic property ->
            // delegate to the accessors of the corresponding volatile property.
            val volatileBackingField = volatileProperty.backingField
                ?: error("Volatile property ${volatileProperty.atomicfuRender()} should have a non-null backingField")
            getter?.delegateToVolatileAccessors(volatileBackingField)
            setter?.delegateToVolatileAccessors(volatileBackingField)
        }

        private fun IrSimpleFunction.delegateToVolatileAccessors(delegateVolatileField: IrField) {
            val accessor = this
            val dispatchReceiver = dispatchReceiverParameter?.capture()
            with(atomicfuSymbols.createBuilder(symbol)) {
                body = irBlockBody {
                    +irReturn(
                        if (accessor.isGetter) {
                            // val res: Boolean = b ----> val res: Boolean = _b$volatile.toBoolean()
                            val getField = irGetField(dispatchReceiver, delegateVolatileField)
                            if (accessor.returnType.isBoolean() && delegateVolatileField.type.isInt()) toBoolean(getField) else getField
                        } else {
                            // b = false --> _b$volatile = 0
                            val arg = accessor.valueParameters.first().capture()
                            irSetField(dispatchReceiver, delegateVolatileField, if (accessor.valueParameters.first().type.isBoolean() && delegateVolatileField.type.isInt()) toInt(arg) else arg)
                        }
                    )
                }
            }
        }

        private fun IrDeclarationContainer.replacePropertyAtIndex(index: Int, newProperty: IrProperty) {
            declarations[index] = newProperty
        }

        internal fun IrProperty.isNotDelegatedAtomic(): Boolean =
            !isDelegated && backingField?.type?.isAtomicType() ?: false

        internal fun IrProperty.isAtomicArray(): Boolean =
            backingField?.type?.isAtomicArrayType() ?: false

        private fun IrCall.isAtomicFactoryCall(): Boolean =
            symbol.owner.isFromKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                    type.isAtomicType()

        private fun IrDeclaration.isAtomicfuTypeProperty(): Boolean =
            this is IrProperty && backingField?.type?.classFqName?.parent()?.asString() == AFU_PKG

        private fun IrProperty.isDelegatedToAtomic(): Boolean = isDelegated && backingField?.type?.isAtomicType() ?: false

        private fun IrProperty.isTrace(): Boolean = backingField?.type?.isTraceBaseType() ?: false
    }

    abstract inner class AtomicExtensionTransformer : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.transformAllAtomicExtensions()
            return super.visitFile(declaration)
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformAllAtomicExtensions()
            return super.visitClass(declaration)
        }

        /**
         * Replace an atomic extension with functions, which can be called on all the atomic handlers.
         * See [addAtomicHandlerValueParameters] for the new value parameter order.
         * Original atomic extension:
         * ```
         * fun AtomicInt.foo(arg: Int)
         * ```
         * The following functions will be generated:
         * on JVM:
         * ```
         * fun foo$atomicfu$AtomicFieldUpdater(atomicHandler: j.u.c.a.AtomicIntegerFieldUpdater, obj: Any?, arg: Int)
         * fun foo$atomicfu$BoxedAtomic(atomicHandler: j.u.c.a.AtomicInteger, arg: Int)
         * fun foo$atomicfu$AtomicArray(atomicHandler: j.u.c.a.AtomicIntegerArray, index: Int, arg: Int)
         * ```
         * On Native:
         * ```
         * fun foo$atomicfu$PropRef(atomicHandler: () -> KMutableProperty<Int>, arg: Int)
         * fun foo$atomicfu$AtomicArray(atomicHandler: kotlin.concurrent.AtomicIntArray, index: Int, arg: Int)
         * ```
         */
        private fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.addAll(transformedExtensionsForAllAtomicHandlers(atomicExtension))
                // the original atomic extension is removed
                declarations.remove(atomicExtension)
            }
        }

        abstract fun transformedExtensionsForAllAtomicHandlers(atomicExtension: IrFunction): List<IrSimpleFunction>

        protected fun generateExtensionForAtomicHandler(
            atomicHandlerType: AtomicHandlerType,
            atomicExtension: IrFunction
        ): IrSimpleFunction =
            generateAtomicExtensionSignatureForAtomicHandler(atomicHandlerType, atomicExtension).apply {
                body = atomicExtension.body?.deepCopyWithSymbols(this)
                body?.transform(
                    object : IrElementTransformerVoid() {
                        override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
                            if (expression.returnTargetSymbol == atomicExtension.symbol) {
                                with(atomicfuSymbols.createBuilder(this@apply.symbol)) {
                                    irReturn(expression.value)
                                }
                            } else {
                                expression
                            }
                        )
                    }, null
                )
                // all usages of the old type parameters should be remapped to the new type parameters.
                val typeRemapper = IrTypeParameterRemapper(atomicExtension.typeParameters.associateWith { this.typeParameters[it.index] })
                remapTypes(typeRemapper)
            }
    }

    abstract inner class AtomicFunctionCallTransformer : IrTransformer<IrFunction?>() {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            val receiver = (expression.extensionReceiver ?: expression.dispatchReceiver) ?: return super.visitCall(expression, data)
            val propertyGetterCall = if (receiver is IrTypeOperatorCallImpl) receiver.argument else receiver // <get-_a>()
            if (!propertyGetterCall.type.isAtomicType()) return super.visitCall(expression, data)
            val valueType = if (receiver is IrTypeOperatorCallImpl) {
                // val a = atomic<Any?>(null)
                // (a as AtomicReference<Array<String>?>).getAndSet(arrayOf("aaa", "bbb"))
                (receiver.type as IrSimpleType).arguments[0] as IrSimpleType
            } else {
                atomicfuSymbols.atomicToPrimitiveType(propertyGetterCall.type as IrSimpleType)
            }
            val dispatchReceiver =
                if (propertyGetterCall is IrCall && propertyGetterCall.isArrayElementGetter())
                    (propertyGetterCall.dispatchReceiver as? IrCall)?.dispatchReceiver
                else (propertyGetterCall as? IrCall)?.dispatchReceiver
            val atomicHandler = getAtomicHandler(propertyGetterCall, data)
            val atomicHandlerExtraArg = atomicHandler.getAtomicHandlerExtraArg(dispatchReceiver, propertyGetterCall, data)
            val builder = atomicfuSymbols.createBuilder(expression.symbol)
            if (expression.symbol.owner.isFromKotlinxAtomicfuPackage()) {
                val functionName = expression.symbol.owner.name.asString()
                if (functionName in ATOMICFU_LOOP_FUNCTIONS) {
                    requireNotNull(data) { "Expected containing function of the call ${expression.render()}, but found null." }
                    val loopCall = builder.transformAtomicfuInlineLoopCall(
                        atomicHandler = atomicHandler,
                        dispatchReceiver = dispatchReceiver,
                        valueType = valueType,
                        atomicHandlerExtraArg = atomicHandlerExtraArg,
                        action = (expression.getValueArgument(0) as IrFunctionExpression),
                        functionName = functionName,
                        parentFunction = data
                    )
                    return super.visitCall(loopCall, data)
                }
                val atomicCall = builder.transformAtomicFunctionCall(
                    atomicHandler = atomicHandler,
                    dispatchReceiver = dispatchReceiver,
                    valueType = valueType,
                    atomicHandlerExtraArg = atomicHandlerExtraArg,
                    callValueArguments = List(expression.valueArgumentsCount) { expression.getValueArgument(it) },
                    functionName = functionName
                )
                return super.visitExpression(atomicCall, data)
            }
            if (expression.symbol.owner.isInline && expression.extensionReceiver != null) {
                requireNotNull(data) { "Expected containing function of the call ${expression.render()}, but found null." }
                val declaration = expression.symbol.owner
                val irCall = builder.transformAtomicExtensionCall(
                    atomicHandler = atomicHandler,
                    dispatchReceiver = dispatchReceiver,
                    callDispatchReceiver = expression.dispatchReceiver,
                    atomicHandlerExtraArg = atomicHandlerExtraArg,
                    callValueArguments = List(expression.valueArgumentsCount) { expression.getValueArgument(it) },
                    callTypeArguments = expression.typeArguments,
                    originalAtomicExtension = declaration,
                    parentFunction = data
                )
                return super.visitCall(irCall, data)
            }
            return super.visitCall(expression, data)
        }

        private fun AbstractAtomicfuIrBuilder.transformAtomicFunctionCall(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            valueType: IrType,
            atomicHandlerExtraArg: IrExpression?,
            callValueArguments: List<IrExpression?>,
            functionName: String
        ): IrExpression {
            val atomicHandlerCallReceiver = getAtomicHandlerCallReceiver(atomicHandler, dispatchReceiver)
            val irCall = invokeFunctionOnAtomicHandler(
                atomicHandler.type,
                atomicHandlerCallReceiver,
                functionName,
                buildList { atomicHandlerExtraArg?.let { add(it) }; addAll(callValueArguments) },
                valueType
            )
            return if (functionName == "<get-value>") irAs(irCall, valueType) else irCall
        }

        private fun AbstractAtomicfuIrBuilder.transformAtomicfuInlineLoopCall(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            valueType: IrType,
            atomicHandlerExtraArg: IrExpression?,
            action: IrFunctionExpression,
            functionName: String,
            parentFunction: IrFunction
        ): IrCall {
            val loopFunc = getOrBuildAtomicfuLoop(
                atomicHandler = atomicHandler,
                functionName = functionName,
                valueType = valueType,
                parentFunction = parentFunction
            )
            val transformedAction = action.apply {
                function.body?.transform(this@AtomicFunctionCallTransformer, parentFunction)
            }.deepCopyWithSymbols(parentFunction)
            val atomicHandlerReceiverValueParam = getAtomicHandlerValueParameterReceiver(atomicHandler, dispatchReceiver, parentFunction)
            return irCallFunction(
                symbol = loopFunc.symbol,
                dispatchReceiver = parentFunction.firstNonLocalFunctionForLambdaParent.dispatchReceiverParameter?.capture(),
                extensionReceiver = null,
                valueArguments = buildList { add(atomicHandlerReceiverValueParam); atomicHandlerExtraArg?.let { add(it) }; add(transformedAction) },
                valueType = valueType
            )
        }

        private fun AbstractAtomicfuIrBuilder.transformAtomicExtensionCall(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            callDispatchReceiver: IrExpression?,
            atomicHandlerExtraArg: IrExpression?,
            callValueArguments: List<IrExpression?>,
            callTypeArguments: List<IrType?>,
            originalAtomicExtension: IrSimpleFunction,
            parentFunction: IrFunction
        ): IrCall {
            val parent = originalAtomicExtension.parent as IrDeclarationContainer
            val transformedAtomicExtension = parent.getOrBuildTransformedAtomicExtension(originalAtomicExtension, atomicHandler.type)
            val atomicHandlerReceiverValueParam = getAtomicHandlerValueParameterReceiver(atomicHandler, dispatchReceiver, parentFunction)
            return irCall(transformedAtomicExtension.symbol).apply {
                this.dispatchReceiver = callDispatchReceiver
                this.extensionReceiver = null
                var shift = 0
                putValueArgument(shift++, atomicHandlerReceiverValueParam)
                atomicHandlerExtraArg?.let { putValueArgument(shift++, it) }
                callValueArguments.forEachIndexed { i, arg -> putValueArgument(i + shift, arg); }
                callTypeArguments.forEachIndexed { i, irType ->
                    typeArguments[i] = irType
                }
            }
        }

        private fun AbstractAtomicfuIrBuilder.getOrBuildAtomicfuLoop(
            atomicHandler: AtomicHandler<*>,
            functionName: String,
            valueType: IrType,
            parentFunction: IrFunction
        ): IrSimpleFunction {
            val parentContainer = parentFunction.parentDeclarationContainer
            val mangledName = mangleAtomicExtension(functionName, atomicHandler.type, valueType)
            parentContainer.findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangledName &&
                        it.checkAtomicHandlerValueParameters(atomicHandler.type, valueType) &&
                        it.checkActionParameter() &&
                        (it.returnType == irBuiltIns.unitType || it.returnType == valueType)
            }?.let { return it }
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = DescriptorVisibilities.PRIVATE
                origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
            }.apply {
                val T = if (!valueType.isPrimitiveType()) irBuiltIns.anyNType else valueType
                val actionReturnType = if (functionName == LOOP) irBuiltIns.unitType else T
                dispatchReceiverParameter = (parentContainer as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
                addAtomicHandlerValueParameters(atomicHandler.type, T)
                addValueParameter(ACTION, atomicfuSymbols.function1Type(T, actionReturnType))
                with(atomicfuSymbols.createBuilder(symbol)) {
                    body = if (functionName == LOOP) {
                        generateLoopBody(atomicHandler.type, T, valueParameters)
                    } else {
                        generateUpdateBody(atomicHandler.type, valueType, valueParameters, functionName)
                    }
                }
                returnType = if (functionName == LOOP || functionName == UPDATE) irBuiltIns.unitType else T
                this.parent = parentContainer
                parentContainer.declarations.add(this)
            }
        }

        private fun IrDeclarationContainer.getOrBuildTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            atomicHandlerType: AtomicHandlerType
        ): IrSimpleFunction {
            val valueType = atomicfuSymbols.atomicToPrimitiveType(declaration.extensionReceiverParameter!!.type as IrSimpleType)
            // Try find the transformed atomic extension in the parent container
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangleAtomicExtension(declaration.name.asString(), atomicHandlerType, valueType)
            }?.let { return it }
            /**
             * NOTE: this comment is applicable to the JVM backend incremental compilation:
             * If the transformed declaration is not found then the call may be performed from another module
             * which depends on the module where declarations are generated from untransformed metadata (real transformed declarations are not there).
             * This happens if the call is performed from the test module or in case of incremental compilation.
             *
             * We build a fake declaration here: it's signature equals the one of the real transformed declaration,
             * it doesn't have body and won't be generated. It is placed in the call site and
             * during compilation this fake declaration will be resolved to the real transformed declaration.
             */
            return generateAtomicExtensionSignatureForAtomicHandler(atomicHandlerType, declaration)
        }

        private fun getAtomicHandler(atomicCallReceiver: IrExpression, parentFunction: IrFunction?): AtomicHandler<*> =
            when {
                atomicCallReceiver is IrCall -> {
                    val isArrayReceiver = atomicCallReceiver.isArrayElementGetter()
                    val getAtomicProperty = if (isArrayReceiver) atomicCallReceiver.dispatchReceiver as IrCall else atomicCallReceiver
                    val atomicProperty = getAtomicProperty.getCorrespondingProperty()
                    atomicfuPropertyToAtomicHandler[atomicProperty]
                        ?: error("No atomic handler found for the atomic property ${atomicProperty.atomicfuRender()}, \n" +
                                         "these properties were registered: ${
                                             buildString {
                                                 atomicfuPropertyToAtomicHandler.forEach {
                                                     appendLine("[ property: ${it.key.render()}, atomicHandler: ${it.value.declaration.render()}]")
                                                 }
                                             }
                                         }" + CONSTRAINTS_MESSAGE)
                }
                atomicCallReceiver.isThisReceiver() -> {
                    requireNotNull(parentFunction) { "Expected containing function of the call with receiver ${atomicCallReceiver.render()}, but found null." + CONSTRAINTS_MESSAGE }
                    require(parentFunction.isTransformedAtomicExtension())
                    valueParameterToAtomicHandler(parentFunction.valueParameters[0])
                }
                else -> error("Unexpected type of atomic function call receiver: ${atomicCallReceiver.render()}, parentFunction = ${parentFunction?.render()}." + CONSTRAINTS_MESSAGE)
            }

        abstract fun valueParameterToAtomicHandler(valueParameter: IrValueParameter): AtomicHandler<*>

        abstract fun AbstractAtomicfuIrBuilder.getAtomicHandlerCallReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression

        abstract fun AbstractAtomicfuIrBuilder.getAtomicHandlerValueParameterReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            parentFunction: IrFunction
        ): IrExpression

        abstract fun AtomicHandler<*>.getAtomicHandlerExtraArg(
            dispatchReceiver: IrExpression?,
            propertyGetterCall: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression?

        protected fun AtomicArray.getAtomicArrayElementIndex(propertyGetterCall: IrExpression): IrExpression =
            requireNotNull((propertyGetterCall as IrCall).getValueArgument(0)) {
                "Expected index argument to be passed to the atomic array getter call ${propertyGetterCall.render()}, but found null." + CONSTRAINTS_MESSAGE
            }

        protected fun AtomicArrayValueParameter.getAtomicArrayElementIndex(parentFunction: IrFunction?): IrExpression {
            require(parentFunction != null && parentFunction.valueParameters.size > 1)
            val index = parentFunction.valueParameters[1]
            require(index.name.asString() == INDEX && index.type == irBuiltIns.intType)
            return index.capture()
        }

        override fun visitBlockBody(body: IrBlockBody, data: IrFunction?): IrBody {
            // Erase messages added by the Trace object from the function body:
            // val trace = Trace(size)
            // Messages may be added via trace invocation:
            // trace { "Doing something" }
            // or via multi-append of arguments:
            // trace.append(index, "CAS", value)
            body.statements.removeIf {
                it.isTraceCall()
            }
            return super.visitBlockBody(body, data)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: IrFunction?): IrExpression {
            // Erase messages added by the Trace object from blocks.
            expression.statements.removeIf {
                it.isTraceCall()
            }
            return super.visitContainerExpression(expression, data)
        }

        private fun IrExpression.isThisReceiver() =
            this is IrGetValue && symbol.owner.let {
                it is IrValueParameter &&
                    (it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver)
            }

        private fun IrCall.isArrayElementGetter(): Boolean =
            dispatchReceiver?.let {
                it.type.isAtomicArrayType() && symbol.owner.name.asString() == GET
            } ?: false

        private fun IrStatement.isTraceCall() = this is IrCall && (isTraceInvoke() || isTraceAppend())

        private fun IrCall.isTraceInvoke(): Boolean =
            symbol.owner.isFromKotlinxAtomicfuPackage() &&
                    symbol.owner.name.asString() == INVOKE &&
                    symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

        private fun IrCall.isTraceAppend(): Boolean =
            symbol.owner.isFromKotlinxAtomicfuPackage() &&
                    symbol.owner.name.asString() == APPEND &&
                    symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

        private val IrFunction.firstNonLocalFunctionForLambdaParent: IrFunction
            get() {
                if (this.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return this
                return parents.filterIsInstance<IrFunction>().firstOrNull {
                    it.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                }
                    ?: error("In the sequence of parents for the local function ${this.render()} no containing function was found" + CONSTRAINTS_MESSAGE)
            }
    }

    /**
     * At the first stage [AtomicExtensionTransformer] replaced atomic extensions with the new functions,
     * which have new generated value parameters in their signatures, e.g.:
     * instead of `AtomicInt.foo(arg: Int)`, `foo$atomicfu(atomicArray: AtomicIntArray, index: Int, arg: Int)` was generated.
     * the bodies for the transformed functions were copied from the original function.
     *
     * At the next stage [AtomicFunctionCallTransformer] transformed the bodies of these functions:
     * e.g. { this.compareAndSet(value, arg) } -> { atomicArray.compareAndSet(index, value, arg) }
     *
     * Note, that `arg` value parameter still has an original function as it's parent,
     * so the parent should be changed for the new transformed declaration.
     * This is done by [RemapValueParameters] transformer.
     *
     * It's launched as a separate transformation stage to avoid recursive visiting.
     */
    private inner class RemapValueParameters : IrTransformer<IrFunction?>() {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction?): IrExpression {
            if (expression.symbol is IrValueParameterSymbol) {
                val valueParameter = expression.symbol.owner as IrValueParameter
                val parent = valueParameter.parent
                if (parent is IrFunctionImpl && parent.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return expression
                // If the parent of the given value parameter was an atomic extension, then this function does not exist anymore,
                // and the value parameter should be remapped to the corresponding transformed extension.
                if (parent is IrFunction && parent.isAtomicExtension()) {
                    require(data != null) { "Tried to remap a value parameter ${valueParameter.render()} to the transformed extension, but the current function data is null." }
                    return valueParameter.remapValueParameters(data.firstTransformedFunctionParent)?.capture() ?: super.visitGetValue(expression, data)
                }
            }
            return super.visitGetValue(expression, data)
        }

        val IrFunction.firstTransformedFunctionParent: IrFunction
            get() =
                if (this.isTransformedAtomicExtension()) this
                else
                    parents.toList().filterIsInstance<IrFunction>().firstOrNull { it.isTransformedAtomicExtension() }
                        ?: error("Failed to find a transformed atomic extension parent function for ${this.render()}.")

        private fun IrValueParameter.remapValueParameters(transformedExtension: IrFunction): IrValueParameter? {
            if (indexInOldValueParameters < 0 && !type.isAtomicType()) {
                // data is a transformed function
                // index == -1 for `this` parameter
                return transformedExtension.dispatchReceiverParameter
                    ?: error("Dispatch receiver of ${transformedExtension.render()} is null" + CONSTRAINTS_MESSAGE)
            }
            if (indexInOldValueParameters >= 0) {
                val shift = transformedExtension.valueParameters.map { it.name.asString() }.count { it.endsWith(ATOMICFU) }
                return transformedExtension.valueParameters[indexInOldValueParameters + shift]
            }
            return null
        }
    }

    private inner class FinalTransformationChecker : IrTransformer<IrFunction?>() {
        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            if (expression.symbol.owner.isGetter && (expression.type.isAtomicType() || expression.type.isAtomicArrayType())) {
                val atomicProperty = expression.getCorrespondingProperty()
                if ((atomicProperty.parent as IrDeclarationContainer).declarations.contains(atomicProperty)) {
                    error(
                        "Untransformed atomic property [${atomicProperty.atomicfuRender()}] is found in ${data?.render()}.\n" +
                                "Probably some constraints on usage of atomic properties were violated." + CONSTRAINTS_MESSAGE
                    )
                } else {
                    error(
                        "Function invocation is expected on the atomic property [${atomicProperty.atomicfuRender()}] in ${data?.render()}.\n" +
                                "Please invoke atomic get or update function." + CONSTRAINTS_MESSAGE
                    )
                }
            }
            return super.visitCall(expression, data)
        }
    }

    private fun IrFunction.checkActionParameter(): Boolean {
        val action = valueParameters.last()
        return action.name.asString() == ACTION &&
                action.type.classOrNull == irBuiltIns.functionN(1).symbol
    }

    private fun generateAtomicExtensionSignatureForAtomicHandler(
        atomicHandlerType: AtomicHandlerType,
        atomicExtension: IrFunction
    ): IrSimpleFunction {
        val valueType = atomicfuSymbols.atomicToPrimitiveType(atomicExtension.extensionReceiverParameter!!.type as IrSimpleType)
        val mangledName = mangleAtomicExtension(atomicExtension.name.asString(), atomicHandlerType, valueType)
        return pluginContext.irFactory.buildFun {
            name = Name.identifier(mangledName)
            isInline = true
            visibility = atomicExtension.visibility
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FUNCTION
            containerSource = atomicExtension.containerSource
        }.apply {
            dispatchReceiverParameter = atomicExtension.dispatchReceiverParameter?.deepCopyWithSymbols(this)
            atomicExtension.typeParameters.forEach { addTypeParameter(it.name.asString(), it.representativeUpperBound) }
            addAtomicHandlerValueParameters(atomicHandlerType, valueType)
            atomicExtension.valueParameters.forEach { addValueParameter(it.name, it.type) }
            returnType = atomicExtension.returnType
            this.parent = atomicExtension.parent
        }
    }

    abstract fun IrFunction.checkAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType): Boolean

    abstract fun IrFunction.addAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType)

    private fun IrFunction.isFromKotlinxAtomicfuPackage(): Boolean = parentDeclarationContainer.kotlinFqName.asString().startsWith(AFU_PKG)

    internal fun List<IrValueParameter>.holdsAt(index: Int, paramName: String, type: IrType): Boolean {
        require(index >= 0 && index < size) { "Index $index is out of bounds of the given value parameter list of size $size" }
        return get(index).name.asString() == paramName && get(index).type == type
    }

    private val IrDeclaration.parentDeclarationContainer: IrDeclarationContainer
        get() = parents.filterIsInstance<IrDeclarationContainer>().firstOrNull()
            ?: error("In the sequence of parents for ${this.render()} no IrDeclarationContainer was found" + CONSTRAINTS_MESSAGE)

    private fun IrType.isAtomicType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_TYPES
        } ?: false

    private fun IrType.isAtomicArrayType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_ARRAY_TYPES
        } ?: false

    private fun IrFunction.isAtomicExtension(): Boolean =
        if (extensionReceiverParameter != null && extensionReceiverParameter!!.type.isAtomicType()) {
            require(this.isInline) {
                "Non-inline extension functions on kotlinx.atomicfu.Atomic* classes are not allowed, " +
                        "please add inline modifier to the function ${this.render()}."
            }
            require(this.visibility == DescriptorVisibilities.PRIVATE || this.visibility == DescriptorVisibilities.INTERNAL) {
                "Only private or internal extension functions on kotlinx.atomicfu.Atomic* classes are allowed, " +
                        "please make the extension function ${this.render()} private or internal."
            }
            true
        } else false

    internal fun IrFunction.isTransformedAtomicExtension(): Boolean =
        name.asString().contains("\$$ATOMICFU") && valueParameters.isNotEmpty() && valueParameters[0].name.asString() == ATOMIC_HANDLER

    private fun IrType.isTraceBaseType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() == TRACE_BASE_TYPE
        } ?: false

    private fun IrCall.getCorrespondingProperty(): IrProperty =
        symbol.owner.correspondingPropertySymbol?.owner
            ?: error("Atomic property accessor ${this.render()} expected to have non-null correspondingPropertySymbol" + CONSTRAINTS_MESSAGE)

    private fun mangleAtomicExtension(name: String, atomicHandlerType: AtomicHandlerType, valueType: IrType) =
        name + "$" + ATOMICFU + "$" + atomicHandlerType + "$" + if (valueType.isPrimitiveType()) valueType.classFqName?.shortName() else "Any"

    internal fun IrValueParameter.capture(): IrGetValue = IrGetValueImpl(startOffset, endOffset, symbol.owner.type, symbol)
}
