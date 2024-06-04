/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.rhizomedb.ir.serializers.CompilerPluginContext

class GeneratedDeclarationsIrBodyFiller(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = CompilerPluginContext(moduleFragment, pluginContext, messageCollector)

        val transformers = listOf(
            TransformerForCompanionGenerator(pluginContext),
            TransformerForAttributeGenerator(pluginContext, context),
        )

        for (transformer in transformers) {
            moduleFragment.acceptChildrenVoid(transformer)
        }
    }
}
