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
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
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
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.kapt3.diagnostic.DefaultErrorMessagesKapt3
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import javax.xml.bind.DatatypeConverter

object Kapt3ConfigurationKeys {
    val SOURCE_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("source files output directory")

    val CLASS_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("class files output directory")

    val STUBS_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("stubs output directory")

    val INCREMENTAL_DATA_OUTPUT_DIR: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("incremental data output directory")

    val ANNOTATION_PROCESSOR_CLASSPATH: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create<List<String>>("annotation processor classpath")

    val APT_OPTIONS: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("annotation processing options")

    val VERBOSE_MODE: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("verbose mode")

    val APT_ONLY: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("do only annotation processing")

    val USE_LIGHT_ANALYSIS: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("do not analyze declaration bodies if can")

    val CORRECT_ERROR_TYPES: CompilerConfigurationKey<String> =
            CompilerConfigurationKey.create<String>("replace error types with ones from the declaration sources")
}

class Kapt3CommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt3"

        val SOURCE_OUTPUT_DIR_OPTION: CliOption =
                CliOption("sources", "<path>", "Output path for the generated files", required = false)

        val CLASS_OUTPUT_DIR_OPTION: CliOption =
                CliOption("classes", "<path>", "Output path for the class files", required = false)

        val STUBS_OUTPUT_DIR_OPTION: CliOption =
                CliOption("stubs", "<path>", "Output path for the Java stubs", required = false)

        val INCREMENTAL_DATA_OUTPUT_DIR_OPTION: CliOption =
                CliOption("incrementalData", "<path>", "Output path for the incremental data", required = false)

        val ANNOTATION_PROCESSOR_CLASSPATH_OPTION: CliOption =
                CliOption("apclasspath", "<classpath>", "Annotation processor classpath",
                          required = false, allowMultipleOccurrences = true)

        val APT_OPTIONS_OPTION: CliOption =
                CliOption("apoptions", "options map", "Encoded annotation processor options",
                          required = false, allowMultipleOccurrences = false)

        val VERBOSE_MODE_OPTION: CliOption =
                CliOption("verbose", "true | false", "Enable verbose output", required = false)

        val APT_ONLY_OPTION: CliOption =
                CliOption("aptOnly", "true | false", "Run only annotation processing, do not compile Kotlin files", required = false)

        val USE_LIGHT_ANALYSIS_OPTION: CliOption =
                CliOption("useLightAnalysis", "true | false", "Do not analyze declaration bodies if can", required = false)

        val CORRECT_ERROR_TYPES_OPTION: CliOption =
                CliOption("correctErrorTypes", "true | false", "Replace generated or error types with ones from the generated sources", required = false)
    }

    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> =
            listOf(SOURCE_OUTPUT_DIR_OPTION, ANNOTATION_PROCESSOR_CLASSPATH_OPTION, APT_OPTIONS_OPTION,
                   CLASS_OUTPUT_DIR_OPTION, VERBOSE_MODE_OPTION, STUBS_OUTPUT_DIR_OPTION, APT_ONLY_OPTION,
                   USE_LIGHT_ANALYSIS_OPTION, CORRECT_ERROR_TYPES_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ANNOTATION_PROCESSOR_CLASSPATH_OPTION -> configuration.appendList(ANNOTATION_PROCESSOR_CLASSPATH, value)
            APT_OPTIONS_OPTION -> configuration.put(Kapt3ConfigurationKeys.APT_OPTIONS, value)
            SOURCE_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.SOURCE_OUTPUT_DIR, value)
            CLASS_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.CLASS_OUTPUT_DIR, value)
            STUBS_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.STUBS_OUTPUT_DIR, value)
            INCREMENTAL_DATA_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.INCREMENTAL_DATA_OUTPUT_DIR, value)
            VERBOSE_MODE_OPTION -> configuration.put(Kapt3ConfigurationKeys.VERBOSE_MODE, value)
            APT_ONLY_OPTION -> configuration.put(Kapt3ConfigurationKeys.APT_ONLY, value)
            USE_LIGHT_ANALYSIS_OPTION -> configuration.put(Kapt3ConfigurationKeys.USE_LIGHT_ANALYSIS, value)
            CORRECT_ERROR_TYPES_OPTION -> configuration.put(Kapt3ConfigurationKeys.CORRECT_ERROR_TYPES, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

class Kapt3ComponentRegistrar : ComponentRegistrar {
    fun decodeAnnotationProcessingOptions(options: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()

        val decodedBytes = DatatypeConverter.parseBase64Binary(options)
        val bis = ByteArrayInputStream(decodedBytes)
        val ois = ObjectInputStream(bis)

        val n = ois.readInt()

        repeat(n) {
            val k = ois.readUTF()
            val v = ois.readUTF()
            map[k] = v
        }

        return map
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val isAptOnly = configuration.get(Kapt3ConfigurationKeys.APT_ONLY) == "true"
        val isVerbose = configuration.get(Kapt3ConfigurationKeys.VERBOSE_MODE) == "true"
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                               ?: PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, isVerbose)
        val logger = KaptLogger(isVerbose, messageCollector)

        val sourcesOutputDir = configuration.get(Kapt3ConfigurationKeys.SOURCE_OUTPUT_DIR)?.let(::File)
        val classFilesOutputDir = configuration.get(Kapt3ConfigurationKeys.CLASS_OUTPUT_DIR)?.let(::File)
        val stubsOutputDir = configuration.get(Kapt3ConfigurationKeys.STUBS_OUTPUT_DIR)?.let(::File)
        val incrementalDataOutputDir = configuration.get(Kapt3ConfigurationKeys.INCREMENTAL_DATA_OUTPUT_DIR)?.let(::File)

        val apClasspath = configuration.get(ANNOTATION_PROCESSOR_CLASSPATH)?.map(::File)

        if (sourcesOutputDir == null || classFilesOutputDir == null || apClasspath == null || stubsOutputDir == null) {
            if (isAptOnly) {
                val nonExistentOptionName = when {
                    sourcesOutputDir == null -> "Sources output directory"
                    classFilesOutputDir == null -> "Classes output directory"
                    apClasspath == null -> "Annotation processing classpath"
                    stubsOutputDir == null -> "Stubs output directory"
                    else -> throw IllegalStateException()
                }
                logger.warn("$nonExistentOptionName is not specified, skipping annotation processing")
                AnalysisHandlerExtension.registerExtension(project, AbortAnalysisHandlerExtension())
            }
            return
        }

        val apOptions = configuration.get(APT_OPTIONS)?.let { decodeAnnotationProcessingOptions(it) } ?: emptyMap()

        sourcesOutputDir.mkdirs()

        val contentRoots = configuration[JVMConfigurationKeys.CONTENT_ROOTS] ?: emptyList()

        val compileClasspath = contentRoots.filterIsInstance<JvmContentRoot>().map { it.file }

        val javaSourceRoots = contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file }

        val useLightAnalysis = configuration.get(Kapt3ConfigurationKeys.USE_LIGHT_ANALYSIS) == "true"
        val correctErrorTypes = configuration.get(Kapt3ConfigurationKeys.CORRECT_ERROR_TYPES) == "true"

        Extensions.getRootArea().getExtensionPoint(DefaultErrorMessages.Extension.EP_NAME).registerExtension(DefaultErrorMessagesKapt3())

        if (isVerbose) {
            logger.info("Kapt3 is enabled.")
            logger.info("Do annotation processing only: $isAptOnly")
            logger.info("Use light analysis: $useLightAnalysis")
            logger.info("Correct error types: $correctErrorTypes")
            logger.info("Source output directory: $sourcesOutputDir")
            logger.info("Classes output directory: $classFilesOutputDir")
            logger.info("Stubs output directory: $stubsOutputDir")
            logger.info("Incremental data output directory: $incrementalDataOutputDir")
            logger.info("Compile classpath: " + compileClasspath.joinToString())
            logger.info("Annotation processing classpath: " + apClasspath.joinToString())
            logger.info("Java source roots: " + javaSourceRoots.joinToString())
            logger.info("Options: $apOptions")
        }

        val kapt3AnalysisCompletedHandlerExtension = ClasspathBasedKapt3Extension(
                compileClasspath, apClasspath, javaSourceRoots, sourcesOutputDir, classFilesOutputDir,
                stubsOutputDir, incrementalDataOutputDir, apOptions,
                isAptOnly, useLightAnalysis, correctErrorTypes, System.currentTimeMillis(), logger)
        AnalysisHandlerExtension.registerExtension(project, kapt3AnalysisCompletedHandlerExtension)
    }

    /* This extension simply disables both code analysis and code generation.
     * When aptOnly is true, and any of required kapt options was not passed, we just abort compilation by providing this extension.
     * */
    private class AbortAnalysisHandlerExtension : AnalysisHandlerExtension {
        override fun doAnalysis(
                project: Project,
                module: ModuleDescriptor,
                projectContext: ProjectContext,
                files: Collection<KtFile>,
                bindingTrace: BindingTrace,
                componentProvider: ComponentProvider
        ): AnalysisResult? {
            return AnalysisResult.success(bindingTrace.bindingContext, module, shouldGenerateCode = false)
        }

        override fun analysisCompleted(
                project: Project,
                module: ModuleDescriptor,
                bindingTrace: BindingTrace,
                files: Collection<KtFile>
        ): AnalysisResult? {
            return AnalysisResult.Companion.success(bindingTrace.bindingContext, module, shouldGenerateCode = false)
        }
    }
}