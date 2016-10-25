/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3

import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.ANNOTATION_PROCESSOR_CLASSPATH
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.APT_OPTIONS
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.kapt3.diagnostic.DefaultErrorMessagesKapt3
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import java.io.File

object Kapt3ConfigurationKeys {
    val GENERATED_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("generated files output directory")

    val CLASS_FILES_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("class files output directory")

    val ANNOTATION_PROCESSOR_CLASSPATH: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation processor classpath")

    val APT_OPTIONS: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation processing options")

    val VERBOSE_MODE: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("verbose mode")
}

class Kapt3CommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt3"

        val GENERATED_OUTPUT_DIR_OPTION: CliOption =
                CliOption("generated", "<path>", "Output path for the generated files", required = false)

        val CLASS_FILES_OUTPUT_DIR_OPTION: CliOption =
                CliOption("classes", "<path>", "Output path for the class files", required = false)

        val ANNOTATION_PROCESSOR_CLASSPATH_OPTION: CliOption =
                CliOption("apclasspath", "<classpath>", "Annotation processor classpath",
                          required = false, allowMultipleOccurrences = true)

        val APT_OPTIONS_OPTION: CliOption =
                CliOption("apoption", "<key>:<value>", "Annotation processor option",
                          required = false, allowMultipleOccurrences = true)

        val VERBOSE_MODE_OPTION: CliOption =
                CliOption("verbose", "true | false", "Enable verbose output", required = false)
    }

    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> =
            listOf(GENERATED_OUTPUT_DIR_OPTION, ANNOTATION_PROCESSOR_CLASSPATH_OPTION, APT_OPTIONS_OPTION,
                   CLASS_FILES_OUTPUT_DIR_OPTION, VERBOSE_MODE_OPTION)

    private fun <T> CompilerConfiguration.appendList(option: CompilerConfigurationKey<List<T>>, value: T) {
        val paths = getList(option).toMutableList()
        paths.add(value)
        put(option, paths)
    }

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ANNOTATION_PROCESSOR_CLASSPATH_OPTION -> configuration.appendList(ANNOTATION_PROCESSOR_CLASSPATH, value)
            APT_OPTIONS_OPTION -> configuration.appendList(APT_OPTIONS, value)
            GENERATED_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.GENERATED_OUTPUT_DIR, value)
            CLASS_FILES_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.CLASS_FILES_OUTPUT_DIR, value)
            VERBOSE_MODE_OPTION -> configuration.put(Kapt3ConfigurationKeys.VERBOSE_MODE, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

class Kapt3ComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val generatedOutputDir = configuration.get(Kapt3ConfigurationKeys.GENERATED_OUTPUT_DIR) ?: return
        val apClasspath = configuration.get(ANNOTATION_PROCESSOR_CLASSPATH)?.map(::File) ?: return

        val apOptions = (configuration.get(APT_OPTIONS) ?: listOf())
                .map { it.split(':') }
                .filter { it.size == 2 }
                .map { it[0] to it[1] }
                .toMap()

        val sourcesOutputDir = File(generatedOutputDir)
        sourcesOutputDir.mkdirs()

        val contentRoots = configuration[JVMConfigurationKeys.CONTENT_ROOTS] ?: emptyList()

        val compileClasspath = contentRoots.filterIsInstance<JvmContentRoot>().map { it.file }
        val classpath = apClasspath + compileClasspath

        val javaSourceRoots = contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file }

        val classesOutputDir = File(configuration.get(Kapt3ConfigurationKeys.CLASS_FILES_OUTPUT_DIR)
                               ?: configuration[JVMConfigurationKeys.MODULES]!!.first().getOutputDirectory())

        val isVerbose = configuration.get(Kapt3ConfigurationKeys.VERBOSE_MODE) == "true"

        Extensions.getRootArea().getExtensionPoint(DefaultErrorMessages.Extension.EP_NAME).registerExtension(DefaultErrorMessagesKapt3())

        val kapt3AnalysisCompletedHandlerExtension = Kapt3AnalysisCompletedHandlerExtension(
                classpath, javaSourceRoots, sourcesOutputDir, classesOutputDir, apOptions, isVerbose)
        AnalysisCompletedHandlerExtension.registerExtension(project, kapt3AnalysisCompletedHandlerExtension)
    }
}