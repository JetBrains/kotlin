/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.kapt3.Kapt3ComponentRegistrar
import org.jetbrains.kotlin.kapt3.base.DetectMemoryLeaksMode
import org.jetbrains.kotlin.kapt3.base.KaptFlag
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.runIf
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

            val javacOptions = module.directives[CodegenTestDirectives.JAVAC_OPTIONS].associate { opt ->
                val (key, value) = opt.split('=').map { it.trim() }.also { assert(it.size == 2) }
                key to value
            }

            this.javacOptions.putAll(javacOptions)
            val kaptFlagsToAdd = KaptTestDirectives.flagDirectives.mapNotNull {
                runIf(it in module.directives) {
                    KaptFlag.valueOf(it.name)
                }
            }
            flags.addAll(kaptFlagsToAdd)
            flags.removeAll(module.directives[KaptTestDirectives.DISABLED_FLAGS])

            processingOptions.putAll(processorOptions)
            detectMemoryLeaks = DetectMemoryLeaksMode.NONE
        }

        val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, "kotlin-annotation-processing-runtime.jar")
        configuration.addJvmClasspathRoot(runtimeLibrary)
        configuration.put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)
    }
}

class KaptRegularExtensionForTestConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        val analysisExtension = object : PartialAnalysisHandlerExtension() {
            override val analyzeDefaultParameterValues: Boolean
                get() = testServices.kaptOptionsProvider[module][KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES]
        }
        AnalysisHandlerExtension.registerExtension(project, analysisExtension)
        StorageComponentContainerContributor.registerExtension(project, Kapt3ComponentRegistrar.KaptComponentContributor(analysisExtension))
    }
}
