/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

private val junit5Classpath = System.getProperty("junit5.classpath").split(",")

fun TestConfigurationBuilder.enableJunit() {
    useConfigurators(::JunitEnvironmentConfigurator)
    useCustomRuntimeClasspathProviders(::JunitRuntimeClassPathProvider)
}

class JunitEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (PowerAssertConfigurationDirectives.WITH_JUNIT5 in module.directives) {
            for (file in junit5Classpath.map { File(it) }) {
                configuration.addJvmClasspathRoot(file)
            }
        }
    }
}

class JunitRuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (PowerAssertConfigurationDirectives.WITH_JUNIT5 in module.directives) {
            return junit5Classpath.map { File(it) }
        } else {
            return emptyList()
        }
    }
}
