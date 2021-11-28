/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.parcelize.ir.AndroidSymbols
import org.jetbrains.kotlin.parcelize.ir.ParcelizeFirIrTransformer
import org.jetbrains.kotlin.parcelize.ir.ParcelizeIrTransformer

class ParcelizeIrGeneratorExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val androidSymbols = AndroidSymbols(pluginContext.irBuiltIns, moduleFragment)
        ParcelizeIrTransformer(pluginContext, androidSymbols).transform(moduleFragment)
    }
}

class ParcelizeFirIrGeneratorExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val androidSymbols = AndroidSymbols(pluginContext.irBuiltIns, moduleFragment)
        ParcelizeFirIrTransformer(pluginContext, androidSymbols).transform(moduleFragment)
    }
}
