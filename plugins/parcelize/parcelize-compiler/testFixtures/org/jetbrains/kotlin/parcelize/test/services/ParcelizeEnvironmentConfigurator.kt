/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.parcelize.ParcelizeComponentRegistrar
import org.jetbrains.kotlin.parcelize.ParcelizeConfigurationKeys
import org.jetbrains.kotlin.parcelize.kotlinxImmutable
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
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
        val runtimeLibraries = if (module.targetPlatform(testServices).isCommon()) {
            ForTestCompileRuntime.parcelizeCommonRuntimeForTests()
        } else {
            ForTestCompileRuntime.parcelizeRuntimeForTests() + KtTestUtil.findAndroidApiJar()
        }
        configuration.addJvmClasspathRoots(runtimeLibraries)

        // Hard coding a name of an additional annotation for parcelize. Test that use this, need to provide the
        // additional annotations as part of the test sources.
        configuration.put(ParcelizeConfigurationKeys.ADDITIONAL_ANNOTATION, listOf("test.TriggerParcelize"))
        // Allow bare value arguments for inherited classes.
        configuration.put(ParcelizeConfigurationKeys.EXPERIMENTAL_CODE_GENERATION, true)
        configuration.put(
            ParcelizeConfigurationKeys.IMPLICITLY_ADD_PARCELABLE_SUPERTYPE,
            IMPLICITLY_ADD_PARCELABLE_SUPERTYPE in module.directives,
        )
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        if (ENABLE_PARCELIZE !in module.directives) return
        with(ParcelizeComponentRegistrar()) { registerExtensions(configuration) }
    }
}

object ParcelizeDirectives : SimpleDirectivesContainer() {
    val ENABLE_PARCELIZE by directive("Enables parcelize plugin")
    val IMPLICITLY_ADD_PARCELABLE_SUPERTYPE by directive(
        "Adds kotlinx.parcelize.internal.Parcelable as a supertype to @Parcelize declarations that do not implement Parcelable directly"
    )
}
