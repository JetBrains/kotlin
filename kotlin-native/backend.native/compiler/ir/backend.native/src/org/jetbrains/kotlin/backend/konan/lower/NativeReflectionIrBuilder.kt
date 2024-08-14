/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.irConstantArray
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.Variance

internal fun IrBuilderWithScope.toNativeConstantReflectionBuilder(symbols: KonanSymbols, onRecursiveUpperBound: IrBuilderWithScope.(String) -> Unit = {}) = NativeConstantReflectionIrBuilder(
        context, scope, startOffset, endOffset, symbols, onRecursiveUpperBound
)

internal fun IrBuilderWithScope.toNativeRuntimeReflectionBuilder(symbols: KonanSymbols, onRecursiveUpperBound: IrBuilderWithScope.(String) -> Unit = {}) = NativeRuntimeReflectionIrBuilder(
        context, scope, startOffset, endOffset, symbols, onRecursiveUpperBound
)

internal class NativeRuntimeReflectionIrBuilder(
        context: IrGeneratorContext,
        scope: Scope,
        startOffset: Int, endOffset: Int,
        symbols: KonanSymbols,
        onRecursiveUpperBound: IrBuilderWithScope.(String) -> Unit,
) : NativeReflectionIrBuilderBase<IrExpression>(context, scope, startOffset, endOffset, symbols, onRecursiveUpperBound) {
    override fun irKClass(symbol: IrClassSymbol): IrExpression {
        val kClassType = symbols.kClassImpl.typeWith(symbol.defaultType)
        return IrClassReferenceImpl(startOffset, endOffset, kClassType, symbol, kClassType)
    }

    override fun irKTypeOfReified(type: IrType): IrExpression {
        return irCall(symbols.typeOf).apply {
            putTypeArgument(0, type)
        }
    }

    override fun irCreateInstance(
            clazz: IrClass,
            elements: Map<String, IrExpression>,
            typeArguments: List<IrType>
    ): IrExpression {
        val constructor = clazz.primaryConstructor!!.symbol.owner
        val arguments = constructor.valueParameters.also {
            require(it.size == elements.size) {
                "Wrong number of values provided for ${clazz.name} construction: ${elements.size} instead of ${it.size}"
            }
        }.map {
            elements[it.name.asString()] ?: error("No value for field named ${it.name} provided")
        }
        return irCallConstructor(constructor.symbol, typeArguments).apply {
            for ((index, i) in arguments.withIndex()) {
                putValueArgument(index, i)
            }
        }
    }

    override fun irConstantNull() = irNull()
    override fun irConstantString(string: String) = irString(string)
    override fun irConstantInt(int: Int) = irInt(int)
    override fun irConstantBoolean(boolean: Boolean) = irBoolean(boolean)

    override fun irCreateArray(elementType: IrType, values: List<IrExpression>): IrExpression {
        val arrayType = symbols.irBuiltIns.primitiveArrayForType[elementType]?.defaultType ?: symbols.array.typeWith(elementType)
        return IrVarargImpl(startOffset, endOffset, arrayType, elementType, values)
    }
}


// these constants are copy-pasted from KVarianceMapper.Companion in KTypeImpl.kt
private fun mapVariance(variance: Variance?) = when (variance) {
    null -> -1
    Variance.INVARIANT -> 0
    Variance.IN_VARIANCE -> 1
    Variance.OUT_VARIANCE -> 2
}

internal class NativeConstantReflectionIrBuilder(
        context: IrGeneratorContext,
        scope: Scope,
        startOffset: Int, endOffset: Int,
        symbols: KonanSymbols,
        onRecursiveUpperBound: IrBuilderWithScope.(String) -> Unit,
) : NativeReflectionIrBuilderBase<IrConstantValue>(context, scope, startOffset, endOffset, symbols, onRecursiveUpperBound) {

    override fun irKTypeOfReified(type: IrType): IrConstantValue = irConstantObject(symbols.kTypeImplIntrinsicConstructor, emptyList(), listOf(type))

    private fun irKClassUnsupported(symbols: KonanSymbols, message: String) =
            irConstantObject(symbols.kClassUnsupportedImpl.owner, mapOf(
                    "message" to irConstantString(message)
            ))

    override fun irConstantNull() = irConstantPrimitive(irNull())
    override fun irConstantString(string: String) = irConstantPrimitive(irString(string))
    override fun irConstantInt(int: Int) = irConstantPrimitive(irInt(int))
    override fun irConstantBoolean(boolean: Boolean) = irConstantPrimitive(irBoolean(boolean))

    override fun irKClass(symbol: IrClassSymbol): IrConstantValue {
        fun IrClass.isNativePointedChild(): Boolean =
                this.symbol == symbols.nativePointed || getSuperClassNotAny()?.isNativePointedChild() == true

        return when {
            symbol.owner.isExternalObjCClass() ->
                if (symbol.owner.isInterface)
                    irKClassUnsupported(symbols, "KClass for Objective-C protocols is not supported yet")
                else
                    irConstantObject(symbols.kObjCClassImplIntrinsicConstructor, emptyList(), listOf(symbol.starProjectedType))

            symbol.owner.isObjCClass() ->
                irKClassUnsupported(symbols, "KClass for Kotlin subclasses of Objective-C classes is not supported yet")

            symbol.owner.isNativePointedChild() ->
                irKClassUnsupported(symbols, "KClass for interop types is not supported yet")

            else -> irConstantObject(symbols.kClassImplIntrinsicConstructor, emptyList(), listOf(symbol.starProjectedType))
        }
    }

    override fun irCreateInstance(clazz: IrClass, elements: Map<String, IrConstantValue>, typeArguments: List<IrType>): IrConstantValue {
        return irConstantObject(clazz, elements, typeArguments)
    }

    override fun irCreateArray(elementType: IrType, values: List<IrConstantValue>): IrConstantValue {
        val arrayType = symbols.irBuiltIns.primitiveArrayForType[elementType]?.defaultType ?: symbols.array.typeWith(elementType)
        return irConstantArray(arrayType, values)
    }
}


internal abstract class NativeReflectionIrBuilderBase<E: IrExpression>(
        context: IrGeneratorContext,
        scope: Scope,
        startOffset: Int, endOffset: Int,
        val symbols: KonanSymbols,
        val onRecursiveUpperBound: IrBuilderWithScope.(String) -> Unit,
) : IrBuilderWithScope(context, scope, startOffset, endOffset) {

    fun irKType(type: IrType, leaveReifiedForLater: Boolean = false) : E =
            irKType(type, leaveReifiedForLater, mutableSetOf())

    private class RecursiveBoundsException(message: String) : Throwable(message)

    private fun irKType(
            type: IrType,
            leaveReifiedForLater: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): E {
        if (type !is IrSimpleType) {
            // Represent as non-denotable type:
            return irKTypeImpl(
                    kClassifier = null,
                    irTypeArguments = emptyList(),
                    isMarkedNullable = false,
                    type = type,
            )
        }
        try {
            val kClassifier = when (val classifier = type.classifier) {
                is IrClassSymbol -> irKClass(classifier)
                is IrTypeParameterSymbol -> {
                    if (classifier.owner.isReified && leaveReifiedForLater) {
                        // Leave as is for reification.
                        return irKTypeOfReified(type)
                    }

                    // Leave upper bounds of non-reified type parameters as is, even if they are reified themselves.
                    irKTypeParameter(classifier.owner, seenTypeParameters = seenTypeParameters)
                }
                is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
            }

            return irKTypeImpl(
                    kClassifier = kClassifier,
                    irTypeArguments = type.arguments.map {
                        when (it) {
                            is IrStarProjection -> null
                            is IrTypeProjection -> it.variance to irKType(it.type, leaveReifiedForLater, seenTypeParameters)
                        }
                    },
                    isMarkedNullable = type.isMarkedNullable(),
                    type = type,
            )
        } catch (t: RecursiveBoundsException) {
            onRecursiveUpperBound(t.message!!)
            return irKTypeForTypeParametersWithRecursiveBounds()
        }
    }


    private fun irKTypeParameter(
            typeParameter: IrTypeParameter,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): E {
        if (!seenTypeParameters.add(typeParameter))
            throw RecursiveBoundsException("Non-reified type parameters with recursive bounds are not supported yet: ${typeParameter.render()}")
        val upperBounds = typeParameter.superTypes.map { irKType(it, false, seenTypeParameters) }
        seenTypeParameters.remove(typeParameter)

        return kTypeParameterImpl(typeParameter, upperBounds)
    }

    fun kTypeParameterImpl(
            typeParameter: IrTypeParameter,
            upperBounds: List<E>
    ): E = irCreateInstance(symbols.kTypeParameterImpl.owner, mapOf(
            "name" to irConstantString(typeParameter.name.asString()),
            "containerFqName" to irConstantString(typeParameter.parentUniqueName),
            "upperBoundsArray" to irCreateArray(symbols.array.typeWith(symbols.kType.defaultType), upperBounds),
            "varianceId" to irConstantInt(mapVariance(typeParameter.variance)),
            "isReified" to irConstantBoolean(typeParameter.isReified),
    ))


    private fun irKTypeImpl(
            kClassifier: E?,
            irTypeArguments: List<Pair<Variance, E>?>,
            isMarkedNullable: Boolean,
            type: IrType,
    ): E = irCreateInstance(symbols.kTypeImpl.owner, mapOf(
            "classifier" to (kClassifier ?: irConstantNull()),
            "arguments" to irKTypeProjectionsList(irTypeArguments),
            "isMarkedNullable" to irConstantBoolean(isMarkedNullable),
    ), listOf(type))

    private fun irKTypeProjectionsList(
            irTypeArguments: List<Pair<Variance, E>?>,
    ): E {
        val variance = irCreateArray(
                symbols.irBuiltIns.intType,
                irTypeArguments.map { irConstantInt(mapVariance(it?.first)) }
        )
        val type = irCreateArray(
                symbols.kType.defaultType.makeNullable(),
                irTypeArguments.map { it?.second ?: irConstantNull() }
        )
        return irCreateInstance(
                symbols.kTypeProjectionList.owner,
                mapOf(
                        "variance" to variance,
                        "type" to type
                ))
    }

    private val IrTypeParameter.parentUniqueName
        get() = when (val parent = parent) {
            is IrFunction -> parent.computeFullName()
            else -> parent.fqNameForIrSerialization.asString()
        }


    private fun irKTypeForTypeParametersWithRecursiveBounds() : E {
        return irCreateInstance(symbols.kTypeImplForTypeParametersWithRecursiveBounds.owner, emptyMap())
    }

    protected abstract fun irCreateInstance(
            clazz: IrClass,
            elements: Map<String, E>,
            typeArguments: List<IrType> = emptyList()
    ): E

    abstract fun irKClass(symbol: IrClassSymbol): E
    protected abstract fun irKTypeOfReified(type: IrType): E
    protected abstract fun irCreateArray(elementType: IrType, values: List<E>): E
    protected abstract fun irConstantNull(): E
    protected abstract fun irConstantString(string: String): E
    protected abstract fun irConstantInt(int: Int): E
    protected abstract fun irConstantBoolean(boolean: Boolean): E

}