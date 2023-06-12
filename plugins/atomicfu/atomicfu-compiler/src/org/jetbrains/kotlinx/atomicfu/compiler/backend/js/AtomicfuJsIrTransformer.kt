/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.js

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder.buildValueParameter
import org.jetbrains.kotlin.ir.util.IdSignature.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.platform.isJs

private const val AFU_PKG = "kotlinx.atomicfu"
private const val LOCKS = "locks"
private const val AFU_LOCKS_PKG = "$AFU_PKG.$LOCKS"
private const val ATOMICFU_RUNTIME_FUNCTION_PREDICATE = "atomicfu_"
private const val REENTRANT_LOCK_TYPE = "ReentrantLock"
private const val TRACE_BASE_TYPE = "TraceBase"
private const val GETTER = "atomicfu\$getter"
private const val SETTER = "atomicfu\$setter"
private const val GET = "get"
private const val GET_VALUE = "getValue"
private const val SET_VALUE = "setValue"
private const val ATOMIC_VALUE_FACTORY = "atomic"
private const val TRACE = "Trace"
private const val INVOKE = "invoke"
private const val APPEND = "append"
private const val ATOMIC_ARRAY_OF_NULLS_FACTORY = "atomicArrayOfNulls"
private const val REENTRANT_LOCK_FACTORY = "reentrantLock"

class AtomicfuJsIrTransformer(private val context: IrPluginContext) {

    private val irBuiltIns = context.irBuiltIns

    private val AFU_CLASSES: Map<String, IrType> = mapOf(
        "AtomicInt" to irBuiltIns.intType,
        "AtomicLong" to irBuiltIns.longType,
        "AtomicRef" to irBuiltIns.anyNType,
        "AtomicBoolean" to irBuiltIns.booleanType
    )

    private val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")
    private val ATOMIC_ARRAY_TYPES = setOf("AtomicIntArray", "AtomicLongArray", "AtomicBooleanArray", "AtomicArray")
    private val ATOMICFU_INLINE_FUNCTIONS = setOf("atomicfu_loop", "atomicfu_update", "atomicfu_getAndUpdate", "atomicfu_updateAndGet")

    fun transform(irFile: IrFile) {
        if (context.platform.isJs()) {
            irFile.transform(AtomicExtensionTransformer(), null)
            irFile.transformChildren(AtomicTransformer(), null)

            irFile.patchDeclarationParents()
        }
    }

    private inner class AtomicExtensionTransformer : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.declarations.addAllTransformedAtomicExtensions()
            return super.visitFile(declaration)
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.declarations.addAllTransformedAtomicExtensions()
            return super.visitClass(declaration)
        }

        private fun MutableList<IrDeclaration>.addAllTransformedAtomicExtensions() {
            val transformedDeclarations = mutableListOf<IrDeclaration>()
            forEach { irDeclaration ->
                irDeclaration.transformAtomicExtension()?.let { it -> transformedDeclarations.add(it) }
            }
            addAll(transformedDeclarations)
        }

        private fun IrDeclaration.transformAtomicExtension(): IrDeclaration? {
            // Transform the signature of the inline Atomic* extension declaration:
            // inline fun AtomicRef<T>.foo(arg) { ... } -> inline fun <T> foo(arg', atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit)
            if (this is IrFunction && isAtomicExtension()) {
                val newDeclaration = deepCopyWithSymbols(parent)
                val valueParametersCount = valueParameters.size
                val type = newDeclaration.extensionReceiverParameter!!.type.atomicToValueType()
                val getterType = context.buildGetterType(type)
                val setterType = context.buildSetterType(type)
                newDeclaration.valueParameters = newDeclaration.valueParameters + listOf(
                    buildValueParameter(newDeclaration, GETTER, valueParametersCount, getterType),
                    buildValueParameter(newDeclaration, SETTER, valueParametersCount + 1, setterType)
                )
                newDeclaration.extensionReceiverParameter = null
                return newDeclaration
            }
            return null
        }
    }

    private inner class AtomicTransformer : IrElementTransformer<IrFunction?> {

        override fun visitProperty(declaration: IrProperty, data: IrFunction?): IrStatement {
            // Support transformation for delegated properties:
            if (declaration.isDelegated && declaration.backingField?.type?.isAtomicValueType() == true) {
                declaration.backingField?.let { delegateBackingField ->
                    delegateBackingField.initializer?.let {
                        val initializer = it.expression as IrCall
                        when {
                            initializer.isAtomicFieldGetter() -> {
                                // val _a = atomic(0)
                                // var a: Int by _a
                                // Accessors of the delegated property `a` are implemented via the generated property `a$delegate`,
                                // that is the copy of the original `_a`.
                                // They should be delegated to the value of the original field `_a` instead of `a$delegate`.

                                // fun <get-a>() = a$delegate.value -> _a.value
                                // fun <set-a>(value: Int) = { a$delegate.value = value } -> { _a.value = value }
                                val originalField = initializer.getBackingField()
                                declaration.transform(DelegatePropertyTransformer(originalField), null)
                            }
                            initializer.isAtomicFactory() -> {
                                // var a by atomic(77) -> var a: Int = 77
                                it.expression = initializer.eraseAtomicFactory()
                                    ?: error("Atomic factory was expected but found ${initializer.render()}")
                                declaration.transform(DelegatePropertyTransformer(delegateBackingField), null)
                            }
                            else -> error("Unexpected initializer of the delegated property: $initializer")
                        }
                    }
                }
            }
            return super.visitProperty(declaration, data)
        }

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitBlockBody(body: IrBlockBody, data: IrFunction?): IrBody {
            // Erase messages added by the Trace object from the function body:
            // val trace = Trace(size)
            // Messages may be added via trace invocation:
            // trace { "Doing something" }
            // or via multi-append of arguments:
            // trace.append(index, "CAS", value)
            body.statements.removeIf { it.isTrace() }
            return super.visitBlockBody(body, data)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: IrFunction?): IrExpression {
            // Erase messages added by the Trace object from blocks.
            expression.statements.removeIf { it.isTrace() }
            return super.visitContainerExpression(expression, data)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            expression.eraseAtomicFactory()?.let { return it.transform(this, data) }
            val isInline = expression.symbol.owner.isInline
            (expression.extensionReceiver ?: expression.dispatchReceiver)?.transform(this, data)?.let { receiver ->
                // Transform invocations of atomic functions
                if (expression.symbol.isKotlinxAtomicfuPackage() && receiver.type.isAtomicValueType()) {
                    // Substitute invocations of atomic functions on atomic receivers
                    // with the corresponding inline declarations from `kotlinx-atomicfu-runtime`,
                    // passing atomic receiver accessors as atomicfu$getter and atomicfu$setter parameters.

                    // In case of the atomic field receiver, pass field accessors:
                    // a.incrementAndGet() -> atomicfu_incrementAndGet(get_a {..}, set_a {..})

                    // In case of the atomic `this` receiver, pass the corresponding atomicfu$getter and atomicfu$setter parameters
                    // from the parent transformed atomic extension declaration:
                    // Note: inline atomic extension signatures are already transformed with the [AtomicExtensionTransformer]
                    // inline fun foo(atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit) { incrementAndGet() } ->
                    // inline fun foo(atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit) { atomicfu_incrementAndGet(atomicfu$getter, atomicfu$setter) }
                    receiver.getReceiverAccessors(data)?.let { accessors ->
                        val receiverValueType = receiver.type.atomicToValueType()
                        val inlineAtomic = expression.inlineAtomicFunction(receiverValueType, accessors).apply {
                            if (symbol.owner.name.asString() in ATOMICFU_INLINE_FUNCTIONS) {
                                val lambdaLoop = (getValueArgument(0) as IrFunctionExpression).function
                                lambdaLoop.body?.transform(this@AtomicTransformer, data)
                            }
                        }
                        return super.visitCall(inlineAtomic, data)
                    }
                }
                // Transform invocations of atomic extension functions
                if (isInline && receiver.type.isAtomicValueType()) {
                    // Transform invocation of the atomic extension on the atomic receiver,
                    // passing field accessors as atomicfu$getter and atomicfu$setter parameters.

                    // In case of the atomic field receiver, pass field accessors:
                    // a.foo(arg) -> foo(arg, get_a {..}, set_a {..})

                    // In case of the atomic `this` receiver, pass the corresponding atomicfu$getter and atomicfu$setter parameters
                    // from the parent transformed atomic extension declaration:
                    // Note: inline atomic extension signatures are already transformed with the [AtomicExtensionTransformer]
                    // inline fun bar(atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit) { ... }
                    // inline fun foo(atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit) { this.bar() } ->
                    // inline fun foo(atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit) { bar(atomicfu$getter, atomicfu$setter) }
                    receiver.getReceiverAccessors(data)?.let { accessors ->
                        val declaration = expression.symbol.owner
                        val transformedAtomicExtension = getDeclarationWithAccessorParameters(declaration, declaration.extensionReceiverParameter)
                        val irCall = buildCall(
                            expression.startOffset,
                            expression.endOffset,
                            target = transformedAtomicExtension.symbol,
                            type = expression.type,
                            valueArguments = expression.getValueArguments() + accessors
                        ).apply {
                            dispatchReceiver = expression.dispatchReceiver
                        }
                        return super.visitCall(irCall, data)
                    }
                }
            }
            return super.visitCall(expression, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction?): IrExpression {
            // For transformed atomic extension functions:
            // replace all usages of old value parameters with the new parameters of the transformed declaration
            // inline fun foo(arg', atomicfu$getter: () -> T, atomicfu$setter: (T) -> Unit) { bar(arg) } -> { bar(arg') }
            if (expression.symbol is IrValueParameterSymbol) {
                val valueParameter = expression.symbol.owner as IrValueParameter
                val parent = valueParameter.parent
                if (parent is IrFunction && parent.isTransformedAtomicExtensionFunction()) {
                    val index = valueParameter.index
                    if (index >= 0) { // index == -1 for `this` parameter
                        val transformedValueParameter = parent.valueParameters[index]
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

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrFunction?): IrExpression {
            // Erase unchecked casts:
            // val a = atomic<Any>("AAA")
            // (a as AtomicRef<String>).value -> a.value
            if ((expression.operator == CAST || expression.operator == IMPLICIT_CAST) && expression.typeOperand.isAtomicValueType()) {
                return expression.argument
            }
            return super.visitTypeOperator(expression, data)
        }

        override fun visitConstructorCall(expression: IrConstructorCall, data: IrFunction?): IrElement {
            // Erase constructor of Atomic(Int|Long|Boolean|)Array:
            // val arr = AtomicIntArray(size) -> val arr = new Int32Array(size)
            if (expression.isAtomicArrayConstructor()) {
                val arrayConstructorSymbol =
                    context.getArrayConstructorSymbol(expression.type as IrSimpleType) { it.owner.valueParameters.size == 1 }
                val size = expression.getValueArgument(0)
                return IrConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    arrayConstructorSymbol.owner.returnType, arrayConstructorSymbol,
                    arrayConstructorSymbol.owner.typeParameters.size, 0, 1
                ).apply {
                    putValueArgument(0, size)
                }
            }
            return super.visitConstructorCall(expression, data)
        }

        private inner class DelegatePropertyTransformer(
            val originalField: IrField
        ): IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                // Accessors of the delegated property have following signatures:

                // public inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
                // public inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T

                // getValue/setValue should get and set the value of the originalField
                val name = expression.symbol.owner.name.asString()
                if (expression.symbol.isKotlinxAtomicfuPackage() && (name == GET_VALUE || name == SET_VALUE)) {
                    val type = originalField.type.atomicToValueType()
                    val isSetter = name == SET_VALUE
                    val runtimeFunction = getRuntimeFunctionSymbol(name, type)
                    // val _a = atomic(77)
                    // var a: Int by _a
                    // This is the delegate getValue operator of property `a`, which should be transformed to getting the value of the original atomic `_a`
                    // operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) {
                    //  return thisRef._a
                    // }
                    val dispatchReceiver = expression.getValueArgument(0)?.let {
                        if (it.isConstNull()) null else it
                    }
                    val fieldAccessors = listOf(
                        context.buildFieldAccessor(originalField, dispatchReceiver, false),
                        context.buildFieldAccessor(originalField, dispatchReceiver, true)
                    )
                    return buildCall(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        target = runtimeFunction,
                        type = type,
                        typeArguments = if (runtimeFunction.owner.typeParameters.size == 1) listOf(type) else emptyList(),
                        valueArguments = if (isSetter) listOf(expression.getValueArgument(2)!!, fieldAccessors[0], fieldAccessors[1]) else
                            fieldAccessors
                    )
                }
                return super.visitCall(expression)
            }
        }

        private fun IrExpression.getReceiverAccessors(parent: IrFunction?): List<IrExpression>? =
            when {
                this is IrCall -> getAccessors()
                isThisReceiver() -> {
                    if (parent is IrFunction && parent.isTransformedAtomicExtensionFunction()) {
                        parent.valueParameters.takeLast(2).map { it.capture() }
                    } else null
                }
                else -> null
            }

        private fun IrExpression.isThisReceiver() =
            this is IrGetValue && symbol.owner.name.asString() == "<this>"

        private fun IrCall.inlineAtomicFunction(atomicType: IrType, accessors: List<IrExpression>): IrCall {
            val valueArguments = getValueArguments()
            val functionName = getAtomicFunctionName()
            val runtimeFunction = getRuntimeFunctionSymbol(functionName, atomicType)
            return buildCall(
                startOffset, endOffset,
                target = runtimeFunction,
                type = type,
                typeArguments = if (runtimeFunction.owner.typeParameters.size == 1) listOf(atomicType) else emptyList(),
                valueArguments = valueArguments + accessors
            )
        }

        private fun IrFunction.hasReceiverAccessorParameters(): Boolean {
            if (valueParameters.size < 2) return false
            val params = valueParameters.takeLast(2)
            return params[0].name.asString() == GETTER && params[1].name.asString() == SETTER
        }

        private fun IrDeclaration.isTransformedAtomicExtensionFunction(): Boolean =
            this is IrFunction && hasReceiverAccessorParameters()

        private fun getDeclarationWithAccessorParameters(
            declaration: IrFunction,
            extensionReceiverParameter: IrValueParameter?
        ): IrSimpleFunction {
            require(extensionReceiverParameter != null)
            val paramsCount = declaration.valueParameters.size
            val receiverType = extensionReceiverParameter.type.atomicToValueType()
            return (declaration.parent as? IrDeclarationContainer)?.let { parent ->
                parent.declarations.singleOrNull {
                    it is IrSimpleFunction &&
                            it.name == declaration.symbol.owner.name &&
                            it.valueParameters.size == paramsCount + 2 &&
                            it.valueParameters.dropLast(2).withIndex()
                                .all { p -> p.value.render() == declaration.valueParameters[p.index].render() } &&
                            it.valueParameters[paramsCount].name.asString() == GETTER && it.valueParameters[paramsCount + 1].name.asString() == SETTER &&
                            it.getGetterReturnType()?.render() == receiverType.render()
                } as? IrSimpleFunction
            } ?: error(
                "Failed to find the transformed atomic extension function with accessor parameters " +
                        "corresponding to the original declaration: ${declaration.render()} in the parent: ${declaration.parent.render()}"
            )
        }

        private fun IrCall.isArrayElementGetter(): Boolean =
            dispatchReceiver?.let {
                it.type.isAtomicArrayType() && symbol.owner.name.asString() == GET
            } ?: false

        private fun IrCall.getAccessors(): List<IrExpression> =
            if (!isArrayElementGetter()) {
                val field = getBackingField()
                listOf(
                    context.buildFieldAccessor(field, dispatchReceiver, false),
                    context.buildFieldAccessor(field, dispatchReceiver, true)
                )
            } else {
                val index = getValueArgument(0)!!
                val arrayGetter = dispatchReceiver as IrCall
                val arrayField = arrayGetter.getBackingField()
                listOf(
                    context.buildArrayElementAccessor(arrayField, arrayGetter, index, false),
                    context.buildArrayElementAccessor(arrayField, arrayGetter, index, true)
                )
            }

        private fun IrStatement.isTrace() =
            this is IrCall && (isTraceInvoke() || isTraceAppend())

        private fun IrCall.isTraceInvoke(): Boolean =
            symbol.isKotlinxAtomicfuPackage() &&
                    symbol.owner.name.asString() == INVOKE &&
                    symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

        private fun IrCall.isTraceAppend(): Boolean =
            symbol.isKotlinxAtomicfuPackage() &&
                    symbol.owner.name.asString() == APPEND &&
                    symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true


        private fun getRuntimeFunctionSymbol(name: String, type: IrType): IrSimpleFunctionSymbol {
            val functionName = when (name) {
                "value.<get-value>" -> "getValue"
                "value.<set-value>" -> "setValue"
                else -> name
            }
            return context.referencePackageFunction(AFU_PKG, "$ATOMICFU_RUNTIME_FUNCTION_PREDICATE$functionName") {
                val typeArg = it.owner.getGetterReturnType()
                !(typeArg as IrType).isPrimitiveType() || typeArg == type
            }
        }

        private fun IrFunction.getGetterReturnType(): IrType? =
            valueParameters.getOrNull(valueParameters.lastIndex - 1)?.let { getter ->
                if (getter.name.asString() == GETTER) {
                    (getter.type as IrSimpleType).arguments.first().typeOrNull
                } else null
            }

        private fun IrCall.getAtomicFunctionName(): String =
            symbol.signature?.let { signature ->
                signature.getDeclarationNameBySignature()?.let { name ->
                    if (name.substringBefore('.') in ATOMIC_VALUE_TYPES) {
                        name.substringAfter('.')
                    } else name
                }
            } ?: error("Incorrect pattern of the atomic function name: ${symbol.owner.render()}")

        private fun IrCall.eraseAtomicFactory() =
            when {
                isAtomicFactory() -> getValueArgument(0) ?: error("Atomic factory should take at least one argument: ${this.render()}")
                isAtomicArrayFactory() -> buildObjectArray()
                isReentrantLockFactory() -> context.buildConstNull()
                isTraceFactory() -> context.buildConstNull()
                else -> null
            }

        private fun IrCall.buildObjectArray(): IrCall {
            val arrayFactorySymbol = context.referencePackageFunction("kotlin", "arrayOfNulls")
            val arrayElementType = getTypeArgument(0) ?: error("AtomicArray factory should have a type argument: ${symbol.owner.render()}")
            val size = getValueArgument(0)
            return buildCall(
                startOffset, endOffset,
                target = arrayFactorySymbol,
                type = type,
                typeArguments = listOf(arrayElementType),
                valueArguments = listOf(size)
            )
        }
    }

    private fun IrFunction.isAtomicExtension(): Boolean =
        extensionReceiverParameter?.let { it.type.isAtomicValueType() && this.isInline } ?: false

    private fun IrSymbol.isKotlinxAtomicfuPackage() =
        this.isPublicApi && signature?.packageFqName()?.asString() == AFU_PKG

    private fun IrType.isAtomicValueType() = belongsTo(AFU_PKG, ATOMIC_VALUE_TYPES)
    private fun IrType.isAtomicArrayType() = belongsTo(AFU_PKG, ATOMIC_ARRAY_TYPES)
    private fun IrType.isReentrantLockType() = belongsTo(AFU_LOCKS_PKG, REENTRANT_LOCK_TYPE)
    private fun IrType.isTraceBaseType() = belongsTo(AFU_PKG, TRACE_BASE_TYPE)

    private fun IrType.belongsTo(packageName: String, typeNames: Set<String>) =
        getSignature()?.let { sig ->
            sig.packageFqName == packageName && sig.declarationFqName in typeNames
        } ?: false

    private fun IrType.belongsTo(packageName: String, typeName: String) =
        getSignature()?.let { sig ->
            sig.packageFqName == packageName && sig.declarationFqName == typeName
        } ?: false

    private fun IrType.getSignature(): CommonSignature? = classOrNull?.let { it.signature?.asPublic() }

    private fun IrType.atomicToValueType(): IrType {
        require(this is IrSimpleType)
        return classifier.signature?.asPublic()?.declarationFqName?.let { classId ->
            if (classId == "AtomicRef")
                arguments.first().typeOrNull ?: error("$AFU_PKG.AtomicRef type parameter is not IrTypeProjection")
            else
                AFU_CLASSES[classId] ?: error("IrType ${this.getClass()} does not match any of atomicfu types")
        } ?: error("Unexpected signature of the atomic type: ${this.render()}")
    }

    private fun IrCall.isAtomicFactory(): Boolean =
        symbol.isKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                type.isAtomicValueType()

    private fun IrCall.isTraceFactory(): Boolean =
        symbol.isKotlinxAtomicfuPackage() && symbol.owner.name.asString() == TRACE &&
                type.isTraceBaseType()

    private fun IrCall.isAtomicArrayFactory(): Boolean =
        symbol.isKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_ARRAY_OF_NULLS_FACTORY &&
                type.isAtomicArrayType()

    private fun IrCall.isAtomicFieldGetter(): Boolean =
        type.isAtomicValueType() && symbol.owner.name.asString().startsWith("<get-")

    private fun IrConstructorCall.isAtomicArrayConstructor(): Boolean = type.isAtomicArrayType()

    private fun IrCall.isReentrantLockFactory(): Boolean =
        symbol.owner.name.asString() == REENTRANT_LOCK_FACTORY && type.isReentrantLockType()
}
