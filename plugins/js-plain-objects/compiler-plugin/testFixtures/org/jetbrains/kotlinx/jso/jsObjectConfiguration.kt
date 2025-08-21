/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.runners

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.jspo.compiler.cli.JsPlainObjectsComponentRegistrar
import java.io.File

class JsPlainObjectsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        JsPlainObjectsComponentRegistrar.registerExtensions(this)
    }
}

class JsPlainObjectsRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return listOf(File(System.getProperty("jso.runtime.path")))
    }
}

fun TestConfigurationBuilder.configureForKotlinxJsPlainObjects() {
    useConfigurators(::JsPlainObjectsEnvironmentConfigurator)
    useCustomRuntimeClasspathProviders(::JsPlainObjectsRuntimeClasspathProvider)
}
