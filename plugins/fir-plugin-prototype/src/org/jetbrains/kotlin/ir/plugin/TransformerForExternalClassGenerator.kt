/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.plugin.generators.ExternalClassGenerator
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody

class TransformerForExternalClassGenerator(context: IrPluginContext) : AbstractTransformerForGenerator(context) {
    override fun interestedIn(key: FirPluginKey): Boolean {
        return key == ExternalClassGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: FirPluginKey): IrBody? {
       return generateDefaultBodyForMaterializeFunction(function)
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: FirPluginKey): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }
}
