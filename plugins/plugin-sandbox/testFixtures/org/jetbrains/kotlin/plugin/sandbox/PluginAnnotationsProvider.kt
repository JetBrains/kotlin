/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime.pluginSandboxAnnotationsJsForTests
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime.pluginSandboxAnnotationsJvmForTests
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform(testServices)
        when {
            platform.isJvm() -> {
                val jar = pluginSandboxAnnotationsJvmForTests()
                configuration.addJvmClasspathRoot(jar)
            }
            platform.isJs() -> {
                val jar = pluginSandboxAnnotationsJsForTests()
                val libraries = configuration.getList(JSConfigurationKeys.LIBRARIES)
                configuration.put(JSConfigurationKeys.LIBRARIES, libraries + jar.absolutePath)
            }
        }
    }
}

class PluginRuntimeAnnotationsProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        val targetPlatform = module.targetPlatform(testServices)
        return when {
            targetPlatform.isJvm() -> listOf(pluginSandboxAnnotationsJvmForTests())
            targetPlatform.isJs() -> listOf(pluginSandboxAnnotationsJsForTests())
            else -> emptyList()
        }
    }
}