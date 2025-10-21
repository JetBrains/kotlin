/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.plugin.sandbox.fir.generators.AdditionalMembersGenerator
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl

class TransformerForAdditionalMembersGenerator(context: IrPluginContext) : AbstractTransformerForGenerator(context, visitBodies = false) {
    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key == AdditionalMembersGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey?): IrBody? {
        return when (function.name) {
            AdditionalMembersGenerator.MATERIALIZE_NAME -> generateDefaultBodyForMaterializeFunction(function)
            AdditionalMembersGenerator.ID_WITH_DEFAULT_NAME -> {
                // default value for parameter
                val parameter = function.parameters.first { it.kind == IrParameterKind.Regular }.also { irParameter ->
                    val defaultValue = IrConstImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        irBuiltIns.stringType,
                        IrConstKind.String,
                        value = "OK"
                    )
                    irParameter.defaultValue = context.irFactory.createExpressionBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, defaultValue)
                }

                // actual body
                val getValue = IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    parameter.type,
                    parameter.symbol
                )
                val returnStatement = IrReturnImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    irBuiltIns.nothingType, function.symbol, getValue
                )
                context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(returnStatement))
            }
            else -> function.body
        }
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: GeneratedDeclarationKey?): IrBody? {
        return constructor.body
    }
}
