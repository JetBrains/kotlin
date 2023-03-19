/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import kotlin.collections.set

private const val AFU_PKG = "kotlinx.atomicfu"
private const val TRACE_BASE_TYPE = "TraceBase"
private const val ATOMIC_VALUE_FACTORY = "atomic"
private const val INVOKE = "invoke"
private const val APPEND = "append"
private const val GET = "get"
private const val ATOMICFU = "atomicfu"
private const val ATOMIC_ARRAY_RECEIVER_SUFFIX = "\$array"
private const val DISPATCH_RECEIVER = "${ATOMICFU}\$dispatchReceiver"
private const val ATOMIC_HANDLER = "${ATOMICFU}\$handler"
private const val ACTION = "${ATOMICFU}\$action"
private const val INDEX = "${ATOMICFU}\$index"
private const val VOLATILE_WRAPPER_SUFFIX = "\$VolatileWrapper"
private const val LOOP = "loop"
private const val UPDATE = "update"

class AtomicfuJvmIrTransformer(
    private val context: IrPluginContext,
    private val atomicSymbols: AtomicSymbols
) {
    private val irBuiltIns = context.irBuiltIns

    private val AFU_VALUE_TYPES: Map<String, IrType> = mapOf(
        "AtomicInt" to irBuiltIns.intType,
        "AtomicLong" to irBuiltIns.longType,
        "AtomicBoolean" to irBuiltIns.booleanType,
        "AtomicRef" to irBuiltIns.anyNType
    )

    private val ATOMICFU_INLINE_FUNCTIONS = setOf("loop", "update", "getAndUpdate", "updateAndGet")
    protected val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")
    protected val ATOMIC_ARRAY_TYPES = setOf("AtomicIntArray", "AtomicLongArray", "AtomicBooleanArray", "AtomicArray")

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicFields(moduleFragment)
        transformAtomicExtensions(moduleFragment)
        transformAtomicfuDeclarations(moduleFragment)
        for (irFile in moduleFragment.files) {
            irFile.patchDeclarationParents()
        }
    }

    private fun transformAtomicFields(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicHandlerTransformer(), null)
        }
    }

    private fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicExtensionTransformer(), null)
        }
    }

    private fun transformAtomicfuDeclarations(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicfuTransformer(), null)
        }
    }

    private val propertyToAtomicHandler = mutableMapOf<IrProperty, IrProperty>()

    private inner class AtomicHandlerTransformer : IrElementTransformer<IrFunction?> {
        override fun visitClass(declaration: IrClass, data: IrFunction?): IrStatement {
            declaration.declarations.filter(::fromKotlinxAtomicfu).forEach {
                (it as IrProperty).transformAtomicfuProperty(declaration)
            }
            return super.visitClass(declaration, data)
        }

        override fun visitFile(declaration: IrFile, data: IrFunction?): IrFile {
            declaration.declarations.filter(::fromKotlinxAtomicfu).forEach {
                (it as IrProperty).transformAtomicfuProperty(declaration)
            }
            return super.visitFile(declaration, data)
        }

        private fun IrProperty.transformAtomicfuProperty(parent: IrDeclarationContainer) {
            val atomicfuProperty = this
            val isTopLevel = parent is IrFile || (parent is IrClass && parent.kind == ClassKind.OBJECT)
            when {
                isAtomic() -> {
                    if (isTopLevel) {
                        val wrapperClass = buildWrapperClass(atomicfuProperty, parent).also {
                            // add a static instance of the generated wrapper class to the parent container
                            context.buildClassInstance(it, parent, atomicfuProperty.visibility, true)
                        }
                        transformAtomicProperty(wrapperClass)
                        moveFromFileToClass(parent, wrapperClass)
                    } else {
                        transformAtomicProperty(parent as IrClass)
                    }
                }
                isDelegatedToAtomic() -> transformDelegatedProperty(parent)
                isAtomicArray() -> transformAtomicArrayProperty(parent)
                isTrace() -> parent.declarations.remove(atomicfuProperty)
                else -> {}
            }
        }

        private fun IrProperty.moveFromFileToClass(
            parentFile: IrDeclarationContainer,
            parentClass: IrClass
        ) {
            parentFile.declarations.remove(this)
            parentClass.declarations.add(this)
            parent = parentClass
        }

        private fun IrProperty.transformAtomicProperty(parentClass: IrClass) {
            // Atomic property transformation:
            // 1. replace it's backingField with a volatile property of atomic value type
            // 2. create j.u.c.a.Atomic*FieldUpdater for this volatile property to handle it's value atomically
            // val a = atomic(0) ->
            // volatile var a: Int = 0
            // val a$FU = AtomicIntegerFieldUpdater.newUpdater(parentClass, "a")
            //
            // Top-level atomic properties transformation:
            // 1. replace it's backingField with a volatile property of atomic value type
            // 2. wrap this volatile property into the generated class
            // 3. create j.u.c.a.Atomic*FieldUpdater for the volatile property to handle it's value atomically
            // val a = atomic(0) ->
            // class A$ParentFile$VolatileWrapper { volatile var a: Int = 0 }
            // val a$FU = AtomicIntegerFieldUpdater.newUpdater(A$ParentFile$VolatileWrapper::class, "a")
            backingField = buildVolatileRawField(this, parentClass)
            // update property accessors
            context.addDefaultGetter(this, parentClass)
            val fieldUpdater = addJucaAFUProperty(this, parentClass)
            registerAtomicHandler(fieldUpdater)
        }

        private fun IrProperty.transformAtomicArrayProperty(parent: IrDeclarationContainer) {
            // Replace atomicfu array classes with the corresponding atomic arrays from j.u.c.a.:
            // val intArr = atomicArrayOfNulls<Any?>(5) ->
            // val intArr = AtomicReferenceArray(5)
            backingField = buildJucaArrayField(this, parent)
            // update property accessors
            context.addDefaultGetter(this, parent)
            registerAtomicHandler(this)
        }

        private fun IrProperty.transformDelegatedProperty(parent: IrDeclarationContainer) {
            backingField?.let {
                it.initializer?.let {
                    val initializer = it.expression as IrCall
                    if (initializer.isAtomicFactory()) {
                        // Property delegated to atomic factory invocation:
                        // 1. replace it's backingField with a volatile property of value type
                        // 2. transform getter/setter
                        // var a by atomic(0) ->
                        // volatile var a: Int = 0
                        val volatileField = buildVolatileRawField(this, parent).also {
                            parent.declarations.add(it)
                        }
                        backingField = null
                        getter?.transformAccessor(volatileField, getter?.dispatchReceiverParameter?.capture())
                        setter?.transformAccessor(volatileField, setter?.dispatchReceiverParameter?.capture())
                    } else {
                        // Property delegated to the atomic property:
                        // 1. delegate it's accessors to get/set of the backingField of the atomic delegate
                        // (that is already transformed to a volatile field of value type)
                        // val _a = atomic(0)
                        // var a by _a ->
                        // volatile var _a: Int = 0
                        // var a by _a
                        val atomicProperty = initializer.getCorrespondingProperty()
                        val volatileField = atomicProperty.backingField!!
                        backingField = null
                        if (atomicProperty.isTopLevel()) {
                            with(atomicSymbols.createBuilder(symbol)) {
                                val wrapper = getStaticVolatileWrapperInstance(atomicProperty)
                                getter?.transformAccessor(volatileField, getProperty(wrapper, null))
                                setter?.transformAccessor(volatileField, getProperty(wrapper, null))
                            }
                        } else {
                            if (this.parent == atomicProperty.parent) {
                                //class A {
                                //    val _a = atomic()
                                //    var a by _a
                                //}
                                getter?.transformAccessor(volatileField, getter?.dispatchReceiverParameter?.capture())
                                setter?.transformAccessor(volatileField, setter?.dispatchReceiverParameter?.capture())
                            } else {
                                //class A {
                                //    val _a = atomic()
                                //    inner class B {
                                //        var a by _a
                                //    }
                                //}
                                val thisReceiver = atomicProperty.parentAsClass.thisReceiver
                                getter?.transformAccessor(volatileField, thisReceiver?.capture())
                                setter?.transformAccessor(volatileField, thisReceiver?.capture())
                            }
                        }
                    }
                }
            }
        }

        private fun IrFunction.transformAccessor(volatileField: IrField, parent: IrExpression?) {
            val accessor = this
            with(atomicSymbols.createBuilder(symbol)) {
                body = irExprBody(
                    irReturn(
                        if (accessor.isGetter) {
                            irGetField(parent, volatileField)
                        } else {
                            irSetField(parent, volatileField, accessor.valueParameters[0].capture())
                        }
                    )
                )
            }
        }

        private fun IrProperty.registerAtomicHandler(atomicHandlerProperty: IrProperty) {
            propertyToAtomicHandler[this] = atomicHandlerProperty
        }

        private fun buildVolatileRawField(property: IrProperty, parent: IrDeclarationContainer): IrField =
            // Generate a new backing field for the given property:
            // a volatile variable of the atomic value type
            // val a = atomic(0)
            // volatile var a: Int = 0
            property.backingField?.let { backingField ->
                val init = backingField.initializer?.expression
                val valueType = backingField.type.atomicToValueType()
                context.irFactory.buildField {
                    name = property.name
                    type = if (valueType.isBoolean()) irBuiltIns.intType else valueType
                    isFinal = false
                    isStatic = parent is IrFile
                    visibility = DescriptorVisibilities.PRIVATE
                }.apply {
                    if (init != null) {
                        val value = (init as IrCall).getAtomicFactoryValueArgument()
                        initializer = IrExpressionBodyImpl(value)
                    } else {
                        // if lateinit field -> initialize it in IrAnonymousInitializer
                        transformLateInitializer(backingField, parent) { init ->
                            val value = (init as IrCall).getAtomicFactoryValueArgument()
                            with(atomicSymbols.createBuilder(this.symbol)) {
                                irSetField((parent as? IrClass)?.thisReceiver?.capture(), this@apply, value)
                            }
                        }
                    }
                    annotations = backingField.annotations + atomicSymbols.volatileAnnotationConstructorCall
                    this.parent = parent
                }
            } ?: error("Backing field of the atomic property ${property.render()} is null")

        private fun addJucaAFUProperty(atomicProperty: IrProperty, parentClass: IrClass): IrProperty =
            // Generate an atomic field updater for the volatile backing field of the given property:
            // val a = atomic(0)
            // volatile var a: Int = 0
            // val a$FU = AtomicIntegerFieldUpdater.newUpdater(parentClass, "a")
            atomicProperty.backingField?.let { volatileField ->
                val fuClass = atomicSymbols.getJucaAFUClass(volatileField.type)
                val fieldName = volatileField.name.asString()
                val fuField = context.irFactory.buildField {
                    name = Name.identifier(mangleFUName(fieldName))
                    type = fuClass.defaultType
                    isFinal = true
                    isStatic = true
                    visibility = DescriptorVisibilities.PRIVATE
                }.apply {
                    initializer = IrExpressionBodyImpl(
                        with(atomicSymbols.createBuilder(symbol)) {
                            newUpdater(fuClass, parentClass, irBuiltIns.anyNType, fieldName)
                        }
                    )
                    parent = parentClass
                }
                return context.buildPropertyForBackingField(fuField, parentClass, atomicProperty.visibility, true)
            } ?: error("Atomic property ${atomicProperty.render()} should have a non-null generated volatile backingField")

        private fun buildJucaArrayField(atomicfuArrayProperty: IrProperty, parent: IrDeclarationContainer) =
            atomicfuArrayProperty.backingField?.let { atomicfuArray ->
                val init = atomicfuArray.initializer?.expression as? IrFunctionAccessExpression
                val atomicArrayClass = atomicSymbols.getAtomicArrayClassByAtomicfuArrayType(atomicfuArray.type)
                context.irFactory.buildField {
                    name = atomicfuArray.name
                    type = atomicArrayClass.defaultType
                    isFinal = atomicfuArray.isFinal
                    isStatic = atomicfuArray.isStatic
                    visibility = DescriptorVisibilities.PRIVATE
                }.apply {
                    if (init != null) {
                        this.initializer = IrExpressionBodyImpl(
                            with(atomicSymbols.createBuilder(symbol)) {
                                val size = init.getArraySizeArgument()
                                newJucaAtomicArray(atomicArrayClass, size, init.dispatchReceiver)
                            }
                        )
                    } else {
                        // if lateinit field -> initialize it in IrAnonymousInitializer
                        transformLateInitializer(atomicfuArray, parent) { init ->
                            init as IrFunctionAccessExpression
                            val size = init.getArraySizeArgument()
                            with(atomicSymbols.createBuilder(this.symbol)) {
                                irSetField(
                                    (parent as? IrClass)?.thisReceiver?.capture(),
                                    this@apply,
                                    newJucaAtomicArray(atomicArrayClass, size, init.dispatchReceiver)
                                )
                            }
                        }
                    }
                    annotations = atomicfuArray.annotations
                    this.parent = parent
                }
            } ?: error("Atomic property does not have backingField")

        private fun buildWrapperClass(atomicProperty: IrProperty, parentContainer: IrDeclarationContainer): IrClass =
            atomicSymbols.buildClass(
                FqName(getVolatileWrapperClassName(atomicProperty)),
                ClassKind.CLASS,
                parentContainer
            ).apply {
                val irClass = this
                irClass.visibility = atomicProperty.visibility
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

        private fun transformLateInitializer(
            field: IrField,
            parent: IrDeclarationContainer,
            generateIrSetField: (init: IrExpression) -> IrExpression
        ) {
            for (declaration in parent.declarations) {
                if (declaration is IrAnonymousInitializer) {
                    declaration.body.statements.singleOrNull {
                        it is IrSetField && it.symbol == field.symbol
                    }?.let {
                        declaration.body.statements.remove(it)
                        val init = (it as IrSetField).value
                        declaration.body.statements.add(
                            generateIrSetField(init)
                        )
                    }
                }
            }
        }

        private fun IrCall.getAtomicFactoryValueArgument() =
            getValueArgument(0)?.deepCopyWithSymbols()
                ?: error("Atomic factory should take at least one argument: ${this.render()}")

        private fun IrFunctionAccessExpression.getArraySizeArgument() =
            getValueArgument(0)?.deepCopyWithSymbols()
                ?: error("Atomic array constructor should take at least one argument: ${this.render()}")

        private fun fromKotlinxAtomicfu(declaration: IrDeclaration): Boolean =
            declaration is IrProperty &&
                    declaration.backingField?.type?.isKotlinxAtomicfuPackage() ?: false

        private fun IrProperty.isAtomic(): Boolean =
            !isDelegated && backingField?.type?.isAtomicValueType() ?: false

        private fun IrProperty.isDelegatedToAtomic(): Boolean =
            isDelegated && backingField?.type?.isAtomicValueType() ?: false

        private fun IrProperty.isAtomicArray(): Boolean =
            backingField?.type?.isAtomicArrayType() ?: false

        private fun IrProperty.isTrace(): Boolean =
            backingField?.type?.isTraceBaseType() ?: false

        private fun IrProperty.isTopLevel(): Boolean =
            parent is IrClass && (parent as IrClass).name.asString().endsWith(VOLATILE_WRAPPER_SUFFIX)

        private fun mangleFUName(fieldName: String) = "$fieldName\$FU"
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
            // Transform the signature of kotlinx.atomicfu.Atomic* class extension functions:
            // inline fun AtomicInt.foo(arg: T)
            // For every signature there are 2 new declarations generated (because of different types of atomic handlers):
            // 1. for the case of atomic value receiver at the invocation:
            // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': T)
            // 2. for the case of atomic array element receiver at the invocation:
            // inline fun foo$atomicfu$array(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerArray, index: Int, arg': T)
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.add(generateAtomicExtension(atomicExtension, this, false))
                declarations.add(generateAtomicExtension(atomicExtension, this, true))
                declarations.remove(atomicExtension)
            }
        }

        private fun generateAtomicExtension(
            atomicExtension: IrFunction,
            parent: IrDeclarationParent,
            isArrayReceiver: Boolean
        ): IrFunction {
            val mangledName = mangleFunctionName(atomicExtension.name.asString(), isArrayReceiver)
            val valueType = atomicExtension.extensionReceiverParameter!!.type.atomicToValueType()
            return context.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = atomicExtension.visibility
            }.apply {
                val newDeclaration = this
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
                // the body will be transformed later by `AtomicFUTransformer`
                body = atomicExtension.body?.deepCopyWithSymbols(this)
                body?.transform(
                    object : IrElementTransformerVoid() {
                        override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
                            if (expression.returnTargetSymbol == atomicExtension.symbol) {
                                with(atomicSymbols.createBuilder(newDeclaration.symbol)) {
                                    irReturn(expression.value)
                                }
                            } else {
                                expression
                            }
                        )
                    }, null
                )
                returnType = atomicExtension.returnType
                this.parent = parent
            }
        }
    }

    private data class AtomicFieldInfo(val dispatchReceiver: IrExpression?, val atomicHandler: IrExpression)

    private inner class AtomicfuTransformer : IrElementTransformer<IrFunction?> {
        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            (expression.extensionReceiver ?: expression.dispatchReceiver)?.transform(this, data)?.let {
                with(atomicSymbols.createBuilder(expression.symbol)) {
                    val receiver = if (it is IrTypeOperatorCallImpl) it.argument else it
                    if (receiver.type.isAtomicValueType()) {
                        val valueType = if (it is IrTypeOperatorCallImpl) {
                            // If receiverExpression is a cast `s as AtomicRef<String>`
                            // then valueType is the type argument of Atomic* class `String`
                            (it.type as IrSimpleType).arguments[0] as IrSimpleType
                        } else {
                            receiver.type.atomicToValueType()
                        }
                        getAtomicFieldInfo(receiver, data)?.let { (dispatchReceiver, atomicHandler) ->
                            val isArrayReceiver = atomicSymbols.isAtomicArrayHandlerType(atomicHandler.type)
                            if (expression.symbol.isKotlinxAtomicfuPackage()) {
                                // Transform invocations of atomic functions, delegating them to the atomicHandler.
                                // 1. For atomic properties (j.u.c.a.Atomic*FieldUpdater):
                                // a.compareAndSet(expect, update) -> a$FU.compareAndSet(dispatchReceiver, expect, update)
                                // 2. For atomic array elements (j.u.c.a.Atomic*Array):
                                // intArr[0].compareAndSet(expect, update) -> intArr.compareAndSet(index, expect, update)
                                val functionName = expression.symbol.owner.name.asString()
                                if (functionName in ATOMICFU_INLINE_FUNCTIONS) {
                                    // If the inline atomicfu loop function was invoked
                                    // a.loop { value -> a.compareAndSet(value, 777) }
                                    // then loop function is generated to replace this declaration.
                                    // `AtomicInt.loop(action: (Int) -> Unit)` for example will be replaced with
                                    // inline fun <T> atomicfu$loop(atomicHandler: AtomicIntegerFieldUpdater, action: (Int) -> Unit) {
                                    //     while (true) {
                                    //         val cur = atomicfu$handler.get()
                                    //         atomicfu$action(cur)
                                    //     }
                                    // }
                                    // And the invocation in place will be transformed:
                                    // a.atomicfu$loop(atomicHandler, action)
                                    require(data != null) { "Function containing loop invocation ${expression.render()} is null" }
                                    val loopFunc = data.parentDeclarationContainer.getOrBuildInlineLoopFunction(
                                        functionName = functionName,
                                        valueType = if (valueType.isBoolean()) irBuiltIns.intType else valueType,
                                        isArrayReceiver = isArrayReceiver
                                    )
                                    val action = (expression.getValueArgument(0) as IrFunctionExpression).apply {
                                        function.body?.transform(this@AtomicfuTransformer, data)
                                        if (function.valueParameters[0].type.isBoolean()) {
                                            function.valueParameters[0].type = irBuiltIns.intType
                                            function.returnType = irBuiltIns.intType
                                        }
                                    }
                                    val loopCall = irCallWithArgs(
                                        symbol = loopFunc.symbol,
                                        dispatchReceiver = data.containingFunction.dispatchReceiverParameter?.capture(),
                                        valueArguments = if (isArrayReceiver) {
                                            val index = receiver.getArrayElementIndex(data)
                                            listOf(atomicHandler, index, action)
                                        } else {
                                            listOf(atomicHandler, action, dispatchReceiver)
                                        }
                                    )
                                    return super.visitCall(loopCall, data)
                                }
                                val irCall = if (isArrayReceiver) {
                                    callAtomicArray(
                                        arrayClassSymbol = atomicHandler.type.classOrNull!!,
                                        functionName = functionName,
                                        dispatchReceiver = atomicHandler,
                                        index = receiver.getArrayElementIndex(data),
                                        valueArguments = expression.getValueArguments(),
                                        isBooleanReceiver = valueType.isBoolean()
                                    )
                                } else {
                                    callFieldUpdater(
                                        fieldUpdaterSymbol = atomicSymbols.getJucaAFUClass(valueType),
                                        functionName = functionName,
                                        dispatchReceiver = atomicHandler,
                                        obj = dispatchReceiver,
                                        valueArguments = expression.getValueArguments(),
                                        castType = if (it is IrTypeOperatorCall) valueType else null,
                                        isBooleanReceiver = valueType.isBoolean()
                                    )
                                }
                                return super.visitExpression(irCall, data)
                            }
                            if (expression.symbol.owner.isInline && expression.extensionReceiver != null) {
                                // Transform invocation of the kotlinx.atomicfu.Atomic* class extension functions,
                                // delegating them to the corresponding transformed atomic extensions:
                                // for atomic property recevers:
                                // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) { ... }
                                // for atomic array element receivers:
                                // inline fun foo$atomicfu$array(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerArray, index: Int, arg': Int) { ... }

                                // The invocation on the atomic property will be transformed:
                                // a.foo(arg) -> a.foo$atomicfu(dispatchReceiver, atomicHandler, arg)
                                // The invocation on the atomic array element will be transformed:
                                // a.foo(arg) -> a.foo$atomicfu$array(dispatchReceiver, atomicHandler, index, arg)
                                val declaration = expression.symbol.owner
                                val parent = declaration.parent as IrDeclarationContainer
                                val transformedAtomicExtension = parent.getTransformedAtomicExtension(declaration, isArrayReceiver)
                                require(data != null) { "Function containing invocation of the extension function ${expression.render()} is null" }
                                val irCall = callAtomicExtension(
                                    symbol = transformedAtomicExtension.symbol,
                                    dispatchReceiver = expression.dispatchReceiver,
                                    syntheticValueArguments = if (isArrayReceiver) {
                                        listOf(dispatchReceiver, atomicHandler, receiver.getArrayElementIndex(data))
                                    } else {
                                        listOf(dispatchReceiver, atomicHandler)
                                    },
                                    valueArguments = expression.getValueArguments()
                                )
                                return super.visitCall(irCall, data)
                            }
                        } ?: return expression
                    }
                }
            }
            return super.visitCall(expression, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction?): IrExpression {
            // For transformed atomic extension functions
            // replace old value parameters with the new parameters of the transformed declaration:
            // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
            //     arg -> arg`
            //}
            if (expression.symbol is IrValueParameterSymbol) {
                val valueParameter = expression.symbol.owner as IrValueParameter
                val parent = valueParameter.parent
                if (data != null && data.isTransformedAtomicExtension() &&
                    parent is IrFunctionImpl && !parent.isTransformedAtomicExtension() &&
                    parent.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                ) {
                    val index = valueParameter.index
                    if (index < 0 && !valueParameter.type.isAtomicValueType()) {
                        // index == -1 for `this` parameter
                        return data.dispatchReceiverParameter?.capture() ?: error { "Dispatchreceiver of ${data.render()} is null" }
                    }
                    if (index >= 0) {
                        val shift = if (data.name.asString().endsWith(ATOMIC_ARRAY_RECEIVER_SUFFIX)) 3 else 2
                        val transformedValueParameter = data.valueParameters[index + shift]
                        return buildGetValue(
                            expression.startOffset,
                            expression.endOffset,
                            transformedValueParameter.symbol
                        )
                    }
                }
            }
            return super.visitGetValue(expression, data)
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

        private fun AtomicfuIrBuilder.getAtomicFieldInfo(
            receiver: IrExpression,
            parentFunction: IrFunction?
        ): AtomicFieldInfo? {
            // For the given function call receiver of atomic type returns:
            // the dispatchReceiver and the atomic handler of the corresponding property
            when {
                receiver is IrCall -> {
                    // Receiver is a property getter call
                    val isArrayReceiver = receiver.isArrayElementGetter()
                    val getAtomicProperty = if (isArrayReceiver) receiver.dispatchReceiver as IrCall else receiver
                    val atomicProperty = getAtomicProperty.getCorrespondingProperty()
                    val dispatchReceiver = getAtomicProperty.dispatchReceiver.let {
                        val isObjectReceiver = it?.type?.classOrNull?.owner?.kind == ClassKind.OBJECT
                        if (it == null || isObjectReceiver) {
                            if (getAtomicProperty.symbol.owner.returnType.isAtomicValueType()) {
                                // for top-level atomic properties get wrapper class instance as a parent
                                getProperty(getStaticVolatileWrapperInstance(atomicProperty), null)
                            } else if (isObjectReceiver && getAtomicProperty.symbol.owner.returnType.isAtomicArrayType()) {
                                it
                            }
                            else null
                        } else it
                    }
                    // atomic property is handled by the Atomic*FieldUpdater instance
                    // atomic array elements handled by the Atomic*Array instance
                    val atomicHandler = propertyToAtomicHandler[atomicProperty]
                        ?: error("No atomic handler found for the atomic property ${atomicProperty.render()}")
                    return AtomicFieldInfo(
                        dispatchReceiver = dispatchReceiver,
                        atomicHandler = getProperty(
                            atomicHandler,
                            if (isArrayReceiver && dispatchReceiver?.type?.classOrNull?.owner?.kind != ClassKind.OBJECT) dispatchReceiver else null
                        )
                    )
                }
                receiver.isThisReceiver() -> {
                    // Receiver is <this> extension receiver of transformed atomic extesnion declaration.
                    // The old function before `AtomicExtensionTransformer` application:
                    // inline fun foo(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
                    //    this().lazySet(arg)
                    //}
                    // By this moment the atomic extension has it's signature transformed,
                    // but still has the untransformed body copied from the old declaration:
                    // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
                    //    this().lazySet(arg) <----
                    //}
                    // The dispatchReceiver and the atomic handler for this receiver are the corresponding arguments
                    // passed to the transformed declaration/
                    return if (parentFunction != null && parentFunction.isTransformedAtomicExtension()) {
                        val params = parentFunction.valueParameters.take(2).map { it.capture() }
                        AtomicFieldInfo(params[0], params[1])
                    } else null
                }
                else -> error("Unsupported type of atomic receiver expression: ${receiver.render()}")
            }
        }

        private val IrDeclaration.parentDeclarationContainer: IrDeclarationContainer
            get() = parents.filterIsInstance<IrDeclarationContainer>().firstOrNull()
                ?: error("In the sequence of parents for ${this.render()} no IrDeclarationContainer was found")

        private val IrFunction.containingFunction: IrFunction
            get() {
                if (this.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return this
                return parents.filterIsInstance<IrFunction>().firstOrNull {
                    it.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                } ?: error("In the sequence of parents for the local function ${this.render()} no containing function was found")
            }

        private fun IrExpression.getArrayElementIndex(parentFunction: IrFunction?): IrExpression =
            when {
                this is IrCall -> getValueArgument(0)!!
                this.isThisReceiver() -> {
                    require(parentFunction != null)
                    parentFunction.valueParameters[2].capture()
                }
                else -> error("Unsupported type of atomic receiver expression: ${this.render()}")
            }

        private fun IrExpression.isThisReceiver() =
            this is IrGetValue && symbol.owner.name.asString() == "<this>"

        private fun IrFunction.isTransformedAtomicExtension(): Boolean {
            val isArrayReceiver = name.asString().endsWith(ATOMIC_ARRAY_RECEIVER_SUFFIX)
            return if (isArrayReceiver) checkSyntheticArrayElementExtensionParameter() else checkSyntheticAtomicExtensionParameters()
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
            val mangledName = mangleFunctionName(functionName, isArrayReceiver)
            val updaterType =
                if (isArrayReceiver) atomicSymbols.getAtomicArrayType(valueType) else atomicSymbols.getFieldUpdaterType(valueType)
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangledName && it.valueParameters[0].type == updaterType
            }?.let { return it }
            return context.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                dispatchReceiverParameter = (parent as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
                if (functionName == LOOP) {
                    if (isArrayReceiver) generateAtomicfuArrayLoop(valueType) else generateAtomicfuLoop(valueType)
                } else {
                    if (isArrayReceiver) generateAtomicfuArrayUpdate(functionName, valueType) else generateAtomicfuUpdate(
                        functionName,
                        valueType
                    )
                }
                this.parent = parent
                parent.declarations.add(this)
            }
        }

        private fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction = findDeclaration {
            it.name.asString() == mangleFunctionName(declaration.name.asString(), isArrayReceiver) &&
                    it.isTransformedAtomicExtension()
        } ?: error("Could not find corresponding transformed declaration for the atomic extension ${declaration.render()}")

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

    private fun getStaticVolatileWrapperInstance(atomicProperty: IrProperty): IrProperty {
        val volatileWrapperClass = atomicProperty.parent as IrClass
        return (volatileWrapperClass.parent as IrDeclarationContainer).declarations.singleOrNull {
            it is IrProperty && it.backingField != null &&
                    it.backingField!!.type.classOrNull == volatileWrapperClass.symbol
        } as? IrProperty
            ?: error("Static instance of ${volatileWrapperClass.name.asString()} is missing in ${volatileWrapperClass.parent}")
    }

    private fun IrType.isKotlinxAtomicfuPackage() =
        classFqName?.let { it.parent().asString() == AFU_PKG } ?: false

    private fun IrSimpleFunctionSymbol.isKotlinxAtomicfuPackage(): Boolean =
        owner.parentClassOrNull?.classId?.let {
            it.packageFqName.asString() == AFU_PKG
        } ?: false

    private fun IrType.isAtomicValueType() =
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

    private fun IrCall.isArrayElementGetter(): Boolean =
        dispatchReceiver?.let {
            it.type.isAtomicArrayType() && symbol.owner.name.asString() == GET
        } ?: false

    private fun IrType.atomicToValueType(): IrType =
        classFqName?.let {
            AFU_VALUE_TYPES[it.shortName().asString()]
        } ?: error("No corresponding value type was found for this atomic type: ${this.render()}")

    private fun IrCall.isAtomicFactory(): Boolean =
        symbol.isKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                type.isAtomicValueType()

    private fun IrFunction.isAtomicExtension(): Boolean =
        extensionReceiverParameter?.let { it.type.isAtomicValueType() && this.isInline } ?: false

    private fun IrStatement.isTraceCall() = this is IrCall && (isTraceInvoke() || isTraceAppend())

    private fun IrCall.isTraceInvoke(): Boolean =
        symbol.isKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == INVOKE &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    private fun IrCall.isTraceAppend(): Boolean =
        symbol.isKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == APPEND &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    private fun getVolatileWrapperClassName(property: IrProperty) =
        property.name.asString().capitalizeAsciiOnly() + '$' +
                (if (property.parent is IrFile) (property.parent as IrFile).name else property.parent.kotlinFqName.asString()).substringBefore('.') +
                VOLATILE_WRAPPER_SUFFIX

    private fun mangleFunctionName(name: String, isArrayReceiver: Boolean) =
        if (isArrayReceiver) "$name$$ATOMICFU$ATOMIC_ARRAY_RECEIVER_SUFFIX" else "$name$$ATOMICFU"
}
