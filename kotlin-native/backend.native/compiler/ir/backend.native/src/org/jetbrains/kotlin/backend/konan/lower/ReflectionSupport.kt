/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.Variance

private fun IrBuilderWithScope.irConstantString(string: String) = irConstantPrimitive(irString(string))
private fun IrBuilderWithScope.irConstantInt(int: Int) = irConstantPrimitive(irInt(int))
private fun IrBuilderWithScope.irConstantBoolean(boolean: Boolean) = irConstantPrimitive(irBoolean(boolean))

internal class KTypeGenerator(
        val context: KonanBackendContext,
        val irFile: IrFile,
        val irElement: IrElement,
        val needExactTypeParameters: Boolean = false
) {
    private val symbols = context.ir.symbols

    fun IrBuilderWithScope.irKType(type: IrType, leaveReifiedForLater: Boolean = false) =
            irKType(type, leaveReifiedForLater, mutableSetOf())

    private class RecursiveBoundsException(message: String) : Throwable(message)

    private fun IrBuilderWithScope.irKType(
            type: IrType,
            leaveReifiedForLater: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): IrConstantValue {
        if (type !is IrSimpleType) {
            // Represent as non-denotable type:
            return irKTypeImpl(
                kClassifier = irConstantPrimitive(irNull()),
                irTypeArguments = emptyList(),
                isMarkedNullable = false,
                leaveReifiedForLater = leaveReifiedForLater,
                seenTypeParameters = seenTypeParameters,
                type = type,
            )
        }
        try {
            val kClassifier = when (val classifier = type.classifier) {
                is IrClassSymbol -> irKClass(classifier)
                is IrTypeParameterSymbol -> {
                    if (classifier.owner.isReified && leaveReifiedForLater) {
                        // Leave as is for reification.
                        return irConstantObject(symbols.kTypeImplIntrinsicConstructor, emptyList(), listOf(type))
                    }

                    // Leave upper bounds of non-reified type parameters as is, even if they are reified themselves.
                    irKTypeParameter(classifier.owner, leaveReifiedForLater = false, seenTypeParameters = seenTypeParameters)
                }
                is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
            }

            return irKTypeImpl(
                    kClassifier = kClassifier,
                    irTypeArguments = type.arguments,
                    isMarkedNullable = type.isMarkedNullable(),
                    leaveReifiedForLater = leaveReifiedForLater,
                    seenTypeParameters = seenTypeParameters,
                    type = type,
            )
        } catch (t: RecursiveBoundsException) {
            if (needExactTypeParameters)
                this@KTypeGenerator.context.reportCompilationError(t.message!!, irFile, irElement)
            return irConstantObject(symbols.kTypeImplForTypeParametersWithRecursiveBounds.owner, emptyMap())
        }
    }

    private fun IrBuilderWithScope.irKTypeImpl(
        kClassifier: IrConstantValue,
        irTypeArguments: List<IrTypeArgument>,
        isMarkedNullable: Boolean,
        leaveReifiedForLater: Boolean,
        seenTypeParameters: MutableSet<IrTypeParameter>,
        type: IrType,
    ): IrConstantValue = irConstantObject(symbols.kTypeImpl.owner, mapOf(
        "classifier" to kClassifier,
        "arguments" to irKTypeProjectionsList(irTypeArguments, leaveReifiedForLater, seenTypeParameters),
        "isMarkedNullable" to irConstantPrimitive(irBoolean(isMarkedNullable)),
    ), listOf(type))

    private fun IrBuilderWithScope.irKClass(symbol: IrClassSymbol) = irKClass(this@KTypeGenerator.context, symbol)

    private fun IrBuilderWithScope.irKTypeParameter(
            typeParameter: IrTypeParameter,
            leaveReifiedForLater: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): IrConstantValue {
        if (!seenTypeParameters.add(typeParameter))
            throw RecursiveBoundsException("Non-reified type parameters with recursive bounds are not supported yet: ${typeParameter.render()}")
        val result = irConstantObject(symbols.kTypeParameterImpl.owner, mapOf(
                "name" to irConstantString(typeParameter.name.asString()),
                "containerFqName" to irConstantString(typeParameter.parentUniqueName),
                "upperBoundsArray" to irKTypeArray(typeParameter.superTypes, leaveReifiedForLater, seenTypeParameters),
                "varianceId" to irConstantInt(mapVariance(typeParameter.variance)),
                "isReified" to irConstantBoolean(typeParameter.isReified),
        ))
        seenTypeParameters.remove(typeParameter)
        return result
    }

    private val IrTypeParameter.parentUniqueName
        get() = when (val parent = parent) {
            is IrFunction -> parent.computeFullName()
            else -> parent.fqNameForIrSerialization.asString()
        }

    private fun IrBuilderWithScope.irKTypeArray(
            types: List<IrType>,
            leaveReifiedForLater: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): IrConstantValue {
        val itemType = symbols.kType.defaultType
        return irConstantArray(symbols.array.typeWith(itemType),
                types.map { irKType(it, leaveReifiedForLater, seenTypeParameters) }
        )
    }

    // this constants are copypasted from KVarianceMapper.Companion in KTypeImpl.kt
    private fun mapVariance(variance: Variance) = when (variance) {
        Variance.INVARIANT -> 0
        Variance.IN_VARIANCE -> 1
        Variance.OUT_VARIANCE -> 2
    }

    private fun IrBuilderWithScope.irKTypeProjectionsList(
            irTypeArguments: List<IrTypeArgument>,
            leaveReifiedForLater: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): IrConstantValue {
        val variance = irConstantArray(
                symbols.intArrayType,
                irTypeArguments.map { argument ->
                    when (argument) {
                        is IrStarProjection -> irConstantInt(-1)
                        is IrTypeProjection -> irConstantInt(mapVariance(argument.variance))
                    }
                })
        val type = irConstantArray(
                symbols.array.typeWith(symbols.kType.defaultType.makeNullable()),
                irTypeArguments.map { argument ->
                    when (argument) {
                        is IrStarProjection -> irConstantPrimitive(irNull())
                        is IrTypeProjection -> irKType(argument.type, leaveReifiedForLater, seenTypeParameters)
                    }
                })
        return irConstantObject(
                symbols.kTypeProjectionList.owner,
                mapOf(
                        "variance" to variance,
                        "type" to type
                ))
    }
}

internal fun IrBuilderWithScope.irKClass(context: KonanBackendContext, symbol: IrClassSymbol): IrConstantValue {
    val symbols = context.ir.symbols

    fun IrClass.isNativePointedChild() : Boolean =
            this.symbol == context.ir.symbols.nativePointed || getSuperClassNotAny()?.isNativePointedChild() == true

    return when {
        symbol.owner.isObjCClass() ->
            irKClassUnsupported(context, "KClass for Objective-C classes is not supported yet")

        symbol.owner.isNativePointedChild() ->
            irKClassUnsupported(context, "KClass for interop types is not supported yet")

        else -> irConstantObject(symbols.kClassImplIntrinsicConstructor, emptyList(), listOf(symbol.starProjectedType))
    }
}

private fun IrBuilderWithScope.irKClassUnsupported(context: KonanBackendContext, message: String) =
        irConstantObject(context.ir.symbols.kClassUnsupportedImpl.owner, mapOf(
                "message" to irConstantString(message)
        ))
