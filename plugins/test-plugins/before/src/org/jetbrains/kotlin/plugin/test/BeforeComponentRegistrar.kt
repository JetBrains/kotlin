/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.test

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name

object BeforeDiagnostics : KtDiagnosticsContainer() {
    val BEFORE_ERROR by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("BeforeDiagnostics") { map ->
            map.put(BEFORE_ERROR, MESSAGE_PLACEHOLDER)
        }
    }
}

class FirBeforeRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(BeforeDiagnostics)
    }
}

class BeforeComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = "kotlin.plugin.test.before"

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(Extension(configuration))
        FirExtensionRegistrar.registerExtension(FirBeforeRegistrar())
    }

    private class Extension(private val configuration: CompilerConfiguration) : IrGenerationExtension {
        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
            for (file in moduleFragment.files) {
                val properties = file.declarations.filterIsInstance<IrProperty>()

                if (properties.singleOrNull { it.name.toString() == "middle_${file.nameWithPackage}" } != null) {
                    configuration.report(
                        BeforeDiagnostics.BEFORE_ERROR,
                        "Plugin 'kotlin.plugin.test.middle' must run after 'kotlin.plugin.test.before'"
                    )
                }

                if (properties.singleOrNull { it.name.toString() == "after_${file.nameWithPackage}" } != null) {
                    configuration.report(
                        BeforeDiagnostics.BEFORE_ERROR,
                        "Plugin 'kotlin.plugin.test.after' must run after 'kotlin.plugin.test.before'"
                    )
                }

                file.declarations += pluginContext.irFactory.buildProperty {
                    name = Name.identifier("before_${file.nameWithPackage}")
                }.apply {
                    parent = file
                    addGetter {
                        returnType = pluginContext.irBuiltIns.stringType
                    }.apply {
                        val builder = pluginContext.irBuiltIns.createIrBuilder(symbol)
                        body = builder.irBlockBody { +irReturn("before".toIrConst(pluginContext.irBuiltIns.stringType)) }
                    }
                }
            }
        }
    }
}
