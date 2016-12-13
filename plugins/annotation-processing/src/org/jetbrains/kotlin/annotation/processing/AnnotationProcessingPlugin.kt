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

package org.jetbrains.kotlin.annotation.processing

import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.annotation.ClasspathBasedAnnotationProcessingExtension
import org.jetbrains.kotlin.annotation.processing.diagnostic.DefaultErrorMessagesAnnotationProcessing
import org.jetbrains.kotlin.cli.jvm.config.IS_KAPT2_ENABLED_KEY
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
import org.jetbrains.kotlin.java.model.internal.JeElementRegistry
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

import org.jetbrains.kotlin.annotation.processing.AnnotationProcessingConfigurationKeys.ANNOTATION_PROCESSOR_CLASSPATH
import org.jetbrains.kotlin.annotation.processing.AnnotationProcessingConfigurationKeys.APT_OPTIONS

object AnnotationProcessingConfigurationKeys {
    val GENERATED_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("generated files output directory")

    val CLASS_FILES_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("class files output directory")
    
    val ANNOTATION_PROCESSOR_CLASSPATH: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation processor classpath")

    val APT_OPTIONS: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation processing options")

    val INCREMENTAL_DATA_FILE: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("data file for incremental compilation support")

    val VERBOSE_MODE: CompilerConfigurationKey<String> = 
            CompilerConfigurationKey.create<String>("verbose mode")
}

class AnnotationProcessingCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt2"
        
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

        val INCREMENTAL_DATA_FILE_OPTION: CliOption =
                CliOption("incrementalData", "<path>", "Location of the incremental data file", required = false)

        val VERBOSE_MODE_OPTION: CliOption =
                CliOption("verbose", "true | false", "Enable verbose output", required = false)
    }

    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> =
            listOf(GENERATED_OUTPUT_DIR_OPTION, ANNOTATION_PROCESSOR_CLASSPATH_OPTION, APT_OPTIONS_OPTION,
                   CLASS_FILES_OUTPUT_DIR_OPTION, INCREMENTAL_DATA_FILE_OPTION, VERBOSE_MODE_OPTION)

    private fun <T> CompilerConfiguration.appendList(option: CompilerConfigurationKey<List<T>>, value: T) {
        val paths = getList(option).toMutableList()
        paths.add(value)
        put(option, paths)
    }

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ANNOTATION_PROCESSOR_CLASSPATH_OPTION -> configuration.appendList(ANNOTATION_PROCESSOR_CLASSPATH, value)
            APT_OPTIONS_OPTION -> configuration.appendList(APT_OPTIONS, value)
            GENERATED_OUTPUT_DIR_OPTION -> configuration.put(AnnotationProcessingConfigurationKeys.GENERATED_OUTPUT_DIR, value)
            CLASS_FILES_OUTPUT_DIR_OPTION -> configuration.put(AnnotationProcessingConfigurationKeys.CLASS_FILES_OUTPUT_DIR, value)
            INCREMENTAL_DATA_FILE_OPTION -> configuration.put(AnnotationProcessingConfigurationKeys.INCREMENTAL_DATA_FILE, value)
            VERBOSE_MODE_OPTION -> configuration.put(AnnotationProcessingConfigurationKeys.VERBOSE_MODE, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

class AnnotationProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val generatedOutputDir = configuration.get(AnnotationProcessingConfigurationKeys.GENERATED_OUTPUT_DIR) ?: return
        val apClasspath = configuration.get(ANNOTATION_PROCESSOR_CLASSPATH)?.map(::File) ?: return

        val apOptions = (configuration.get(APT_OPTIONS) ?: listOf())
                .map { it.split(':') }
                .filter { it.size == 2 }
                .map { it[0] to it[1] }
                .toMap()

        val incrementalDataFile = configuration.get(AnnotationProcessingConfigurationKeys.INCREMENTAL_DATA_FILE)?.let(::File)

        val generatedOutputDirFile = File(generatedOutputDir)
        generatedOutputDirFile.mkdirs()

        val contentRoots = configuration[JVMConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
        
        val compileClasspath = contentRoots.filterIsInstance<JvmContentRoot>().map { it.file }
        val classpath = apClasspath + compileClasspath

        val javaRoots = contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file }
        
        val classesOutputDir = File(configuration.get(AnnotationProcessingConfigurationKeys.CLASS_FILES_OUTPUT_DIR) 
                               ?: configuration[JVMConfigurationKeys.MODULES]!!.first().getOutputDirectory()) 
        
        val verboseOutput = configuration.get(AnnotationProcessingConfigurationKeys.VERBOSE_MODE) == "true"

        Extensions.getRootArea().getExtensionPoint(DefaultErrorMessages.Extension.EP_NAME)
                .registerExtension(DefaultErrorMessagesAnnotationProcessing())
        
        // Annotations with the "SOURCE" retention will be written to class files
        project.putUserData(IS_KAPT2_ENABLED_KEY, true)

        val annotationProcessingExtension = ClasspathBasedAnnotationProcessingExtension(
                classpath, apOptions, generatedOutputDirFile, classesOutputDir, javaRoots, verboseOutput,
                incrementalDataFile)

        project.registerService(JeElementRegistry::class.java, JeElementRegistry())
        
        AnalysisHandlerExtension.registerExtension(project, annotationProcessingExtension)
    }
}