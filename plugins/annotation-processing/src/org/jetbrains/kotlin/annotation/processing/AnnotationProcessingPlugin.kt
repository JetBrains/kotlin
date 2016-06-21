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
import org.jetbrains.kotlin.annotation.AnnotationProcessingExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import java.io.File

object AnnotationProcessingConfigurationKeys {
    val GENERATED_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("generated files output directory")
    
    val ANNOTATION_PROCESSOR_CLASSPATH: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation processor classpath")
}

class AnnotationProcessingCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt2"
        
        val GENERATED_OUTPUT_DIR_OPTION: CliOption =
                CliOption("generated", "<path>", "Output path for generated files", required = false)

        val ANNOTATION_PROCESSOR_CLASSPATH_OPTION: CliOption =
                CliOption("apclasspath", "<classpath>", "Annotation processor classpath", 
                          required = false, allowMultipleOccurrences = true)
    }

    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> =
            listOf(GENERATED_OUTPUT_DIR_OPTION, ANNOTATION_PROCESSOR_CLASSPATH_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ANNOTATION_PROCESSOR_CLASSPATH_OPTION -> {
                val paths = configuration.getList(AnnotationProcessingConfigurationKeys.ANNOTATION_PROCESSOR_CLASSPATH).toMutableList()
                paths.add(value)
                configuration.put(AnnotationProcessingConfigurationKeys.ANNOTATION_PROCESSOR_CLASSPATH, paths)
            }
            GENERATED_OUTPUT_DIR_OPTION -> configuration.put(AnnotationProcessingConfigurationKeys.GENERATED_OUTPUT_DIR, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

class AnnotationProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val generatedOutputDir = configuration.get(AnnotationProcessingConfigurationKeys.GENERATED_OUTPUT_DIR) ?: return
        val classpath = configuration.get(AnnotationProcessingConfigurationKeys.ANNOTATION_PROCESSOR_CLASSPATH) ?: return

        val generatedOutputDirFile = File(generatedOutputDir)
        generatedOutputDirFile.mkdirs()
        
        val annotationProcessingExtension = AnnotationProcessingExtension(generatedOutputDirFile, classpath)
        AnalysisCompletedHandlerExtension.registerExtension(project, annotationProcessingExtension)
    }
}