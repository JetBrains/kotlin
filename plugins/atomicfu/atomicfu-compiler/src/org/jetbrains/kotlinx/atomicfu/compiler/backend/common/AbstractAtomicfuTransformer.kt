/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

private const val ATOMICFU = "atomicfu"
private const val ARRAY = "array"
private const val AFU_PKG = "kotlinx.atomicfu"
private const val TRACE_BASE_TYPE = "TraceBase"
private const val ATOMIC_VALUE_FACTORY = "atomic"
private const val INVOKE = "invoke"
private const val APPEND = "append"
private const val GET = "get"
private const val VOLATILE = "\$volatile"
private const val VOLATILE_WRAPPER_SUFFIX = "\$VolatileWrapper\$$ATOMICFU"

abstract class AbstractAtomicfuTransformer(val pluginContext: IrPluginContext) {

    companion object {
        const val CONSTRAINTS_MESSAGE =
            "\n\nPlease make sure that you follow these constraints for using atomic properties:\n" +
                    "   * To ensure that atomic properties are not accessed out of the current Kotlin module, it is necessary to declare atomic properties as private or internal,\n" +
                    "     or make the containing class private or internal.\n" +
                    "     To expose the atomic property value to the public, use a delegated property declared in the same scope, e.g: \n" +
                    "       ```\n" +
                    "       private val _a = atomic<T>(initial) \n" +
                    "       public var a: T by _a \n" +
                    "       ```\n" +
                    "   * Only perform operations directly on atomic values:\n" +
                    "       * Avoid storing references to atomic values in local variables, e.g.\n" +
                    "           ```\n" +
                    "           val top = atomic<Node?>(null)\n" +
                    "           top.compareAndSet(null, Node(1)) // OK: direct invocation on the atomic property is allowed \n" +
                    "           ```\n" +
                    "           ```\n" +
                    "           val tmp = top\n" +
                    "           tmp.compareAndSet(null, Node(1)) // DON'T DO THIS: invocation on a local variable is NOT allowed  \n" +
                    "           ```\n" +
                    "       * Do not leak references to atomic values in other way (return, pass as params, etc).\n" +
                    "   * Avoid introducing complex data flow within parameters of atomic operations:\n" +
                    "       Instead of top.compareAndSet(cur, <complex_expression>) use \n" +
                    "       ```\n" +
                    "       val newValue = <complex_expression>\n" +
                    "       top.compareAndSet(cur, newValue) \n" +
                    "       ```\n" +
                    "\n"
    }

    abstract val atomicSymbols: AbstractAtomicSymbols
    protected val irBuiltIns = pluginContext.irBuiltIns

    private val ATOMICFU_INLINE_FUNCTIONS = setOf("loop", "update", "getAndUpdate", "updateAndGet")
    private val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")
    private val ATOMIC_ARRAY_TYPES = setOf("AtomicIntArray", "AtomicLongArray", "AtomicBooleanArray", "AtomicArray")

    protected val atomicPropertyToVolatile = mutableMapOf<IrProperty, IrProperty>()
    protected val propertyToAtomicHandler = mutableMapOf<IrProperty, IrProperty>()

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicProperties(moduleFragment)
        transformAtomicExtensions(moduleFragment)
        transformAtomicFunctions(moduleFragment)
        finalTransformationCheck(moduleFragment)
        for (irFile in moduleFragment.files) {
            irFile.patchDeclarationParents()
        }
    }

    protected abstract val atomicPropertiesTransformer: AtomicPropertiesTransformer
    protected abstract val atomicExtensionsTransformer: AtomicExtensionTransformer
    protected abstract val atomicFunctionsTransformer: AtomicFunctionCallTransformer

    private fun transformAtomicProperties(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicPropertiesTransformer, null)
        }
    }

    private fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicExtensionsTransformer, null)
        }
    }

    private fun transformAtomicFunctions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicFunctionsTransformer, null)
        }
    }

    private fun finalTransformationCheck(moduleFragment: IrModuleFragment) {
        val finalTransformationChecker = FinalTransformationChecker()
        for (irFile in moduleFragment.files) {
            irFile.accept(finalTransformationChecker, null)
        }
    }

    protected abstract inner class AtomicPropertiesTransformer : IrElementTransformer<IrFunction?> {

        override fun visitClass(declaration: IrClass, data: IrFunction?): IrStatement {
            val declarationsToBeRemoved = mutableListOf<IrDeclaration>()
            declaration.declarations.withIndex().filter { isPropertyOfAtomicfuType(it.value) }.forEach {
                transformAtomicProperty(it.value as IrProperty, it.index, declarationsToBeRemoved)
            }
            declaration.declarations.removeAll(declarationsToBeRemoved)
            return super.visitClass(declaration, data)
        }

        override fun visitFile(declaration: IrFile, data: IrFunction?): IrFile {
            val declarationsToBeRemoved = mutableListOf<IrDeclaration>()
            declaration.declarations.withIndex().filter { isPropertyOfAtomicfuType(it.value) }.forEach {
                transformAtomicProperty(it.value as IrProperty, it.index, declarationsToBeRemoved)
            }
            declaration.declarations.removeAll(declarationsToBeRemoved)
            return super.visitFile(declaration, data)
        }

        private fun transformAtomicProperty(atomicProperty: IrProperty, index: Int, declarationsToBeRemoved: MutableList<IrDeclaration>) {
            val parentContainer = atomicProperty.parents.firstIsInstance<IrDeclarationContainer>()
            val isTopLevel = parentContainer is IrFile || (parentContainer is IrClass && parentContainer.kind == ClassKind.OBJECT)
            when {
                atomicProperty.isAtomic() -> {
                    if (isTopLevel) {
                        atomicProperty.checkVisibility()
                        parentContainer.addTransformedStaticAtomic(atomicProperty, index)
                    } else {
                        atomicProperty.checkVisibility()
                        (parentContainer as IrClass).addTransformedInClassAtomic(atomicProperty, index)
                    }.also {
                        declarationsToBeRemoved.add(atomicProperty)
                    }
                }
                atomicProperty.isAtomicArray() -> {
                    atomicProperty.checkVisibility()
                    parentContainer.addTransformedAtomicArray(atomicProperty, index)?.also {
                        declarationsToBeRemoved.add(atomicProperty)
                    }
                }
                atomicProperty.isDelegatedToAtomic() -> parentContainer.transformDelegatedAtomic(atomicProperty)
                atomicProperty.isTrace() -> declarationsToBeRemoved.add(atomicProperty)
                else -> {}
            }
        }

        /**
         * Generates a volatile property that can be atomically updated instead of the given atomic property
         * and adds it to the parent class.
         * Returns the volatile property.
         */
        abstract fun IrClass.addTransformedInClassAtomic(atomicProperty: IrProperty, index: Int): IrProperty

        /**
         * Generates a volatile property that can be atomically updated instead of the given static atomic property
         * and adds it to the parent container.
         * Returns the voaltile property.
         */
        abstract fun IrDeclarationContainer.addTransformedStaticAtomic(atomicProperty: IrProperty, index: Int): IrProperty

        /**
         * Generates an array that can be atomically updated instead of the given atomic array
         * and adds it to the parent class.
         * Returns the new property or null if transformation was skipped.
         * NOTE: skipping transformation is supported for K/N backend, because arrays are currently not supported there.
         */
        abstract fun IrDeclarationContainer.addTransformedAtomicArray(atomicProperty: IrProperty, index: Int): IrProperty?

        /**
         * Transforms the given property that was delegated to the atomic property:
         * delegates accessors to the volatile property that was generated instead of the atomic property.
         */
        private fun IrDeclarationContainer.transformDelegatedAtomic(atomicProperty: IrProperty) {
            val getDelegate = atomicProperty.backingField?.initializer?.expression
            require(getDelegate is IrCall) { "Expected initializer of the delegated property ${this.render()} is IrCall but found ${getDelegate?.render()}" }
            val delegateVolatileField = when {
                getDelegate.isAtomicFactoryCall() -> {
                    /**
                     * 1. Property delegated to atomic factory invocation is transformed to the volatile property:
                     *
                     * var a by atomic(0)  --> @Volatile var a = 0
                     *                           get() = a
                     *                           set(value: Int) { a = value }
                     */
                    with(atomicSymbols.createBuilder(atomicProperty.symbol)) {
                        buildVolatileBackingField(atomicProperty, this@transformDelegatedAtomic, false).also {
                            declarations.add(it)
                        }
                    }
                }
                getDelegate.symbol.owner.isGetter -> {
                    /**
                     * 2. Property delegated to another atomic property:
                     * it's accessors should get/set the value of the delegate (that is already transformed to the atomically updated volatile property).
                     *
                     * val _a = atomic(0)       @Volatile _a = 0 (+ atomic updaters)
                     * var a by _a         -->  @Volatile var a = 0
                     *                           get() = _a
                     *                           set(value: Int) { _a = value }
                     */
                    val delegate = getDelegate.getCorrespondingProperty()
                    check(delegate.parent == atomicProperty.parent) {
                        "The delegated property [${atomicProperty.render()}] declared in [${atomicProperty.parent.render()}] should be declared in the same scope " +
                                "as the corresponding atomic property [${delegate.render()}] declared in [${delegate.parent.render()}]." + CONSTRAINTS_MESSAGE}
                    val volatileProperty = atomicPropertyToVolatile[delegate] ?: error("The delegate property was not transformed: ${delegate.render()}.")
                    volatileProperty.backingField ?: error("Transformed atomic field should have a non-null backingField.")
                }
                else -> error("Unexpected initializer of the delegated property ${this.render()}")
            }
            atomicProperty.getter?.transformAccessor(delegateVolatileField)
            atomicProperty.setter?.transformAccessor(delegateVolatileField)
            atomicProperty.backingField = null
        }

        private fun IrSimpleFunction.transformAccessor(delegateVolatileField: IrField) {
            val dispatchReceiver =
                if (delegateVolatileField.parent is IrClass && delegateVolatileField.parentAsClass.name.asString().contains(VOLATILE_WRAPPER_SUFFIX)) {
                    getStaticVolatileWrapperInstance(delegateVolatileField.parentAsClass)
                } else {
                    dispatchReceiverParameter?.capture()
                }
            with(atomicSymbols.createBuilder(symbol)) {
                body = irExprBody(
                    if (this@transformAccessor.isGetter) {
                        irGetField(dispatchReceiver, delegateVolatileField)
                    } else {
                        irSetField(dispatchReceiver, delegateVolatileField, this@transformAccessor.valueParameters[0].capture())
                    }
                )
            }
        }

        /**
         * Generates a private volatile field initialized with the initial value of the given atomic property:
         * private val a = atomic(0)  --> private @Volatile a: Int = 0
         */
        protected fun AbstractAtomicfuIrBuilder.buildVolatileBackingField(
            atomicProperty: IrProperty,
            parentContainer: IrDeclarationContainer,
            tweakBooleanToInt: Boolean
        ): IrField {
            val atomicField = requireNotNull(atomicProperty.backingField) { "BackingField of atomic property $atomicProperty should not be null." }
            val fieldType = atomicField.type.atomicToPrimitiveType()
            val initializer = atomicField.initializer?.expression
            if (initializer == null) {
                val initBlock = atomicField.getInitBlockForField(parentContainer)
                val initExprWithIndex = initBlock.getInitExprWithIndexFromInitBlock(atomicField.symbol)
                    ?: error("Expected property ${atomicProperty.render()} initialization in init block ${initBlock.render()}.")
                val atomicFactoryCall = initExprWithIndex.value.value
                val initExprIndex = initExprWithIndex.index
                val initValue = atomicFactoryCall.getAtomicFactoryValueArgument()
                return irVolatileField(
                    atomicProperty.name.asString() + VOLATILE,
                    // JVM: AtomicBoolean is transformed to a volatile Int field (boolean fields can only be updated with AtomicIntegerFieldUpdater)
                    // K/N: AtomicBoolean should be a volatile Boolean field
                    if (tweakBooleanToInt && fieldType.isBoolean()) irBuiltIns.intType else fieldType,
                    null,
                    atomicField.annotations,
                    parentContainer
                ).also {
                    initBlock.updateFieldInitialization(atomicField.symbol, it.symbol, initValue, initExprIndex)
                }
            } else {
                val initValue = initializer.getAtomicFactoryValueArgument()
                return irVolatileField(
                    atomicProperty.name.asString() + VOLATILE,
                    // JVM: AtomicBoolean is transformed to a volatile Int field (boolean fields can only be updated with AtomicIntegerFieldUpdater)
                    // K/N: AtomicBoolean should be a volatile Boolean field
                    if (tweakBooleanToInt && fieldType.isBoolean()) irBuiltIns.intType else fieldType,
                    initValue,
                    atomicField.annotations,
                    parentContainer
                )
            }
        }

        /**
         * In case if atomic property is initialized in init block it's declaration is replaced with the volatile property
         * and initialization of the backing field is also performed in the init block:
         *
         * private val _a: AtomicInt   --> @Volatile var _a: Int
         *
         * init {                          init {
         *   _a = atomic(0)                  _a = 0
         * }                               }
         */
        protected fun IrAnonymousInitializer.getInitExprWithIndexFromInitBlock(
            oldFieldSymbol: IrFieldSymbol
        ): IndexedValue<IrSetField>? =
            body.statements.withIndex().singleOrNull { it.value is IrSetField && (it.value as IrSetField).symbol == oldFieldSymbol }?.let {
                @Suppress("UNCHECKED_CAST")
                it as IndexedValue<IrSetField>
            }

        protected fun IrAnonymousInitializer.updateFieldInitialization(
            oldFieldSymbol: IrFieldSymbol,
            volatileFieldSymbol: IrFieldSymbol,
            initExpr: IrExpression,
            index: Int
        ) {
            // save the order of field initialization in init block
            body.statements.singleOrNull {
                it is IrSetField && it.symbol == oldFieldSymbol
            }?.let {
                it as IrSetField
                with(atomicSymbols.createBuilder(it.symbol)) {
                    body.statements[index] = irSetField(it.receiver, volatileFieldSymbol.owner, initExpr)
                }
            }
        }

        protected fun IrField.getInitBlockForField(parentContainer: IrDeclarationContainer): IrAnonymousInitializer {
            for (declaration in parentContainer.declarations) {
                if (declaration is IrAnonymousInitializer) {
                    if (declaration.body.statements.any { it is IrSetField && it.symbol == this.symbol }) {
                        return declaration
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

        // atomic(value = 0) -> 0
        private fun IrExpression.getAtomicFactoryValueArgument(): IrExpression {
            require(this is IrCall) { "Expected atomic factory invocation but found: ${this.render()}." }
            return getValueArgument(0)?.deepCopyWithSymbols()
                ?: error("Atomic factory should take at least one argument: ${this.render()}.")
        }

        // AtomicIntArray(size = 10) -> 10
        protected fun IrExpression.getArraySizeArgument(): IrExpression {
            require(this is IrFunctionAccessExpression) {
                "Expected atomic array factory invocation, but found: ${this.render()}."
            }
            return getValueArgument(0)?.deepCopyWithSymbols()
                ?: error("Atomic array factory should take at least one argument: ${this.render()}.")
        }

        private fun IrProperty.checkVisibility() =
            check((visibility == DescriptorVisibilities.PRIVATE || visibility == DescriptorVisibilities.INTERNAL) ||
                        (parent is IrClass &&
                                (parentAsClass.visibility == DescriptorVisibilities.PRIVATE || parentAsClass.visibility == DescriptorVisibilities.INTERNAL))) {
                "To ensure that atomic properties are not accessed out of the current Kotlin module, it is necessary to declare atomic properties as private or internal.\n" +
                "Please consider declaring [${this.atomicfuRender()}] from [${this.parent.render()}] as a private or internal property." +
                if (parent is IrClass) ",\nYou may also make the containing class [${parentAsClass.render()}] private or internal.\n" else "\n" +
                "Alternatively, if you need to expose the atomic property value to the public, you can use a delegated property declared within the same scope, e.g:\n" +
                "```\n" +
                "private val _a = atomic<T>(initial) \n" +
                "public var a: T by _a \n" +
                "```\n"
            }

        protected fun IrProperty.getMinVisibility(): DescriptorVisibility {
            // To protect atomic properties from leaking out of the current sourceSet, they are required to be internal or private,
            // or the containing class may be internal or private.
            // This method returns the minimal visibility between the property visibility and the class visibility applied to atomic updaters or volatile wrappers.
            val classVisibility = if (this.parent is IrClass) parentAsClass.visibility else DescriptorVisibilities.PUBLIC
            val compare = visibility.compareTo(classVisibility) ?: -1 // in case of non-comparable visibilities (e.g. local and private) return property visibility
            return if (compare > 0) classVisibility else visibility
        }
    }

    protected abstract inner class AtomicExtensionTransformer : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.transformAllAtomicExtensions()
            return super.visitFile(declaration)
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformAllAtomicExtensions()
            return super.visitClass(declaration)
        }

        abstract fun IrDeclarationContainer.transformAllAtomicExtensions()
    }

    protected abstract inner class AtomicFunctionCallTransformer : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            (expression.extensionReceiver ?: expression.dispatchReceiver)?.transform(this, data)?.let {
                val propertyGetterCall = if (it is IrTypeOperatorCallImpl) it.argument else it // <get-_a>()
                if (propertyGetterCall.type.isAtomicValueType()) {
                    val valueType = if (it is IrTypeOperatorCallImpl) {
                        // If receiverExpression is a cast `s as AtomicRef<String>`
                        // then valueType is the type argument of Atomic* class `String`
                        (it.type as IrSimpleType).arguments[0] as IrSimpleType
                    } else {
                        propertyGetterCall.type.atomicToPrimitiveType()
                    }
                    val isArrayReceiver = propertyGetterCall.isArrayElementReceiver(data)
                    if (expression.symbol.owner.isFromKotlinxAtomicfuPackage()) {
                        /**
                         * Transform invocations of functions from kotlinx.atomicfu on atomics properties or atomic array elements:
                         *
                         * <get-_a>().compareAndSet(10, 45)
                         * <get-intArr>()[1].getAndSet(10)
                         * <get-_a>().updateAndGet { cur -> cur + 100 }
                         */
                        val functionName = expression.symbol.owner.name.asString()
                        if (functionName in ATOMICFU_INLINE_FUNCTIONS) {
                            val loopCall = transformedAtomicfuInlineFunctionCall(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                getPropertyReceiver = propertyGetterCall,
                                isArrayReceiver = isArrayReceiver,
                                parentFunction = data
                            )
                            return super.visitCall(loopCall, data)
                        }
                        val irCall = if (isArrayReceiver) {
                            transformAtomicUpdateCallOnArrayElement(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                getPropertyReceiver = propertyGetterCall,
                                parentFunction = data
                            )
                        } else {
                            transformAtomicUpdateCallOnProperty(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                castType = if (it is IrTypeOperatorCall) valueType else null,
                                getPropertyReceiver = propertyGetterCall,
                                parentFunction = data
                            )
                        }
                        return super.visitExpression(irCall, data)
                    }
                    if (expression.symbol.owner.isInline && expression.extensionReceiver != null) {
                        /**
                         * Transform invocation of Atomic* extension functions, delegating them to the corresponding transformed atomic extensions:
                         *
                         * val _a = atomic(0)
                         * inline fun AtomicInt.foo() { ... }
                         * _a.foo()
                         */
                        val declaration = expression.symbol.owner
                        val irCall = transformAtomicExtensionCall(
                            expression = expression,
                            originalAtomicExtension = declaration,
                            getPropertyReceiver = propertyGetterCall,
                            isArrayReceiver = isArrayReceiver,
                            parentFunction = data
                        )
                        return super.visitCall(irCall, data)
                    }
                }
            }
            return super.visitCall(expression, data)
        }

        abstract fun transformAtomicUpdateCallOnProperty(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression

        abstract fun transformAtomicUpdateCallOnArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression

        abstract fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall

        abstract fun transformAtomicExtensionCall(
            expression: IrCall,
            originalAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall

        abstract fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction

        override fun visitGetValue(expression: IrGetValue, data: IrFunction?): IrExpression {
            /**
             * During transformation of atomic extensions value parameters are changed, though the body is just copied from the original declaration.
             * This function replaces capturing of old value parameters with new parameters in the body of a transformed atomic extension.
             *
             * JVM example:
             *
             * inline fun AtomicInt.foo(to: Int) {   --> inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, to': Int) {
             *   compareAndSet(0, to)                      handler.compareAndSet(0, to) // there is no parameter `to` in the new signature,
             *                                                                          // it should be replaced with `to'`
             * }                                         }
             */
            if (expression.symbol is IrValueParameterSymbol) {
                val valueParameter = expression.symbol.owner as IrValueParameter
                val parent = valueParameter.parent
                // skip value parameters of lambdas
                if (parent is IrFunctionImpl && parent.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return expression
                if (data != null && data.isTransformedAtomicExtension() &&
                    parent is IrFunctionImpl && !parent.isTransformedAtomicExtension()) {
                    return valueParameter.remapValueParameter(data)?.capture() ?: super.visitGetValue(expression, data)
                }
            }
            return super.visitGetValue(expression, data)
        }

        abstract fun IrValueParameter.remapValueParameter(transformedExtension: IrFunction): IrValueParameter?

        abstract fun IrFunction.isTransformedAtomicExtension(): Boolean

        abstract fun IrExpression.isArrayElementReceiver(
            parentFunction: IrFunction?
        ): Boolean

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
    }

    private inner class FinalTransformationChecker: IrElementTransformer<IrFunction?> {
        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            if (expression.symbol.owner.isGetter && (expression.type.isAtomicValueType() || expression.type.isAtomicArrayType())) {
                val atomicProperty = expression.getCorrespondingProperty()
                if ((atomicProperty.parent as IrDeclarationContainer).declarations.contains(atomicProperty)) {
                    error("Untransformed atomic property [${atomicProperty.atomicfuRender()}] is found in ${data?.render()}.\n" +
                                "Probably some constraints on usage of atomic properties were violated." + CONSTRAINTS_MESSAGE)
                } else {
                    error("Function invocation is expected on the atomic property [${atomicProperty.atomicfuRender()}] in ${data?.render()}.\n" +
                                "Please invoke atomic get or update function." + CONSTRAINTS_MESSAGE)
                }
            }
            return super.visitCall(expression, data)
        }
    }

    // Util transformer functions

    protected fun getStaticVolatileWrapperInstance(volatileWrapperClass: IrClass): IrExpression {
        val volatileWrapperClassInstance = volatileWrapperClass.parentDeclarationContainer.declarations.find {
            it is IrProperty && it.backingField?.type?.classOrNull == volatileWrapperClass.symbol
        } ?: error("Instance of ${volatileWrapperClass.name.asString()} was not found in the parent class ${volatileWrapperClass.parentDeclarationContainer.render()}")
        return with(atomicSymbols.createBuilder(volatileWrapperClass.symbol)) {
            irGetProperty(volatileWrapperClassInstance as IrProperty, null)
        }
    }

    private fun IrFunction.isFromKotlinxAtomicfuPackage(): Boolean = parentDeclarationContainer.kotlinFqName.asString().startsWith(AFU_PKG)

    private fun isPropertyOfAtomicfuType(declaration: IrDeclaration): Boolean =
        declaration is IrProperty && declaration.backingField?.type?.classFqName?.parent()?.asString() == AFU_PKG

    private fun IrProperty.isAtomic(): Boolean =
        !isDelegated && backingField?.type?.isAtomicValueType() ?: false

    private fun IrProperty.isDelegatedToAtomic(): Boolean =
        isDelegated && backingField?.type?.isAtomicValueType() ?: false

    private fun IrProperty.isAtomicArray(): Boolean =
        backingField?.type?.isAtomicArrayType() ?: false

    private fun IrProperty.isTrace(): Boolean =
        backingField?.type?.isTraceBaseType() ?: false

    protected fun IrType.isAtomicValueType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_VALUE_TYPES
        } ?: false

    private fun IrType.isAtomicArrayType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_ARRAY_TYPES
        } ?: false

    private fun IrType.isTraceBaseType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() == TRACE_BASE_TYPE
        } ?: false

    private fun IrCall.isTraceInvoke(): Boolean =
        symbol.owner.isFromKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == INVOKE &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    private fun IrCall.isTraceAppend(): Boolean =
        symbol.owner.isFromKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == APPEND &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    private fun IrStatement.isTraceCall() = this is IrCall && (isTraceInvoke() || isTraceAppend())

    protected fun IrCall.isArrayElementGetter(): Boolean =
        dispatchReceiver?.let {
            it.type.isAtomicArrayType() && symbol.owner.name.asString() == GET
        } ?: false

    protected fun IrType.atomicToPrimitiveType(): IrType =
        when(classFqName?.shortName()?.asString()) {
            "AtomicInt" -> irBuiltIns.intType
            "AtomicLong" -> irBuiltIns.longType
            "AtomicBoolean" -> irBuiltIns.booleanType
            "AtomicRef" -> irBuiltIns.anyNType
            else -> error("Expected kotlinx.atomicfu.(AtomicInt|AtomicLong|AtomicBoolean|AtomicRef) type, but found ${this.render()}" + CONSTRAINTS_MESSAGE)
        }

    protected fun IrCall.isAtomicFactoryCall(): Boolean =
        symbol.owner.isFromKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                type.isAtomicValueType()

    protected fun IrFunction.isAtomicExtension(): Boolean =
        if (extensionReceiverParameter != null && extensionReceiverParameter!!.type.isAtomicValueType()) {
            require(this.isInline) { "Non-inline extension functions on kotlinx.atomicfu.Atomic* classes are not allowed, " +
                    "please add inline modifier to the function ${this.render()}." }
            require(this.visibility == DescriptorVisibilities.PRIVATE || this.visibility == DescriptorVisibilities.INTERNAL) {
                "Only private or internal extension functions on kotlinx.atomicfu.Atomic* classes are allowed, " +
                        "please make the extension function ${this.render()} private or internal."
            }
            true
        } else false

    protected fun IrCall.getCorrespondingProperty(): IrProperty =
        symbol.owner.correspondingPropertySymbol?.owner
            ?: error("Atomic property accessor ${this.render()} expected to have non-null correspondingPropertySymbol" + CONSTRAINTS_MESSAGE)

    protected fun IrExpression.isThisReceiver() =
        this is IrGetValue && symbol.owner.name.asString() == "<this>"

    protected val IrDeclaration.parentDeclarationContainer: IrDeclarationContainer
        get() = parents.filterIsInstance<IrDeclarationContainer>().firstOrNull()
            ?: error("In the sequence of parents for ${this.render()} no IrDeclarationContainer was found")

    protected val IrFunction.containingFunction: IrFunction
        get() {
            if (this.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return this
            return parents.filterIsInstance<IrFunction>().firstOrNull {
                it.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            } ?: error("In the sequence of parents for the local function ${this.render()} no containing function was found")
        }

    // A.kt -> A$VolatileWrapper$atomicfu
    // B -> B$VolatileWrapper$atomicfu
    protected fun mangleVolatileWrapperClassName(parent: IrDeclarationContainer): String =
        ((if (parent is IrFile) parent.name else (parent as IrClass).name.asString())).substringBefore(".") + VOLATILE_WRAPPER_SUFFIX

    protected fun mangleAtomicExtensionName(name: String, isArrayReceiver: Boolean) =
        if (isArrayReceiver) "$name$$ATOMICFU$$ARRAY" else "$name$$ATOMICFU"

    protected fun String.isMangledAtomicArrayExtension() = endsWith("$$ATOMICFU$$ARRAY")

    protected fun IrClass.isVolatileWrapper(v: DescriptorVisibility): Boolean =
        this.name.asString() == mangleVolatileWrapperClassName(this.parent as IrDeclarationContainer) + "$" + v

    protected fun IrValueParameter.capture(): IrGetValue = IrGetValueImpl(startOffset, endOffset, symbol.owner.type, symbol)

    protected fun IrType.isObject() = classOrNull?.owner?.kind == ClassKind.OBJECT

    protected fun IrProperty.atomicfuRender(): String =
        "val " + name.asString() + ": " + backingField?.type?.render()
}
