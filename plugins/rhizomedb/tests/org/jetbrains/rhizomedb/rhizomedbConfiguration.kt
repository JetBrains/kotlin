/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.rhizomedb.fir.RhizomedbFirExtensionRegistrar
import org.jetbrains.rhizomedb.ir.GeneratedDeclarationsIrBodyFiller
import java.io.File

class RhizomedbAdditionalSourceFileProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        const val COMMON_SOURCE_PATH = "plugins/rhizomedb/testData/common.kt"
    }

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        return listOf(File(COMMON_SOURCE_PATH).toTestFile())
    }
}

class RhizomedbEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(rhizomeClasspath)
    }

    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(RhizomedbFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(GeneratedDeclarationsIrBodyFiller())
    }
}

class RhizomedbRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return rhizomeClasspath
    }
}

private val rhizomeClasspath = System.getProperty("rhizome.classpath")?.split(File.pathSeparator)?.map(::File)
    ?: error("Unable to get a valid classpath from 'rhizome.classpath' property")

fun TestConfigurationBuilder.configureForRhizomedb(target: TargetBackend = TargetBackend.JVM) {
    useConfigurators(::RhizomedbEnvironmentConfigurator)
    useAdditionalSourceProviders(::RhizomedbAdditionalSourceFileProvider)
    useCustomRuntimeClasspathProviders(::RhizomedbRuntimeClasspathProvider)
//    when (target) {
//        TargetBackend.JVM, TargetBackend.JVM_IR -> useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathJvmProvider)
//        TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> useCustomRuntimeClasspathProviders(::SerializationRuntimeClasspathJsProvider)
//        else -> error("Unsupported backend")
//    }
}
