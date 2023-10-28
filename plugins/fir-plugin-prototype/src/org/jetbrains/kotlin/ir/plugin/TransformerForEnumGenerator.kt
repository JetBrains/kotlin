/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.plugin.generators.EnumGenerator
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.constructors

class TransformerForEnumGenerator(context: IrPluginContext) : AbstractTransformerForGenerator(context, visitBodies = true) {
    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key == EnumGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey?): IrBody? {
        return null
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: GeneratedDeclarationKey?): IrBody? {
        val type = constructor.returnType as? IrSimpleType ?: return null

        val enumConstructor = irBuiltIns.enumClass.owner.constructors.singleOrNull() ?: return null
        val delegatingCall = IrEnumConstructorCallImpl(
            -1,
            -1,
            type,
            enumConstructor.symbol,
            typeArgumentsCount = 1,
            valueArgumentsCount = enumConstructor.valueParameters.size
        )
        delegatingCall.putTypeArgument(0, type)

        val initializerCall = IrInstanceInitializerCallImpl(
            -1,
            -1,
            (constructor.parent as? IrClass)?.symbol ?: return null,
            type
        )

        return irFactory.createBlockBody(-1, -1, listOf(delegatingCall, initializerCall))
    }
}
