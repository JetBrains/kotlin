/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.parcel.ir.AndroidSymbols
import org.jetbrains.kotlin.android.parcel.ir.ParcelableIrTransformer
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.extensions.PureIrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class ParcelableIrGeneratorExtension : PureIrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, context: CommonBackendContext) {
        ParcelableIrTransformer(context, AndroidSymbols(context, moduleFragment)).transform(moduleFragment)
    }
}
