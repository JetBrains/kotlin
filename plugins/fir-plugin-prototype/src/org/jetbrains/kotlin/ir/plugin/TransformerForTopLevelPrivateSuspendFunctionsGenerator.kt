/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.plugin.generators.TopLevelPrivateSuspendFunctionGenerator
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody

internal class TransformerForTopLevelPrivateSuspendFunctionsGenerator(context: IrPluginContext) : AbstractTransformerForGenerator(context, visitBodies = false) {
    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key == TopLevelPrivateSuspendFunctionGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey?): IrBody {
        return irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: GeneratedDeclarationKey?): IrBody? {
        error("Should not be called")
    }
}
