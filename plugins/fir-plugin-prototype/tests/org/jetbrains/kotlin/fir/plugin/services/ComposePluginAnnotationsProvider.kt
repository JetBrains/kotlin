/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

private const val COMPOSE_RUNTIME_DIR = "plugins/compose/compiler-hosted/libs/"
private val COMPOSE_RUNTIME_JAR_FILTER = createFilter("compose-runtime", ".jar")

class ComposePluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform
        when {
            platform.isJvm() -> {
                findLibByPath(COMPOSE_RUNTIME_DIR, COMPOSE_RUNTIME_JAR_FILTER)?.let { configuration.addJvmClasspathRoot(it) }
            }
        }
    }
}