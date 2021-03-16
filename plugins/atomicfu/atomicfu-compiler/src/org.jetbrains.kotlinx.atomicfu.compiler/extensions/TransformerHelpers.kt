/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

interface TransformerHelpers {

    val context: IrPluginContext

    fun buildFunction(
        parent: IrDeclarationParent,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        returnType: IrType
    ): IrSimpleFunction =
        context.irFactory.buildFun {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            this.origin = origin
            this.name = name
            this.visibility = visibility
            this.isInline = isInline
            this.returnType = returnType
        }.apply {
            this.parent = parent
        }

    fun buildCall(
        target: IrSimpleFunctionSymbol,
        type: IrType? = null,
        origin: IrStatementOrigin? = null,
        typeArguments: List<IrType> = emptyList(),
        valueArguments: List<IrExpression?> = emptyList()
    ): IrCall =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
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

    fun buildBlockBody(statements: List<IrStatement>) =
        context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

    fun buildSetField(
        symbol: IrFieldSymbol,
        receiver: IrExpression?,
        value: IrExpression,
        superQualifierSymbol: IrClassSymbol? = null
    ) =
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

    fun buildGetField(symbol: IrFieldSymbol, receiver: IrExpression?, superQualifierSymbol: IrClassSymbol? = null, type: IrType? = null) =
        IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            type ?: symbol.owner.type,
            receiver,
            IrStatementOrigin.GET_PROPERTY,
            superQualifierSymbol
        )

    fun buildFunctionSimpleType(paramsCount: Int, typeParameters: List<IrType>): IrSimpleType {
        val classSymbol = context.irBuiltIns.function(paramsCount)
        return IrSimpleTypeImpl(
            classifier = classSymbol,
            hasQuestionMark = false,
            arguments = typeParameters.map { it.toTypeArgument() },
            annotations = emptyList()
        )
    }

    fun buildGetterType(valueType: IrType) = buildFunctionSimpleType(0, listOf(valueType))
    fun buildSetterType(valueType: IrType) = buildFunctionSimpleType(1, listOf(valueType, context.irBuiltIns.unitType))

    fun buildGetValue(symbol: IrValueSymbol) =
        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.type, symbol)

    fun getterName(name: String) = "<get-$name>"
    fun setterName(name: String) = "<set-$name>"
    fun Name.getFieldName() = "<get-(\\w+)>".toRegex().find(asString())?.groupValues?.get(1)
        ?: error("Getter name ${this.asString()} does not match special name pattern <get-fieldName>")

    private fun IrType.toTypeArgument(): IrTypeArgument {
        return makeTypeProjection(this, Variance.INVARIANT)
    }

    fun IrFunctionAccessExpression.getValueArguments() = (0 until valueArgumentsCount).map { i ->
        getValueArgument(i)
    }

    fun IrValueParameter.capture() = buildGetValue(symbol)
}