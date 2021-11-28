/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.parcel.ir.AndroidSymbols
import org.jetbrains.kotlin.android.parcel.ir.ParcelableIrTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class ParcelableIrGeneratorExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val arrayOfNulls = pluginContext.irBuiltIns.arrayOfNulls
        val charSequence = pluginContext.irBuiltIns.charSequenceClass
        val androidSymbols = AndroidSymbols(pluginContext.irBuiltIns, arrayOfNulls, charSequence, moduleFragment)
        ParcelableIrTransformer(pluginContext, androidSymbols).transform(moduleFragment)
    }
}
