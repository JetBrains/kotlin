/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.parcelize.ParcelizeComponentRegistrar
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class ParcelizeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, PathUtil.PARCELIZE_RUNTIME_PLUGIN_JAR_NAME)
        val androidExtensionsRuntimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, PathUtil.ANDROID_EXTENSIONS_RUNTIME_PLUGIN_JAR_NAME)
        val androidApiJar = KtTestUtil.findAndroidApiJar()
        configuration.addJvmClasspathRoots(listOf(runtimeLibrary, androidExtensionsRuntimeLibrary, androidApiJar))
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        ParcelizeComponentRegistrar.registerParcelizeComponents(this, useFir = module.frontendKind == FrontendKinds.FIR)
    }
}
