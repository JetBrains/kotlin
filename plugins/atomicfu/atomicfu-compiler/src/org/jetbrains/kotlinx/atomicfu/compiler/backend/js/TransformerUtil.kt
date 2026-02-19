/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.js

import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder.buildValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

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
    arguments: List<IrExpression?> = emptyList()
): IrCall =
    IrCallImpl(
        startOffset,
        endOffset,
        type ?: target.owner.returnType,
        target,
        typeArguments.size,
        origin
    ).apply {
        this.typeArguments.assignFrom(typeArguments)
        this.arguments.assignFrom(arguments)
    }

internal fun IrFactory.buildBlockBody(statements: List<IrStatement>) =
    createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

internal fun IrPluginContext.buildSetField(
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
        irBuiltIns.unitType,
        superQualifierSymbol = superQualifierSymbol
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
        superQualifierSymbol = superQualifierSymbol
    )

internal fun buildSimpleType(
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

internal fun IrExpression.isConstNull() = this is IrConst && this.kind.asString == "Null"

internal fun IrField.getterName() = "<get-${name.asString()}>"
internal fun IrField.setterName() = "<set-${name.asString()}>"

internal fun IrValueParameter.capture() = buildGetValue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol)

internal fun IrPluginContext.buildGetterType(valueType: IrType): IrSimpleType =
    buildSimpleType(
        irBuiltIns.functionN(0).symbol,
        listOf(valueType)
    )

internal fun IrPluginContext.buildSetterType(valueType: IrType): IrSimpleType =
    buildSimpleType(
        irBuiltIns.functionN(1).symbol,
        listOf(valueType, irBuiltIns.unitType)
    )

private fun IrPluginContext.buildSetField(backingField: IrField, ownerClass: IrExpression?, value: IrGetValue): IrSetField {
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

private fun IrPluginContext.buildDefaultPropertyAccessor(name: String): IrSimpleFunction =
    irFactory.buildFun {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
        this.origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        this.visibility = DescriptorVisibilities.LOCAL
        this.isInline = true
        this.name = Name.identifier(name)
    }

internal fun IrPluginContext.buildArrayElementAccessor(
    arrayField: IrField,
    arrayGetter: IrCall,
    index: IrExpression,
    isSetter: Boolean
): IrExpression {
    val valueType = arrayField.type
    val functionType = if (isSetter) buildSetterType(valueType) else buildGetterType(valueType)
    val returnType = if (isSetter) irBuiltIns.unitType else valueType
    val name = if (isSetter) arrayField.setterName() else arrayField.getterName()
    val accessorFunction = buildDefaultPropertyAccessor(name).apply {
        val valueParameter = buildValueParameter(this, name, valueType)
        this.parameters = if (isSetter) listOf(valueParameter) else emptyList()
        body = irFactory.buildBlockBody(
            listOf(
                if (isSetter) {
                    val setSymbol = referenceFunction(referenceArrayClass(arrayField.type as IrSimpleType), SET)
                    buildCall(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        target = setSymbol,
                        type = irBuiltIns.unitType,
                        origin = IrStatementOrigin.LAMBDA,
                        arguments = listOf(arrayGetter, index, valueParameter.capture())
                    )
                } else {
                    val getField = buildGetField(arrayField, arrayGetter.dispatchReceiver)
                    val getSymbol = referenceFunction(referenceArrayClass(arrayField.type as IrSimpleType), GET)
                    buildCall(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        target = getSymbol,
                        type = valueType,
                        origin = IrStatementOrigin.LAMBDA,
                        arguments = listOf(getField, index)
                    )
                }
            )
        )
        this.returnType = returnType
    }
    return IrFunctionExpressionImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        functionType,
        accessorFunction,
        IrStatementOrigin.LAMBDA
    )
}

internal fun IrPluginContext.buildFieldAccessor(
    field: IrField,
    dispatchReceiver: IrExpression?,
    isSetter: Boolean
): IrExpression {
    val valueType = field.type
    val functionType = if (isSetter) buildSetterType(valueType) else buildGetterType(valueType)
    val returnType = if (isSetter) irBuiltIns.unitType else valueType
    val name = if (isSetter) field.setterName() else field.getterName()
    val accessorFunction = buildDefaultPropertyAccessor(name).apply {
        val valueParameter = buildValueParameter(this, name, valueType)
        if (isSetter) parameters += valueParameter
        body = irFactory.buildBlockBody(
            listOf(
                if (isSetter) {
                    buildSetField(field, dispatchReceiver, valueParameter.capture())
                } else {
                    buildGetField(field, dispatchReceiver)
                }
            )
        )
        this.returnType = returnType
    }
    return IrFunctionExpressionImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        functionType,
        accessorFunction,
        IrStatementOrigin.LAMBDA
    )
}

internal fun IrCall.getBackingField(): IrField =
    symbol.owner.correspondingPropertySymbol?.let { propertySymbol ->
        propertySymbol.owner.backingField ?: error("Property expected to have backing field")
    } ?: error("Atomic property accessor ${this.render()} expected to have non-null correspondingPropertySymbol")

internal fun IrPluginContext.referencePackageFunction(
    packageName: String,
    name: String,
    predicate: (IrFunctionSymbol) -> Boolean = { true }
): IrSimpleFunctionSymbol = try {
    finderForBuiltins().findFunctions(CallableId(FqName(packageName), Name.identifier(name))).single(predicate)
} catch (e: RuntimeException) {
    error("Exception while looking for the function `$name` in package `$packageName`: ${e.message}")
}

internal fun IrPluginContext.referenceFunction(classSymbol: IrClassSymbol, functionName: String): IrSimpleFunctionSymbol {
    val functionId = CallableId(FqName("$KOTLIN.${classSymbol.owner.name}"), Name.identifier(functionName))
    return try {
        finderForBuiltins().findFunctions(functionId).single()
    } catch (e: RuntimeException) {
        error("Exception while looking for the function `$functionId`: ${e.message}")
    }
}

private fun IrPluginContext.referenceArrayClass(irType: IrSimpleType): IrClassSymbol {
    val jsArrayName = irType.getArrayClassClassId()
    return finderForBuiltins().findClass(jsArrayName) ?: error("Array class $jsArrayName was not found in the context")
}

internal fun IrPluginContext.getArrayConstructorSymbol(
    irType: IrSimpleType,
    predicate: (IrConstructorSymbol) -> Boolean = { true }
): IrConstructorSymbol {
    val jsArrayName = irType.getArrayClassClassId()
    return try {
        finderForBuiltins().findConstructors(jsArrayName).single(predicate)
    } catch (_: RuntimeException) {
        error("Array constructor $jsArrayName matching the predicate was not found in the context")
    }
}

private fun IrSimpleType.getArrayClassClassId(): ClassId {
    val fqName = classifier.signature?.let { signature ->
        signature.getDeclarationNameBySignature().let { name ->
            AFU_ARRAY_CLASSES[name]?.let { jsArrayName ->
                FqName("$KOTLIN.$jsArrayName")
            }
        }
    } ?: error("Unexpected array type ${this.render()}")
    return ClassId.topLevel(fqName)
}

internal fun IdSignature.getDeclarationNameBySignature(): String? {
    val commonSignature = if (this is IdSignature.AccessorSignature) accessorSignature else asPublic()
    return commonSignature?.declarationFqName
}
