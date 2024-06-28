/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.parcelize.ParcelizeComponentRegistrar
import org.jetbrains.kotlin.parcelize.ParcelizeConfigurationKeys
import org.jetbrains.kotlin.parcelize.kotlinxImmutable
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

private fun getLibraryJar(classToDetect: String): File? = try {
    PathUtil.getResourcePathForClass(Class.forName(classToDetect))
} catch (e: ClassNotFoundException) {
    null
}

class ParcelizeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (ENABLE_PARCELIZE !in module.directives) return
        val libPath = PathUtil.kotlinPathsForCompiler.libPath
        val runtimeLibrary = File(libPath, PathUtil.PARCELIZE_RUNTIME_PLUGIN_JAR_NAME)
        val androidExtensionsRuntimeLibrary = File(libPath, PathUtil.ANDROID_EXTENSIONS_RUNTIME_PLUGIN_JAR_NAME)
        val androidApiJar = KtTestUtil.findAndroidApiJar()
        val kotlinxCollectionsImmutable = getLibraryJar(kotlinxImmutable("ImmutableList"))
            ?: error("kotlinx-collections-immutable is not found on classpath")

        configuration.addJvmClasspathRoots(
            listOf(
                runtimeLibrary,
                androidExtensionsRuntimeLibrary,
                androidApiJar,
                kotlinxCollectionsImmutable
            )
        )

        // Hard coding a name of an additional annotation for parcelize. Test that use this, need to provide the
        // additional annotations as part of the test sources.
        configuration.put(ParcelizeConfigurationKeys.ADDITIONAL_ANNOTATION, listOf("test.TriggerParcelize"))
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        if (ENABLE_PARCELIZE !in module.directives) return
        val additionalAnnotation = configuration.get(ParcelizeConfigurationKeys.ADDITIONAL_ANNOTATION) ?: emptyList()
        ParcelizeComponentRegistrar.registerParcelizeComponents(
            this,
            additionalAnnotation,
            useFir = module.frontendKind == FrontendKinds.FIR
        )
    }
}

object ParcelizeDirectives : SimpleDirectivesContainer() {
    val ENABLE_PARCELIZE by directive("Enables parcelize plugin")
}
