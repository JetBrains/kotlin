/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.compiler.extension

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.rhizomedb.fir.extensions.RhizomedbFirExtensionRegistrar
import org.jetbrains.rhizomedb.ir.GeneratedDeclarationsIrBodyFiller

class RhizomedbComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(GeneratedDeclarationsIrBodyFiller())
        FirExtensionRegistrarAdapter.registerExtension(RhizomedbFirExtensionRegistrar())
    }

    override val supportsK2: Boolean
        get() = true
}
