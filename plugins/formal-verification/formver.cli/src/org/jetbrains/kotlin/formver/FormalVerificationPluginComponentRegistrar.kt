/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class FormalVerificationPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val logLevel = configuration.get(FormalVerificationConfigurationKeys.LOG_LEVEL, LogLevel.defaultLogLevel())
        val behaviour = configuration.get(
            FormalVerificationConfigurationKeys.UNSUPPORTED_FEATURE_BEHAVIOUR,
            UnsupportedFeatureBehaviour.defaultBehaviour()
        )
        val config = PluginConfiguration(logLevel, behaviour)
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
    }
}
