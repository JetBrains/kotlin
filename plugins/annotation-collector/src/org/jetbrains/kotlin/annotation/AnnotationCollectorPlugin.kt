/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

object AnnotationCollectorConfigurationKeys {
    val ANNOTATION_FILTER_LIST: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation filter regular expressions")
    val OUTPUT_FILENAME: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("annotation file name")
    val STUBS_PATH: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("stubs output directory")
    val INHERITED: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("support inherited annotations")
}

class AnnotationCollectorCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_COLLECTOR_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt"

        val ANNOTATION_FILTER_LIST_OPTION: CliOption =
                CliOption("annotations", "<path>", "Annotation filter regular expressions, separated by commas", required = false)

        val OUTPUT_FILENAME_OPTION: CliOption =
                CliOption("output", "<path>", "File in which annotated declarations will be placed", required = false)

        val STUBS_PATH_OPTION: CliOption =
                CliOption("stubs", "<path>", "Output path for stubs", required = false)

        val INHERITED_ANNOTATIONS_OPTION: CliOption =
                CliOption("inherited", "<true/false>",
                          "True if collecting Kotlin class names for inherited annotations is needed", required = false)
    }

    override val pluginId: String = ANNOTATION_COLLECTOR_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> =
            listOf(ANNOTATION_FILTER_LIST_OPTION, OUTPUT_FILENAME_OPTION, STUBS_PATH_OPTION, INHERITED_ANNOTATIONS_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ANNOTATION_FILTER_LIST_OPTION -> {
                val annotations = value.split(',').filter { !it.isEmpty() }.toList()
                configuration.put(AnnotationCollectorConfigurationKeys.ANNOTATION_FILTER_LIST, annotations)
            }
            OUTPUT_FILENAME_OPTION -> configuration.put(AnnotationCollectorConfigurationKeys.OUTPUT_FILENAME, value)
            STUBS_PATH_OPTION -> configuration.put(AnnotationCollectorConfigurationKeys.STUBS_PATH, value)
            INHERITED_ANNOTATIONS_OPTION -> configuration.put(AnnotationCollectorConfigurationKeys.INHERITED, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

class AnnotationCollectorComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val supportInheritedAnnotations = "true" == (configuration.get(AnnotationCollectorConfigurationKeys.INHERITED) ?: "true")

        val annotationFilterList = configuration.get(AnnotationCollectorConfigurationKeys.ANNOTATION_FILTER_LIST)
        val outputFilename = configuration.get(AnnotationCollectorConfigurationKeys.OUTPUT_FILENAME)
        if (outputFilename != null) {
            val collectorExtension = AnnotationCollectorExtension(annotationFilterList, outputFilename, supportInheritedAnnotations)
            ClassBuilderInterceptorExtension.registerExtension(project, collectorExtension)
        }

        val stubs = configuration.get(AnnotationCollectorConfigurationKeys.STUBS_PATH)
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        if (stubs != null) {
            AnalysisHandlerExtension.registerExtension(project, StubProducerExtension(File(stubs), messageCollector))
        }
    }
}

