/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.*
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private fun <V> List<TestFile>.retrieveByPathMatch(map: Map<String, V>, default: V): V {
        for ((k, v) in map) {
            if (any { it.originalFile.absolutePath.contains(k) }) {
                return v
            }
        }
        return default
    }

    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        val logLevel = module.files.retrieveByPathMatch(
            mapOf(
                "full_viper_dump" to LogLevel.FULL_VIPER_DUMP,
                "predicates" to LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES
            ), default = LogLevel.SHORT_VIPER_DUMP
        )
        val errorStyle = ErrorStyle.USER_FRIENDLY
        val verificationSelection = module.files.retrieveByPathMatch(
            mapOf(
                "always_validate" to TargetsSelection.ALL_TARGETS,
                "no_contracts" to TargetsSelection.NO_TARGETS
            ), default = TargetsSelection.TARGETS_WITH_CONTRACT
        )
        val config = PluginConfiguration(
            logLevel,
            errorStyle,
            UnsupportedFeatureBehaviour.THROW_EXCEPTION,
            conversionSelection = TargetsSelection.ALL_TARGETS,
            verificationSelection = verificationSelection
        )
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
    }
}
