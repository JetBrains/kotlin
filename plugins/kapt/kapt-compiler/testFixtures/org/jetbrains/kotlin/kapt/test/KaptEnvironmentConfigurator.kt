/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.kapt.KAPT_OPTIONS
import org.jetbrains.kotlin.kapt.base.AptMode
import org.jetbrains.kotlin.kapt.base.DetectMemoryLeaksMode
import org.jetbrains.kotlin.kapt.base.KaptFlag
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class KaptEnvironmentConfigurator(
    testServices: TestServices,
    private val processorOptions: Map<String, String> = emptyMap()
) : EnvironmentConfigurator(testServices) {
    companion object {
        const val KAPT_RUNNER_DIRECTORY_NAME = "kaptRunner"
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(
        CodegenTestDirectives,
        KaptTestDirectives
    )

    override val additionalServices: List<ServiceRegistrationData> = listOf(service(::KaptOptionsProvider))

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        testServices.kaptOptionsProvider.registerKaptOptions(module) {
            val temporaryDirectoryManager = testServices.temporaryDirectoryManager
            projectBaseDir = temporaryDirectoryManager.rootDir
            compileClasspath.addAll(PathUtil.getJdkClassesRootsFromCurrentJre() + PathUtil.kotlinPathsForIdeaPlugin.stdlibPath)

            sourcesOutputDir = temporaryDirectoryManager.getOrCreateTempDirectory(KAPT_RUNNER_DIRECTORY_NAME)
            classesOutputDir = sourcesOutputDir
            stubsOutputDir = sourcesOutputDir
            incrementalDataOutputDir = sourcesOutputDir

            for (option in module.directives[CodegenTestDirectives.JAVAC_OPTIONS]) {
                val (key, value) = option.split('=').map { it.trim() }.also { assert(it.size == 2) }
                javacOptions.put(key, value)
            }

            for (directive in KaptTestDirectives.flagDirectives) {
                if (directive in module.directives) {
                    flags.add(KaptFlag.valueOf(directive.name))
                }
            }

            processingOptions.putAll(processorOptions)
            detectMemoryLeaks = DetectMemoryLeaksMode.NONE
            if (testServices.defaultsProvider.frontendKind == FrontendKinds.FIR) {
                mode = AptMode.STUBS_AND_APT

                if (processingClasspath.isEmpty()) {
                    // `checkOptions` skips stub generation if processing classpath is empty, which makes integration tests fail.
                    processingClasspath.add(File("."))
                }
            }
            configuration.put(KAPT_OPTIONS, this)
        }

        val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, "kotlin-annotation-processing-runtime.jar")
        configuration.addJvmClasspathRoot(runtimeLibrary)
        configuration.put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)

        configuration.put(JVMConfigurationKeys.SKIP_BODIES, true)
        if (testServices.defaultsProvider.frontendKind == FrontendKinds.FIR) {
            val moduleBuilder = ModuleBuilder(module.name, "", "test-module")
            configuration.put(JVMConfigurationKeys.MODULES, listOf(moduleBuilder))
        }
    }
}
