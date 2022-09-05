/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationIntrinsicsState
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationExtensionRegistrar
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
        FirExtensionRegistrarAdapter.registerExtension(FirSerializationExtensionRegistrar())
    }
}

class SerializationRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return librariesPaths
    }
}

fun TestConfigurationBuilder.configureForKotlinxSerialization(noLibraries: Boolean = false) {
    useConfigurators(::SerializationEnvironmentConfigurator.bind(noLibraries))
    if (!noLibraries) {
        useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathProvider)
    }
}
