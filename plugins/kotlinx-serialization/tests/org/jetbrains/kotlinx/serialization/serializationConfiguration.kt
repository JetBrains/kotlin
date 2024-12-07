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
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.configureModernJavaTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathJsProvider
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlinx.serialization.SerializationDirectives.ENABLE_SERIALIZATION
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationIntrinsicsState
import java.io.File

private val librariesPaths = listOfNotNull(RuntimeLibraryInClasspathTest.coreLibraryPath, RuntimeLibraryInClasspathTest.jsonLibraryPath)

class SerializationEnvironmentConfigurator(
    testServices: TestServices,
    private val noLibraries: Boolean
) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(SerializationDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (ENABLE_SERIALIZATION !in module.directives) return
        if (noLibraries) return
        configuration.addJvmClasspathRoots(librariesPaths)
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        if (ENABLE_SERIALIZATION !in module.directives) return
        SerializationComponentRegistrar.registerExtensions(this, SerializationIntrinsicsState.FORCE_ENABLED)
    }
}

class SerializationRuntimeClasspathJvmProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (ENABLE_SERIALIZATION !in module.directives) return emptyList()
        return librariesPaths
    }
}

class SerializationRuntimeClasspathJsProvider(testServices: TestServices) : RuntimeClasspathJsProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (ENABLE_SERIALIZATION !in module.directives) return emptyList()
        return super.runtimeClassPaths(module) + listOf(
            File(System.getProperty("serialization.core.path")!!),
            File(System.getProperty("serialization.json.path")!!),
        )
    }
}

fun TestConfigurationBuilder.configureForKotlinxSerialization(
    noLibraries: Boolean = false,
    target: TargetBackend = TargetBackend.JVM_IR,
    useJdk11: Boolean = false
) {
    defaultDirectives {
        +ENABLE_SERIALIZATION
    }

    useConfigurators(::SerializationEnvironmentConfigurator.bind(noLibraries))

    if (useJdk11) {
        configureModernJavaTest(TestJdkKind.FULL_JDK_11, JvmTarget.JVM_11)
    }

    if (!noLibraries) {
        enableSerializationRuntimeProviders(target)
    }
}

fun TestConfigurationBuilder.enableSerializationRuntimeProviders(target: TargetBackend) {
    when (target) {
        TargetBackend.JVM_IR -> useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathJvmProvider)
        TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathJsProvider)
        else -> error("Unsupported backend")
    }
}

object SerializationDirectives : SimpleDirectivesContainer() {
    val ENABLE_SERIALIZATION by directive("Enables serialization plugin")
}
