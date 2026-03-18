/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.powerassert.PowerAssertConfigurationDirectives.DISABLE_RUNTIME
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

fun TestConfigurationBuilder.enableRuntime() {
    useConfigurators(::RuntimeEnvironmentConfigurator)
    useCustomRuntimeClasspathProviders(::RuntimeRuntimeClassPathProvider)
}

class RuntimeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (DISABLE_RUNTIME in module.directives) return

        val platform = module.targetPlatform(testServices)
        when {
            platform.isJvm() -> {
                configuration.addJvmClasspathRoots(findJvmLib())
            }
            platform.isJs() -> {
                val libraries = configuration.getList(JSConfigurationKeys.LIBRARIES)
                configuration.put(JSConfigurationKeys.LIBRARIES, libraries + findJsLib().map { it.absolutePath })
            }
        }
    }
}

class RuntimeRuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (DISABLE_RUNTIME in module.directives) return emptyList()

        val targetPlatform = module.targetPlatform(testServices)
        return when {
            targetPlatform.isJvm() -> findJvmLib()
            targetPlatform.isJs() -> findJsLib()
            else -> emptyList()
        }
    }
}

private fun findJvmLib(): List<File> = findLibFromProperty("powerAssertRuntime.jvm.classpath")
private fun findJsLib(): List<File> = findLibFromProperty("powerAssertRuntime.js.classpath")

private fun findLibFromProperty(property: String): List<File> {
    val paths = System.getProperty(property)
        ?: error("Unable to get a valid classpath from '${property}' property")
    return paths.split(File.pathSeparator).map(::File)
}
