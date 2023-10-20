/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        val logLevel = when {
            module.files.any { it.name.contains("full_viper_dump") } -> LogLevel.FULL_VIPER_DUMP
            module.files.any { it.name.contains("predicates") } -> LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES
            else -> LogLevel.SHORT_VIPER_DUMP
        }
        val verificationSelection =
            if (module.files.any { it.name.contains("always_validate") }) TargetsSelection.ALL_TARGETS
            else if (module.files.any {it.name.contains("no_contracts") }) TargetsSelection.NO_TARGETS
            else TargetsSelection.TARGETS_WITH_CONTRACT
        val config = PluginConfiguration(
            logLevel,
            UnsupportedFeatureBehaviour.THROW_EXCEPTION,
            conversionSelection = TargetsSelection.ALL_TARGETS,
            verificationSelection = verificationSelection
        )
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
    }
}
