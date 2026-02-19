/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.test

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name

class MiddleComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = "kotlin.plugin.test.middle"

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(Extension(configuration.messageCollector))
    }

    private class Extension(private val messageCollector: MessageCollector) : IrGenerationExtension {
        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
            for (file in moduleFragment.files) {
                val properties = file.declarations.filterIsInstance<IrProperty>()

                if (properties.singleOrNull { it.name.toString() == "before_${file.nameWithPackage}" } == null) {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Plugin 'kotlin.plugin.test.before' must run before 'kotlin.plugin.test.middle'"
                    )
                }

                if (properties.singleOrNull { it.name.toString() == "after_${file.nameWithPackage}" } != null) {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Plugin 'kotlin.plugin.test.after' must run after 'kotlin.plugin.test.middle'"
                    )
                }

                file.declarations += pluginContext.irFactory.buildProperty {
                    name = Name.identifier("middle_${file.nameWithPackage}")
                }.apply {
                    parent = file
                    addGetter {
                        returnType = pluginContext.irBuiltIns.stringType
                    }.apply {
                        val builder = pluginContext.irBuiltIns.createIrBuilder(symbol)
                        body = builder.irBlockBody { +irReturn("middle".toIrConst(pluginContext.irBuiltIns.stringType)) }
                    }
                }
            }
        }
    }
}
