/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.plugin.sandbox.fir.FirPluginPrototypeExtensionRegistrar
import org.jetbrains.kotlin.plugin.sandbox.ir.GeneratedDeclarationsIrBodyFiller
import org.jetbrains.kotlin.test.AbstractLoadedMetadataDumpHandler
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

object PluginSandboxDirectives : SimpleDirectivesContainer() {
    val DONT_LOAD_IN_SYNTHETIC_MODULES by directive("""
        If enabled plugin won't be applied to the synthetic modules.
        E.g. for empty module created in [${AbstractLoadedMetadataDumpHandler::class}]
    """.trimIndent())
}

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(PluginSandboxDirectives)

    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        if (PluginSandboxDirectives.DONT_LOAD_IN_SYNTHETIC_MODULES in moduleStructure.allDirectives) {
            if (module !in moduleStructure.modules) {
                return
            }
        }
        FirExtensionRegistrarAdapter.registerExtension(FirPluginPrototypeExtensionRegistrar())
        IrGenerationExtension.registerExtension(GeneratedDeclarationsIrBodyFiller())
    }
}
