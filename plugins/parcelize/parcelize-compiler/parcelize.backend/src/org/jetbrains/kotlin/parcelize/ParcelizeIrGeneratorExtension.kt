/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName

// This class is open so that the IDE integration can create a subclass with a fixed set of annotations.
open class ParcelizeIrGeneratorExtension(private val parcelizeAnnotations: List<FqName>) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val androidSymbols = AndroidSymbols(pluginContext, moduleFragment)
        ParcelizeIrTransformer(pluginContext, androidSymbols, parcelizeAnnotations).transform(moduleFragment)
    }
}
