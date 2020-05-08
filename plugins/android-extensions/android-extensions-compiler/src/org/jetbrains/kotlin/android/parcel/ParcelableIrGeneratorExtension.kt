/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.parcel.ir.AndroidSymbols
import org.jetbrains.kotlin.android.parcel.ir.ParcelableIrTransformer
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

@Suppress("DEPRECATION_ERROR")
class ParcelableIrGeneratorExtension : org.jetbrains.kotlin.backend.common.extensions.PureIrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, context: CommonBackendContext) {
        val arrayOfNulls = context.ir.symbols.arrayOfNulls
        val charSequence = context.ir.symbols.charSequence
        val androidSymbols = AndroidSymbols(context.irBuiltIns, arrayOfNulls, charSequence, moduleFragment)
        ParcelableIrTransformer(context, androidSymbols).transform(moduleFragment)
    }
}
