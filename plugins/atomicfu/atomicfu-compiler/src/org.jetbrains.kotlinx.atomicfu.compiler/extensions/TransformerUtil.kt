/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.extensions

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

private const val KOTLIN = "kotlin"
private const val GET = "get"
private const val SET = "set"

private val AFU_ARRAY_CLASSES: Map<String, String> = mapOf(
    "AtomicIntArray" to "IntArray",
    "AtomicLongArray" to "LongArray",
    "AtomicBooleanArray" to "BooleanArray",
    "AtomicArray" to "Array"
)

internal fun buildCall(
    startOffset: Int,
    endOffset: Int,
    target: IrSimpleFunctionSymbol,
    type: IrType? = null,
    origin: IrStatementOrigin? = null,
    typeArguments: List<IrType> = emptyList(),
    valueArguments: List<IrExpression?> = emptyList()
): IrCall =
    IrCallImpl(
        startOffset,
        endOffset,
        type ?: target.owner.returnType,
        target,
        typeArguments.size,
        valueArguments.size,
        origin
    ).apply {
        typeArguments.let {
            it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
        }
        valueArguments.let {
            it.withIndex().forEach { (i, arg) -> putValueArgument(i, arg) }
        }
    }

internal fun IrFactory.buildBlockBody(statements: List<IrStatement>) =
    createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

internal fun buildSetField(
    symbol: IrFieldSymbol,
    receiver: IrExpression?,
    value: IrExpression,
    superQualifierSymbol: IrClassSymbol? = null
): IrSetField =
    IrSetFieldImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        symbol,
        receiver,
        value,
        value.type,
        IrStatementOrigin.GET_PROPERTY,
        superQualifierSymbol
    )

internal fun buildGetField(
    symbol: IrFieldSymbol,
    receiver: IrExpression?,
    superQualifierSymbol: IrClassSymbol? = null,
    type: IrType? = null
): IrGetField =
    IrGetFieldImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        symbol,
        type ?: symbol.owner.type,
        receiver,
        IrStatementOrigin.GET_PROPERTY,
        superQualifierSymbol
    )

internal fun buildFunctionSimpleType(
    symbol: IrClassifierSymbol,
    typeParameters: List<IrType>
): IrSimpleType =
    IrSimpleTypeImpl(
        classifier = symbol,
        hasQuestionMark = false,
        arguments = typeParameters.map { makeTypeProjection(it, Variance.INVARIANT) },
        annotations = emptyList()
    )

internal fun buildGetValue(
    startOffset: Int,
    endOffset: Int,
    symbol: IrValueSymbol
): IrGetValue =
    IrGetValueImpl(
        startOffset,
        endOffset,
        symbol.owner.type,
        symbol
    )

internal fun IrPluginContext.buildConstNull() = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.anyNType)

internal fun getterName(getterCall: IrCall) = "<get-${getterCall.symbol.owner.name.getFieldName()}>"
internal fun setterName(getterCall: IrCall) = "<set-${getterCall.symbol.owner.name.getFieldName()}>"

private fun Name.getFieldName() = "<get-(\\w+)>".toRegex().find(asString())?.groupValues?.get(1)
    ?: error("Getter name ${this.asString()} does not match special name pattern <get-fieldName>")

internal fun IrFunctionAccessExpression.getValueArguments() =
    (0 until valueArgumentsCount).map { i ->
        getValueArgument(i)
    }

internal fun IrValueParameter.capture() = buildGetValue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol)

internal fun IrPluginContext.buildGetterType(valueType: IrType): IrSimpleType =
    buildFunctionSimpleType(
        irBuiltIns.functionN(0).symbol,
        listOf(valueType)
    )

internal fun IrPluginContext.buildSetterType(valueType: IrType): IrSimpleType =
    buildFunctionSimpleType(
        irBuiltIns.functionN(1).symbol,
        listOf(valueType, irBuiltIns.unitType)
    )

private fun buildSetField(backingField: IrField, ownerClass: IrExpression?, value: IrGetValue): IrSetField {
    val receiver = if (ownerClass is IrTypeOperatorCall) ownerClass.argument as IrGetValue else ownerClass
    return buildSetField(
        symbol = backingField.symbol,
        receiver = receiver,
        value = value
    )
}

private fun buildGetField(backingField: IrField, ownerClass: IrExpression?): IrGetField {
    val receiver = if (ownerClass is IrTypeOperatorCall) ownerClass.argument as IrGetValue else ownerClass
    return buildGetField(
        symbol = backingField.symbol,
        receiver = receiver
    )
}

internal fun IrPluginContext.buildAccessorLambda(
    getter: IrCall,
    valueType: IrType,
    isSetter: Boolean,
    isArrayElement: Boolean
): IrExpression {
    val getterCall = if (isArrayElement) getter.dispatchReceiver as IrCall else getter
    val type = if (isSetter) buildSetterType(valueType) else buildGetterType(valueType)
    val name = if (isSetter) setterName(getterCall) else getterName(getterCall)
    val returnType = if (isSetter) irBuiltIns.unitType else valueType
    val accessorFunction = irFactory.buildFun {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
        this.origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        this.name = Name.identifier(name)
        this.visibility = DescriptorVisibilities.LOCAL
        this.isInline = true
        this.returnType = returnType
    }.apply {
        val valueParameter = JsIrBuilder.buildValueParameter(this, name, 0, valueType)
        this.valueParameters = if (isSetter) listOf(valueParameter) else emptyList()
        val body = if (isSetter) {
            if (isArrayElement) {
                val setSymbol = referenceFunction(referenceArrayClass(getterCall.type as IrSimpleType), SET)
                val elementIndex = getter.getValueArgument(0)!!.deepCopyWithVariables()
                buildCall(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    target = setSymbol,
                    type = irBuiltIns.unitType,
                    origin = IrStatementOrigin.LAMBDA,
                    valueArguments = listOf(elementIndex, valueParameter.capture())
                ).apply {
                    dispatchReceiver = getterCall
                }
            } else {
                buildSetField(getterCall.getBackingField(), getterCall.dispatchReceiver, valueParameter.capture())
            }
        } else {
            val getField = buildGetField(getterCall.getBackingField(), getterCall.dispatchReceiver)
            if (isArrayElement) {
                val getSymbol = referenceFunction(referenceArrayClass(getterCall.type as IrSimpleType), GET)
                val elementIndex = getter.getValueArgument(0)!!.deepCopyWithVariables()
                buildCall(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    target = getSymbol,
                    type = valueType,
                    origin = IrStatementOrigin.LAMBDA,
                    valueArguments = listOf(elementIndex)
                ).apply {
                    dispatchReceiver = getField.deepCopyWithVariables()
                }
            } else {
                getField.deepCopyWithVariables()
            }
        }
        this.body = irFactory.buildBlockBody(listOf(body))
        origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
    }
    return IrFunctionExpressionImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        type,
        accessorFunction,
        IrStatementOrigin.LAMBDA
    )
}

private fun IrCall.getBackingField(): IrField {
    val correspondingPropertySymbol = symbol.owner.correspondingPropertySymbol!!
    return correspondingPropertySymbol.owner.backingField!!
}

internal fun IrPluginContext.referencePackageFunction(
    packageName: String,
    name: String,
    predicate: (IrFunctionSymbol) -> Boolean = { true }
) = try {
        referenceFunctions(FqName("$packageName.$name")).single(predicate)
    } catch (e: RuntimeException) {
        error("Exception while looking for the function `$name` in package `$packageName`: ${e.message}")
    }

internal fun IrPluginContext.referenceFunction(classSymbol: IrClassSymbol, functionName: String): IrSimpleFunctionSymbol {
    val functionId = FqName("$KOTLIN.${classSymbol.owner.name}.$functionName")
    return try {
        referenceFunctions(functionId).single()
    } catch (e: RuntimeException) {
        error("Exception while looking for the function `$functionId`: ${e.message}")
    }
}

private fun IrPluginContext.referenceArrayClass(irType: IrSimpleType): IrClassSymbol {
    val afuClassId = (irType.classifier.signature!!.asPublic())!!.declarationFqName
    val classId = FqName("$KOTLIN.${AFU_ARRAY_CLASSES[afuClassId]!!}")
    return referenceClass(classId)!!
}

internal fun IrPluginContext.getArrayConstructorSymbol(irType: IrSimpleType, predicate: (IrConstructorSymbol) -> Boolean = { true }): IrConstructorSymbol {
    val afuClassId = (irType.classifier.signature!!.asPublic())!!.declarationFqName
    val classId = FqName("$KOTLIN.${AFU_ARRAY_CLASSES[afuClassId]!!}")
    return referenceConstructors(classId).single(predicate)
}
