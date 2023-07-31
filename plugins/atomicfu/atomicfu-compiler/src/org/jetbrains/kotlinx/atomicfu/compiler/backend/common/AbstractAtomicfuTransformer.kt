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
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
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
import org.jetbrains.kotlin.name.Name
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
private const val LOOP = "loop"
private const val ACTION = "action\$$ATOMICFU"
private const val INDEX = "index\$$ATOMICFU"
private const val UPDATE = "update"

abstract class AbstractAtomicfuTransformer(val pluginContext: IrPluginContext) {

    companion object {
        const val CONSTRAINTS_MESSAGE =
            "\nPlease make sure that you follow these constraints for using atomic properties:\n" +
                    "   * To ensure that atomic properties are not accessed out of the current Kotlin module, it is necessary to declare atomic properties as private or internal.\n" +
                    "     Alternatively, you can make the containing class private or internal.\n" +
                    "     If you need to expose the atomic property value to the public, consider using a delegated property declared within the same scope:\n" +
                    "       ```\n" +
                    "       private val _a = atomic<T>(initial) \n" +
                    "       public var a: T by _a \n" +
                    "       ```\n" +
                    "   * Directly invoke operations on atomic properties, like this:\n" +
                    "       ```\n" +
                    "       val top = atomic<Node?>(null)\n" +
                    "       top.compareAndSet(null, Node(1)) // OK\n" +
                    "       ```\n" +
                    "   * Refrain from invoking atomic operations on local variables:\n" +
                    "       ```\n" +
                    "       val top = atomic<Node?>(null)\n" +
                    "       val tmp = top\n" +
                    "       tmp.compareAndSet(null, Node(1)) // DON'T DO THIS\n" +
                    "       ```\n" +
                    "   * Avoid leaking references to atomic values in other ways, such as returning them or passing them as parameters.\n" +
                    "   * Be cautious with the complexity of data flow within parameters of atomic operations.\n" +
                    "     For instance, instead of using intricate expression directly as an argument, e.g.:\n" +
                    "       ```\n" +
                    "       top.compareAndSet(cur, <complex_expression>)\n" +
                    "       ```\n" +
                    "     create a separate variable to hold the complex expression's value and then perform the operation:\n" +
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

    // Maps atomicfu atomic property to the corresponding volatile property.
    protected val atomicfuPropertyToVolatile = mutableMapOf<IrProperty, IrProperty>()
    // Maps atomicfu property to the atomic handler (field updater/atomic array).
    protected val atomicfuPropertyToAtomicHandler = mutableMapOf<IrProperty, IrProperty>()

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
    protected abstract val atomicFunctionsTransformer: AtomicFunctionCallTransformer

    private fun transformAtomicProperties(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(atomicPropertiesTransformer, null)
        }
    }

    private fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicExtensionTransformer(), null)
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
                    parentContainer.addTransformedAtomicArray(atomicProperty, index).also {
                        declarationsToBeRemoved.add(atomicProperty)
                    }
                }
                atomicProperty.isDelegatedToAtomic() -> parentContainer.transformDelegatedAtomic(atomicProperty)
                atomicProperty.isTrace() -> declarationsToBeRemoved.add(atomicProperty)
                else -> {}
            }
        }

        /**
         * Builds a volatile property that can be atomically updated instead of the given atomicfu property,
         * and replaces the original declaration in the parent class.
         * Returns the new volatile property.
         */
        abstract fun IrClass.addTransformedInClassAtomic(atomicProperty: IrProperty, index: Int): IrProperty

        /**
         * Builds a volatile property that can be atomically updated instead of the given static atomicfu property
         * and replaces the original declaration in the parent container.
         * Returns the new volatile property.
         */
        abstract fun IrDeclarationContainer.addTransformedStaticAtomic(atomicProperty: IrProperty, index: Int): IrProperty

        /**
         * Builds an array that can be atomically updated instead of the given atomicfu atomic array
         * and replaces the original declaration in the parent class.
         * Returns the generated array.
         * For JVM: atomic arrays are replaced with the corresponding java.util.concurrent.Atomic*Array
         * For Native: atomic arrays are replaced with the corresponding kotlin.concurrent.Atomic*Array
         * (In the future atomic arrays will be commonized in Kotlin stdlib)
         *
         * val intArr = kotlinx.atomicfu.AtomicIntArray(45)  -->  val intArr = java.util.concurrent.AtomicIntegerArray(45) // JVM
         * val intArr = kotlinx.atomicfu.AtomicIntArray(45)  -->  val intArr = kotlin.concurrent.AtomicIntArray(45) // Native
         */
        private fun IrDeclarationContainer.addTransformedAtomicArray(atomicProperty: IrProperty, index: Int): IrProperty {
            val parentContainer = this
            with(atomicSymbols.createBuilder(atomicProperty.symbol)) {
                val javaAtomicArrayField = buildAtomicArrayField(atomicProperty, parentContainer)
                return parentContainer.replacePropertyAtIndex(
                    javaAtomicArrayField,
                    atomicProperty.visibility,
                    isVar = false,
                    isStatic = parentContainer is IrFile,
                    index
                ).also {
                    atomicfuPropertyToAtomicHandler[atomicProperty] = it
                }
            }
        }

        /**
         * Transforms the given property that was delegated to the atomic property:
         * delegates accessors to the volatile property that was generated instead of the atomic property.
         */
        private fun IrDeclarationContainer.transformDelegatedAtomic(atomicProperty: IrProperty) {
            val getDelegate = atomicProperty.backingField?.initializer?.expression
            require(getDelegate is IrCall) { "Unexpected initializer of the delegated property ${atomicProperty.atomicfuRender()}: " +
                    "expected invocation of the delegate atomic property getter, but found ${getDelegate?.render()}." + CONSTRAINTS_MESSAGE }
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
                                "as the corresponding atomic property [${delegate.render()}] declared in [${delegate.parent.render()}]" + CONSTRAINTS_MESSAGE}
                    val volatileProperty = atomicfuPropertyToVolatile[delegate]
                        ?: error("No generated volatile property was found for the delegate atomic property ${delegate.render()}")
                    volatileProperty.backingField
                        ?: error("Volatile property ${volatileProperty.render()} corresponding to the atomic property ${delegate.render()} should have a non-null backingField")
                }
                else -> error("Unexpected initializer of the delegated property ${getDelegate.render()}" + CONSTRAINTS_MESSAGE)
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
                body = irBlockBody {
                    +irReturn(
                        if (this@transformAccessor.isGetter) {
                            irGetField(dispatchReceiver, delegateVolatileField)
                        } else {
                            irSetField(dispatchReceiver, delegateVolatileField, this@transformAccessor.valueParameters[0].capture())
                        }
                    )
                }
            }
        }

        /**
         * Builds a private volatile field initialized with the initial value of the given atomic property:
         * private val a = atomic(0)  --> private @Volatile a: Int = 0
         */
        protected fun AbstractAtomicfuIrBuilder.buildVolatileBackingField(
            atomicProperty: IrProperty,
            parentContainer: IrDeclarationContainer,
            tweakBooleanToInt: Boolean
        ): IrField {
            val atomicField = requireNotNull(atomicProperty.backingField) { "The backing field of the atomic property ${atomicProperty.render()} declared in ${parentContainer.render()} should not be null."  + CONSTRAINTS_MESSAGE }
            val fieldType = (atomicField.type as IrSimpleType).atomicToPrimitiveType()
            val initializer = atomicField.initializer?.expression
            if (initializer == null) {
                val initBlock = atomicField.getInitBlockForField(parentContainer)
                val initExprWithIndex = initBlock.getInitExprWithIndexFromInitBlock(atomicField.symbol)
                    ?: error("The atomic property ${atomicProperty.render()} was not initialized neither at the declaration, nor in the init block." + CONSTRAINTS_MESSAGE)
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
         * Builds an atomic array field initialized with the initial size of the given atomic array,
         * the generated field has the same visibility as the original atomic array:
         * internal val intArr = kotlinx.atomicfu.AtomicIntArray  --> internal val intArr = j.u.c.a.AtomicIntegerArray // JVM
         *                                                            internal val intArr = kotlin.concurrent.AtomicIntArray // Native
         */
        private fun AbstractAtomicfuIrBuilder.buildAtomicArrayField(
            atomicProperty: IrProperty,
            parentContainer: IrDeclarationContainer
        ): IrField {
            val atomicArrayField =
                requireNotNull(atomicProperty.backingField) { "The backing field of the atomic array [${atomicProperty.atomicfuRender()}] should not be null." + CONSTRAINTS_MESSAGE }
            val initializer = atomicArrayField.initializer?.expression
            if (initializer == null) {
                // replace field initialization in the init block
                val initBlock = atomicArrayField.getInitBlockForField(parentContainer)
                // property initialization order in the init block matters -> transformed initializer should be placed at the same position
                val initExprWithIndex = initBlock.getInitExprWithIndexFromInitBlock(atomicArrayField.symbol)
                    ?: error("The atomic array ${atomicProperty.render()} was not initialized neither at the declaration, nor in the init block." + CONSTRAINTS_MESSAGE)
                val atomicFactoryCall = initExprWithIndex.value.value
                val initExprIndex = initExprWithIndex.index
                val arraySize = atomicFactoryCall.getArraySizeArgument()
                return irAtomicArrayField(
                    atomicArrayField.name,
                    atomicSymbols.getAtomicArrayClassByAtomicfuArrayType(atomicArrayField.type),
                    atomicArrayField.isStatic,
                    atomicArrayField.annotations,
                    arraySize,
                    (atomicFactoryCall as IrFunctionAccessExpression).dispatchReceiver,
                    parentContainer
                ).also {
                    val initExpr = it.initializer?.expression
                        ?: error("The generated atomic array field [${it.render()}] should've already be initialized." + CONSTRAINTS_MESSAGE)
                    it.initializer = null
                    initBlock.updateFieldInitialization(atomicArrayField.symbol, it.symbol, initExpr, initExprIndex)
                }
            } else {
                val arraySize = initializer.getArraySizeArgument()
                return irAtomicArrayField(
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
        private fun IrAnonymousInitializer.getInitExprWithIndexFromInitBlock(
            oldFieldSymbol: IrFieldSymbol
        ): IndexedValue<IrSetField>? =
            body.statements.withIndex().singleOrNull { it.value is IrSetField && (it.value as IrSetField).symbol == oldFieldSymbol }?.let {
                @Suppress("UNCHECKED_CAST")
                it as IndexedValue<IrSetField>
            }

        private fun IrAnonymousInitializer.updateFieldInitialization(
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

        private fun IrField.getInitBlockForField(parentContainer: IrDeclarationContainer): IrAnonymousInitializer {
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

        private fun IrProperty.checkVisibility() =
            check((visibility == DescriptorVisibilities.PRIVATE || visibility == DescriptorVisibilities.INTERNAL) ||
                        (parent is IrClass &&
                                (parentAsClass.visibility == DescriptorVisibilities.PRIVATE || parentAsClass.visibility == DescriptorVisibilities.INTERNAL))) {
                "To ensure that atomic properties are not accessed out of the current Kotlin module, it is necessary to declare atomic properties as private or internal.\n" +
                "Please consider declaring [${this.atomicfuRender()}] from [${this.parent.render()}] as a private or internal property.\n" +
                        (if (parent is IrClass) "You may also make the containing class [${parentAsClass.render()}] private or internal.\n" else "") +
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

    private inner class AtomicExtensionTransformer : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.transformAllAtomicExtensions()
            return super.visitFile(declaration)
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformAllAtomicExtensions()
            return super.visitClass(declaration)
        }

        private fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.add(transformAtomicExtension(atomicExtension, this, false))
                declarations.add(transformAtomicExtension(atomicExtension, this, true))
                // the original atomic extension is removed
                declarations.remove(atomicExtension)
            }
        }

        private fun transformAtomicExtension(
            atomicExtension: IrFunction,
            parent: IrDeclarationContainer,
            isArrayReceiver: Boolean
        ): IrFunction {
            /**
             * At this step, only signature of the atomic extension is changed,
             * the body is just copied and will be transformed at the next step by AtomicFunctionCallTransformer.
             *
             * Two different signatures are generated for the case of atomic property receiver
             * and for the case of atomic array element receiver, due to different atomic updaters.
             *
             * Generated signatures for JVM:
             * inline fun AtomicInt.foo(arg: Int) --> inline fun foo$atomicfu(dispatchReceiver: Any?, atomicHandler: AtomicIntegerFieldUpdater, arg': Int)
             *                                        inline fun foo$atomicfu$array(dispatchReceiver: Any?, atomicHandler: AtomicIntegerArray, index: Int, arg': Int)
             *
             * Generated signatures for Native:
             * inline fun AtomicInt.foo(arg: Int) --> inline fun foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, arg': Int)
             *              *                         inline fun foo$atomicfu$array(atomicArray: AtomicIntegerArray, index: Int, arg': Int)
             */
            return buildTransformedAtomicExtensionSignature(atomicExtension, isArrayReceiver).apply {
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
                // all usages of the old type parameters should be remapped to the new type parameters.
                val typeRemapper = IrTypeParameterRemapper(atomicExtension.typeParameters.associateWith { this.typeParameters[it.index] })
                remapTypes(typeRemapper)
            }
        }
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
                        (propertyGetterCall.type as IrSimpleType).atomicToPrimitiveType()
                    }
                    val isArrayReceiver = when {
                        propertyGetterCall is IrCall -> propertyGetterCall.isArrayElementReceiver(data)
                        propertyGetterCall.isThisReceiver() -> data != null && data.name.asString().isMangledAtomicArrayExtension()
                        else -> error("Could not define the type of receiver of the function call ${expression.render()}: " +
                                    "found function call receiver ${propertyGetterCall.render()}, parentFunction = ${data?.render()}." + CONSTRAINTS_MESSAGE)
                    }
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

        private fun transformAtomicUpdateCallOnArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Atomic update call on the atomic array element is replaced
                 * with the call to the j.u.c.a.Atomic*Array (for JVM) or kotlin.concurrent.AtomicIntArray (for Native).
                 * The API of these classes is consistent -> this transformation is commonized.
                 *
                 * 1. Function call receiver is atomic property getter call.
                 *
                 * The call is delegated to the corresponding atomic array:
                 *
                 * val intArr = kotlinx.atomicfu.AtomicIntArray(10)         val intArr = AtomicIntArray(10)
                 * <get-intArr>()[5].compareAndSet(0, 5)             --->   intArr.compareAndSet(5, 0, 5)
                 *
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is <this> receiver of the original atomic extension:
                 *
                 * inline fun AtomicInt.foo(new: Int) {          inline fun foo$atomicfu$array(atomicArray: AtomicIntArray, index: Int, arg': Int)
                 *   this.getAndSet(value, new)            --->    atomicArray.getAndSet(index, new)
                 * }                                             }
                 */
                val getAtomicArray = getAtomicHandler(getPropertyReceiver, parentFunction)
                return callAtomicArray(
                    arrayClassSymbol = getAtomicArray.type.classOrNull!!,
                    functionName = functionName,
                    dispatchReceiver = getAtomicArray,
                    index = getPropertyReceiver.getArrayElementIndex(parentFunction),
                    valueArguments = expression.valueArguments,
                    isBooleanReceiver = valueType.isBoolean()
                )
            }
        }

        private fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * a.loop { value -> a.compareAndSet(value, 777) } -->
                 *
                 * inline fun <T> atomicfu$loop(dispatchReceiver: Any?, fu: AtomicIntegerFieldUpdater, action: (Int) -> Unit) {
                 *  while (true) {
                 *    val cur = fu.get()
                 *    action(cur)
                 *   }
                 * }
                 *
                 * a.atomicfu$loop(dispatchReceiver, fu) { ... }
                 */
                requireNotNull(parentFunction) { "Expected containing function of the call ${expression.render()}, but found null." + CONSTRAINTS_MESSAGE }
                val loopFunc = parentFunction.parentDeclarationContainer.getOrBuildInlineLoopFunction(
                    functionName = functionName,
                    valueType = if (valueType.isBoolean() && castBooleanFieldsToInt) irBuiltIns.intType else valueType,
                    isArrayReceiver = isArrayReceiver
                )
                // We have to copy this lambda, because it is passed to both foo$atomicfu and foo$atomicfu$array transformations,
                // and otherwise causes "lambda is already bound" error in K/N.
                val action = (expression.getValueArgument(0) as IrFunctionExpression).apply {
                    function.body?.transform(this@AtomicFunctionCallTransformer, parentFunction)
                    // In case of AtomicBoolean receiver on JVM the argument of the lambda is cast to Int, because the current value of the receiver is retrieved with the AtomicIntegerUpdater.
                    // AtomicBoolean.loop(action: (Boolean)-> Unit) -> action: (Int) -> Unit
                    // For native this is not necessary and the type is left unchanged.
                    if (function.valueParameters[0].type.isBoolean() && castBooleanFieldsToInt) {
                        function.valueParameters[0].type = irBuiltIns.intType
                        function.returnType = irBuiltIns.intType
                    }
                }.deepCopyWithSymbols(parentFunction)
                val syntheticArguments = buildSyntheticValueArgsForTransformedAtomicExtensionCall(expression, getPropertyReceiver, isArrayReceiver, parentFunction)
                return irCallWithArgs(
                    symbol = loopFunc.symbol,
                    dispatchReceiver = parentFunction.containingFunction.dispatchReceiverParameter?.capture(),
                    extensionReceiver = null,
                    valueArguments = syntheticArguments + action
                )
            }
        }

        private fun transformAtomicExtensionCall(
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
                 * Transformation variant of the atomic extension is chosen according to the type of receiver
                 * (atomic property -> foo$atomicfu, atomic array element -> foo$atomicfu$array)
                 *
                 * For JVM:
                 *
                 * inline fun AtomicInt.foo(arg: Int) {..}             inline fun foo$atomicfu(dispatchReceiver: Any?, fu: j.u.c.a.AtomicIntegerFieldUpdater, arg: Int)
                 * aClass._a.foo(arg)                              --> foo$atomicfu(aClass, _a$volatile$FU, arg)
                 *
                 * For Native:
                 * inline fun AtomicInt.foo(arg: Int) {..}             inline foo$atomicfu(refGetter: () -> KMutableProperty0<Int>, arg: Int)
                 * aClass._a.foo(arg)                              --> foo$atomicfu({_ -> aClass::_a}, arg)
                 *
                 * 2. Function is called in the body of the transformed atomic extension,
                 * the call receiver is the old <this> receiver of the extension.
                 * In this case value parameters captured from the parent function are passed as arguments.
                 *
                 * For JVM:
                 *
                 * inline fun AtomicInt.bar(new: Int)            inline fun bar$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int)
                 * inline fun AtomicInt.foo(new: Int) {          inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int)
                 *   bar(new)                              --->    bar$atomicfu(dispatchReceiver, handler, new)
                 * }
                 */
                requireNotNull(parentFunction) { "Expected containing function of the call ${expression.render()}, but found null." }
                val parent = originalAtomicExtension.parent as IrDeclarationContainer
                val transformedAtomicExtension = parent.getOrBuildTransformedAtomicExtension(originalAtomicExtension, isArrayReceiver)
                val syntheticValueArguments = buildSyntheticValueArgsForTransformedAtomicExtensionCall(expression, getPropertyReceiver, isArrayReceiver, parentFunction)
                return callAtomicExtension(
                    symbol = transformedAtomicExtension.symbol,
                    dispatchReceiver = expression.dispatchReceiver,
                    syntheticValueArguments = syntheticValueArguments,
                    valueArguments = expression.valueArguments
                )
            }
        }

        private fun IrDeclarationContainer.getOrBuildInlineLoopFunction(
            functionName: String,
            valueType: IrType,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            val parent = this
            val mangledName = mangleAtomicExtensionName(functionName, isArrayReceiver)
            findDeclaration<IrSimpleFunction> {
                // check the type of the receiver here
                it.name.asString() == mangledName && it.checkSyntheticParameterTypes(isArrayReceiver, valueType)
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

        private fun IrDeclarationContainer.getOrBuildTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            // Try find the transformed atomic extension in the parent container
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangleAtomicExtensionName(declaration.name.asString(), isArrayReceiver) &&
                        it.isTransformedAtomicExtension()
            }?.let { return it }
            /**
             * NOTE: this comment is applicable to the JVM backend incremental compialtion:
             * If the transformed declaration is not found then the call may be performed from another module
             * which depends on the module where declarations are generated from untransformed metadata (real transformed declarations are not there).
             * This happens if the call is performed from the test module or in case of incremental compilation.
             *
             * We build a fake declaration here: it's signature equals the one of the real transformed declaration,
             * it doesn't have body and won't be generated. It is placed in the call site and
             * during compilation this fake declaration will be resolved to the real transformed declaration.
             */
            return buildTransformedAtomicExtensionSignature(declaration, isArrayReceiver)
        }

        protected fun getAtomicHandler(atomicCallReceiver: IrExpression, parentFunction: IrFunction?): IrExpression =
            when {
                atomicCallReceiver is IrCall -> {
                    val isArrayReceiver = atomicCallReceiver.isArrayElementGetter()
                    val getAtomicProperty = if (isArrayReceiver) atomicCallReceiver.dispatchReceiver as IrCall else atomicCallReceiver
                    val atomicProperty = getAtomicProperty.getCorrespondingProperty()
                    val atomicHandlerProperty = atomicfuPropertyToAtomicHandler[atomicProperty]
                        ?: error("No atomic handler found for the atomic property ${atomicProperty.render()}, \n" +
                                         "these properties were registered: ${
                                             atomicfuPropertyToAtomicHandler.keys.toList().joinToString("\n") { it.render() }
                                         }" + CONSTRAINTS_MESSAGE)
                    with(atomicSymbols.createBuilder(atomicCallReceiver.symbol)) {
                        // dispatchReceiver for get-a$FU() is null, because a$FU is a static property
                        // dispatchReceiver for get-arr'() is equal to the dispatchReceiver of the original getter
                        irGetProperty(atomicHandlerProperty, if (isArrayReceiver) getAtomicProperty.dispatchReceiver else null)
                    }
                }
                atomicCallReceiver.isThisReceiver() -> {
                    requireNotNull(parentFunction) { "Expected containing function of the call with receiver ${atomicCallReceiver.render()}, but found null." + CONSTRAINTS_MESSAGE}
                    require(parentFunction.isTransformedAtomicExtension())
                    val isArrayReceiver =  parentFunction.name.asString().isMangledAtomicArrayExtension()
                    if (isArrayReceiver) parentFunction.valueParameters[0].capture() else parentFunction.valueParameters[1].capture()
                }
                else -> error("Unexpected type of atomic function call receiver: ${atomicCallReceiver.render()}, parentFunction = ${parentFunction?.render()}." + CONSTRAINTS_MESSAGE)
            }

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

        // Generates value arguments for the transformed atomic extension invocation.
        abstract fun buildSyntheticValueArgsForTransformedAtomicExtensionCall(
            expression: IrCall,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction
        ): List<IrExpression?>

        abstract fun IrValueParameter.remapValueParameter(transformedExtension: IrFunction): IrValueParameter?

        protected fun IrFunction.isTransformedAtomicExtension(): Boolean {
            val isArrayReceiver = name.asString().isMangledAtomicArrayExtension()
            return if (isArrayReceiver) checkSyntheticArrayElementExtensionParameter() else checkSyntheticAtomicExtensionParameters()
        }

        abstract fun IrFunction.checkSyntheticAtomicExtensionParameters(): Boolean

        abstract fun IrFunction.checkSyntheticArrayElementExtensionParameter(): Boolean

        abstract fun IrFunction.checkSyntheticParameterTypes(isArrayReceiver: Boolean, receiverValueType: IrType): Boolean

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

        private fun IrSimpleFunction.generateAtomicfuLoop(valueType: IrType) {
            addSyntheticValueParametersToTransformedAtomicExtension(false, valueType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuLoopBody(valueType, valueParameters)
            }
            returnType = irBuiltIns.unitType
        }

        private fun IrSimpleFunction.generateAtomicfuArrayLoop(valueType: IrType) {
            val atomicfuArrayClass = atomicSymbols.getAtomicArrayClassByValueType(valueType)
            addSyntheticValueParametersToTransformedAtomicExtension(true, valueType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuArrayLoopBody(atomicfuArrayClass, valueType, valueParameters)
            }
            returnType = irBuiltIns.unitType
        }

        private fun IrSimpleFunction.generateAtomicfuUpdate(functionName: String, valueType: IrType) {
            addSyntheticValueParametersToTransformedAtomicExtension(false, valueType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, valueType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuUpdateBody(
                    functionName,
                    valueType,
                    valueParameters
                )
            }
            returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
        }

        private fun IrSimpleFunction.generateAtomicfuArrayUpdate(functionName: String, valueType: IrType) {
            val atomicfuArrayClass = atomicSymbols.getAtomicArrayClassByValueType(valueType)
            addSyntheticValueParametersToTransformedAtomicExtension(true, valueType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, valueType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuArrayUpdateBody(
                    functionName,
                    valueType,
                    atomicfuArrayClass,
                    valueParameters
                )
            }
            returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
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

    // If true, transforms AtomicBoolean properties to Int volatile property + updater.
    // Tweaks types of value arguments where this property is used accordingly.
    abstract val castBooleanFieldsToInt: Boolean

    // Builds the signature of the transformed atomic extension.
    abstract fun buildTransformedAtomicExtensionSignature(atomicExtension: IrFunction, isArrayReceiver: Boolean): IrSimpleFunction
    // Adds value parameters for transformed inline extension functions.
    abstract fun IrFunction.addSyntheticValueParametersToTransformedAtomicExtension(isArrayReceiver: Boolean, valueType: IrType)

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

    protected fun IrSimpleType.atomicToPrimitiveType(): IrType =
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
            ?: error("In the sequence of parents for ${this.render()} no IrDeclarationContainer was found" + CONSTRAINTS_MESSAGE)

    protected val IrFunction.containingFunction: IrFunction
        get() {
            if (this.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return this
            return parents.filterIsInstance<IrFunction>().firstOrNull {
                it.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            } ?: error("In the sequence of parents for the local function ${this.render()} no containing function was found" + CONSTRAINTS_MESSAGE)
        }

    // atomic(value = 0) -> 0
    protected fun IrExpression.getAtomicFactoryValueArgument(): IrExpression {
        require(this is IrCall) { "Expected atomic factory invocation but found: ${this.render()}" }
        return getValueArgument(0)?.deepCopyWithSymbols()
            ?: error("Atomic factory should take at least one argument: ${this.render()}" + CONSTRAINTS_MESSAGE)
    }

    // AtomicIntArray(size = 10) -> 10
    protected fun IrExpression.getArraySizeArgument(): IrExpression {
        require(this is IrFunctionAccessExpression) {
            "Expected atomic array factory invocation, but found: ${this.render()}."
        }
        return getValueArgument(0)?.deepCopyWithSymbols()
            ?: error("Atomic array factory should take at least one argument: ${this.render()}" + CONSTRAINTS_MESSAGE)
    }

    protected fun IrExpression.getArrayElementIndex(parentFunction: IrFunction?): IrExpression =
        when {
            this is IrCall -> getValueArgument(0)!!
            this.isThisReceiver() -> {
                require(parentFunction != null)
                val index = parentFunction.valueParameters[1]
                require(index.name.asString() == INDEX && index.type == irBuiltIns.intType)
                index.capture()
            }
            else -> error("Unexpected type of atomic array receiver: ${this.render()}, parentFunction = ${parentFunction?.render()}\n" + CONSTRAINTS_MESSAGE)
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
