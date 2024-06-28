/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

private val powerAssertRuntimeClasspath = System.getProperty("powerAssertRuntime.classpath")?.split(File.pathSeparator)?.map(::File)
    ?: error("Unable to get a valid classpath from 'powerAssertRuntime.classpath' property")

fun TestConfigurationBuilder.enableRuntime() {
    useConfigurators(::RuntimeEnvironmentConfigurator)
    useCustomRuntimeClasspathProviders(::RuntimeRuntimeClassPathProvider)
}

class RuntimeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        for (file in powerAssertRuntimeClasspath) {
            configuration.addJvmClasspathRoot(file)
        }
    }
}

class RuntimeRuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return powerAssertRuntimeClasspath
    }
}
