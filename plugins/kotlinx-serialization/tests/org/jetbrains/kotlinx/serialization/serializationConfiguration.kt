/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.configureModernJavaTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathJsProvider
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationIntrinsicsState
import java.io.File

private val librariesPaths = listOfNotNull(RuntimeLibraryInClasspathTest.coreLibraryPath, RuntimeLibraryInClasspathTest.jsonLibraryPath)

class SerializationEnvironmentConfigurator(
    testServices: TestServices,
    private val noLibraries: Boolean
) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (noLibraries) return
        configuration.addJvmClasspathRoots(librariesPaths)
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        SerializationComponentRegistrar.registerExtensions(this, SerializationIntrinsicsState.FORCE_ENABLED)
    }
}

class SerializationRuntimeClasspathJvmProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return librariesPaths
    }
}

class SerializationRuntimeClasspathJsProvider(testServices: TestServices) : RuntimeClasspathJsProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return super.runtimeClassPaths(module) + listOf(
            File(System.getProperty("serialization.core.path")!!),
            File(System.getProperty("serialization.json.path")!!),
        )
    }
}

fun TestConfigurationBuilder.configureForKotlinxSerialization(
    noLibraries: Boolean = false,
    target: TargetBackend = TargetBackend.JVM,
    useJdk11: Boolean = false
) {
    useConfigurators(::SerializationEnvironmentConfigurator.bind(noLibraries))

    if (useJdk11) {
        configureModernJavaTest(TestJdkKind.FULL_JDK_11, JvmTarget.JVM_11)
    }

    if (!noLibraries) {
        when (target) {
            TargetBackend.JVM, TargetBackend.JVM_IR -> useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathJvmProvider)
            TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathJsProvider)
            else -> error("Unsupported backend")
        }
    }
}
