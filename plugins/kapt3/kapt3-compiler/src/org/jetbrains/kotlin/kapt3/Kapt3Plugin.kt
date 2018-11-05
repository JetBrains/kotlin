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
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.kapt.cli.KaptCliOption
import org.jetbrains.kotlin.kapt.cli.KaptCliOption.Companion.ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.kapt.cli.KaptCliOption.*
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.ANNOTATION_PROCESSOR_CLASSPATH
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.APT_OPTION
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.APT_OPTIONS
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.JAVAC_CLI_OPTIONS
import org.jetbrains.kotlin.kapt3.Kapt3ConfigurationKeys.JAVAC_OPTION
import org.jetbrains.kotlin.kapt3.base.Kapt
import org.jetbrains.kotlin.kapt3.base.KaptPaths
import org.jetbrains.kotlin.kapt3.base.log
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.utils.decodePluginOptions
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.util.*

object Kapt3ConfigurationKeys {
    val SOURCE_OUTPUT_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(SOURCE_OUTPUT_DIR_OPTION.description)

    val CLASS_OUTPUT_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(CLASS_OUTPUT_DIR_OPTION.description)

    val STUBS_OUTPUT_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(STUBS_OUTPUT_DIR_OPTION.description)

    val INCREMENTAL_DATA_OUTPUT_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(INCREMENTAL_DATA_OUTPUT_DIR_OPTION.description)

    val ANNOTATION_PROCESSOR_CLASSPATH: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create<List<String>>(ANNOTATION_PROCESSOR_CLASSPATH_OPTION.description)

    @Suppress("DEPRECATION")
    @Deprecated("Use APT_OPTION instead.")
    val APT_OPTIONS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(APT_OPTIONS_OPTION.description)

    val APT_OPTION: CompilerConfigurationKey<Map<String, String>> =
        CompilerConfigurationKey.create<Map<String, String>>(APT_OPTION_OPTION.description)

    @Suppress("DEPRECATION")
    @Deprecated("Use JAVAC_OPTION instead.")
    val JAVAC_CLI_OPTIONS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(JAVAC_CLI_OPTIONS_OPTION.description)

    val JAVAC_OPTION: CompilerConfigurationKey<Map<String, String>> =
        CompilerConfigurationKey.create<Map<String, String>>(JAVAC_OPTION_OPTION.description)

    val ANNOTATION_PROCESSORS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create<List<String>>(ANNOTATION_PROCESSORS_OPTION.description)

    val VERBOSE_MODE: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(VERBOSE_MODE_OPTION.description)

    val INFO_AS_WARNINGS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(INFO_AS_WARNINGS_OPTION.description)

    @Suppress("DEPRECATION")
    @Deprecated("Use APT_MODE instead.")
    val APT_ONLY: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(APT_ONLY_OPTION.description)

    val APT_MODE: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(APT_MODE_OPTION.description)

    val USE_LIGHT_ANALYSIS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(USE_LIGHT_ANALYSIS_OPTION.description)

    val CORRECT_ERROR_TYPES: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(CORRECT_ERROR_TYPES_OPTION.description)

    val MAP_DIAGNOSTIC_LOCATIONS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(MAP_DIAGNOSTIC_LOCATIONS_OPTION.description)

    val STRICT_MODE: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(STRICT_MODE_OPTION.description)

    val DETECT_MEMORY_LEAKS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(DETECT_MEMORY_LEAKS_OPTION.description)
}

class Kapt3CommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option !is KaptCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        @Suppress("DEPRECATION")
        return when (option) {
            ANNOTATION_PROCESSOR_CLASSPATH_OPTION -> configuration.appendList(ANNOTATION_PROCESSOR_CLASSPATH, value)
            ANNOTATION_PROCESSORS_OPTION -> configuration.appendList(
                Kapt3ConfigurationKeys.ANNOTATION_PROCESSORS, value.split(',').map { it.trim() }.filter { it.isNotEmpty() })
            APT_OPTIONS_OPTION -> configuration.put(APT_OPTIONS, value)
            JAVAC_CLI_OPTIONS_OPTION -> configuration.put(JAVAC_CLI_OPTIONS, value)
            APT_OPTION_OPTION -> configuration.appendMap(APT_OPTION, option, value)
            JAVAC_OPTION_OPTION -> configuration.appendMap(JAVAC_OPTION, option, value)
            SOURCE_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.SOURCE_OUTPUT_DIR, value)
            CLASS_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.CLASS_OUTPUT_DIR, value)
            STUBS_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.STUBS_OUTPUT_DIR, value)
            INCREMENTAL_DATA_OUTPUT_DIR_OPTION -> configuration.put(Kapt3ConfigurationKeys.INCREMENTAL_DATA_OUTPUT_DIR, value)
            VERBOSE_MODE_OPTION -> configuration.put(Kapt3ConfigurationKeys.VERBOSE_MODE, value)
            APT_ONLY_OPTION -> configuration.put(Kapt3ConfigurationKeys.APT_ONLY, value)
            APT_MODE_OPTION -> configuration.put(Kapt3ConfigurationKeys.APT_MODE, value)
            USE_LIGHT_ANALYSIS_OPTION -> configuration.put(Kapt3ConfigurationKeys.USE_LIGHT_ANALYSIS, value)
            CORRECT_ERROR_TYPES_OPTION -> configuration.put(Kapt3ConfigurationKeys.CORRECT_ERROR_TYPES, value)
            MAP_DIAGNOSTIC_LOCATIONS_OPTION -> configuration.put(Kapt3ConfigurationKeys.MAP_DIAGNOSTIC_LOCATIONS, value)
            INFO_AS_WARNINGS_OPTION -> configuration.put(Kapt3ConfigurationKeys.INFO_AS_WARNINGS, value)
            STRICT_MODE_OPTION -> configuration.put(Kapt3ConfigurationKeys.STRICT_MODE, value)
            DETECT_MEMORY_LEAKS_OPTION -> configuration.put(Kapt3ConfigurationKeys.DETECT_MEMORY_LEAKS, value)
            CONFIGURATION -> configuration.applyOptionsFrom(decodePluginOptions(value), pluginOptions)
            TOOLS_JAR_OPTION -> throw CliOptionProcessingException("'${TOOLS_JAR_OPTION.optionName}' is only supported in the kapt CLI tool")
        }
    }

    private fun CompilerConfiguration.appendMap(
        key: CompilerConfigurationKey<Map<String, String>>,
        option: AbstractCliOption,
        keyValuePair: String
    ) {
        val keyValueDecoded = keyValuePair.split('=', limit = 2)
        if (keyValueDecoded.size != 2) {
            throw CliOptionProcessingException("Invalid option format for ${option.optionName}: key=value expected")
        }

        val oldMap = getMap(key)
        val newMap = (oldMap as? MutableMap<String, String>) ?: oldMap.toMutableMap()

        newMap[keyValueDecoded[0]] = keyValueDecoded[1]
        put(key, newMap)
    }
}

class Kapt3ComponentRegistrar : ComponentRegistrar {
    private fun decodeMap(options: String): Map<String, String> {
        if (options.isEmpty()) {
            return emptyMap()
        }

        val map = LinkedHashMap<String, String>()

        val decodedBytes = Base64.getDecoder().decode(options)
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
        @Suppress("DEPRECATION")
        val aptMode = AptMode.parse(configuration.get(Kapt3ConfigurationKeys.APT_MODE) ?: configuration.get(Kapt3ConfigurationKeys.APT_ONLY))

        val isVerbose = configuration.get(Kapt3ConfigurationKeys.VERBOSE_MODE) == "true"
        val infoAsWarnings = configuration.get(Kapt3ConfigurationKeys.INFO_AS_WARNINGS) == "true"
        val strictMode = configuration.get(Kapt3ConfigurationKeys.STRICT_MODE) == "true"
        val detectMemoryLeaks = configuration.get(Kapt3ConfigurationKeys.DETECT_MEMORY_LEAKS) != "false"
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, isVerbose)
        val logger = MessageCollectorBackedKaptLogger(isVerbose, infoAsWarnings, messageCollector)

        fun abortAnalysis() = AnalysisHandlerExtension.registerExtension(project, AbortAnalysisHandlerExtension())

        if (!Kapt.checkJavacComponentsAccess(logger)) {
            abortAnalysis()
            return
        }

        val sourcesOutputDir = configuration.get(Kapt3ConfigurationKeys.SOURCE_OUTPUT_DIR)?.let(::File)
        val stubsOutputDir = configuration.get(Kapt3ConfigurationKeys.STUBS_OUTPUT_DIR)?.let(::File)
        val incrementalDataOutputDir = configuration.get(Kapt3ConfigurationKeys.INCREMENTAL_DATA_OUTPUT_DIR)?.let(::File)

        val classFilesOutputDir = configuration.get(Kapt3ConfigurationKeys.CLASS_OUTPUT_DIR)?.let(::File)
            ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)

        if (classFilesOutputDir == null && configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            logger.error("Kapt does not support specifying JAR file outputs. Please specify the classes output directory explicitly.")
            abortAnalysis()
            return
        }

        val annotationProcessors = configuration.get(Kapt3ConfigurationKeys.ANNOTATION_PROCESSORS) ?: emptyList()

        val apClasspath = configuration.get(ANNOTATION_PROCESSOR_CLASSPATH)?.map(::File) ?: emptyList()

        if (apClasspath.isEmpty()) {
            // Skip annotation processing if no annotation processors were provided
            if (aptMode != AptMode.WITH_COMPILATION) {
                abortAnalysis()
            }
            return
        }

        if (sourcesOutputDir == null || classFilesOutputDir == null || stubsOutputDir == null) {
            if (aptMode != AptMode.WITH_COMPILATION) {
                val nonExistentOptionName = when {
                    sourcesOutputDir == null -> "Sources output directory"
                    classFilesOutputDir == null -> "Classes output directory"
                    stubsOutputDir == null -> "Stubs output directory"
                    else -> throw IllegalStateException()
                }
                val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME)
                                 ?: configuration.get(JVMConfigurationKeys.MODULES).orEmpty().joinToString()
                logger.warn("$nonExistentOptionName is not specified for $moduleName, skipping annotation processing")
                abortAnalysis()
            }
            return
        }

        @Suppress("DEPRECATION")
        val apOptions = decodeMap(configuration.get(APT_OPTIONS).orEmpty()).toMutableMap()
        configuration.get(APT_OPTION)?.let { apOptions += it }

        @Suppress("DEPRECATION")
        val javacOptions = decodeMap(configuration.get(JAVAC_CLI_OPTIONS).orEmpty()).toMutableMap()
        configuration.get(JAVAC_OPTION)?.let { javacOptions += it }

        sourcesOutputDir.mkdirs()

        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()

        val compileClasspath = contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file }

        val javaSourceRoots = contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file }

        val useLightAnalysis = configuration.get(Kapt3ConfigurationKeys.USE_LIGHT_ANALYSIS) != "false"
        val correctErrorTypes = configuration.get(Kapt3ConfigurationKeys.CORRECT_ERROR_TYPES) == "true"
        val mapDiagnosticLocations = configuration.get(Kapt3ConfigurationKeys.MAP_DIAGNOSTIC_LOCATIONS) == "true"

        val paths = KaptPaths(
            project.basePath?.let(::File),
            compileClasspath, apClasspath, javaSourceRoots, sourcesOutputDir, classFilesOutputDir,
            stubsOutputDir, incrementalDataOutputDir
        )

        if (isVerbose) {
            logger.info("Kapt3 is enabled.")
            logger.info("Annotation processing mode: $aptMode")
            logger.info("Use light analysis: $useLightAnalysis")
            logger.info("Correct error types: $correctErrorTypes")
            logger.info("Map diagnostic locations: $mapDiagnosticLocations")
            logger.info("Info as warnings: $infoAsWarnings")
            logger.info("Strict mode: $strictMode")
            paths.log(logger)
            logger.info("Annotation processors: " + annotationProcessors.joinToString())
            logger.info("Javac options: $apOptions")
            logger.info("AP options: $apOptions")
        }

        val kapt3AnalysisCompletedHandlerExtension = ClasspathBasedKapt3Extension(
            paths, apOptions, javacOptions, annotationProcessors,
            aptMode, useLightAnalysis, correctErrorTypes, mapDiagnosticLocations, strictMode, detectMemoryLeaks,
            System.currentTimeMillis(), logger, configuration
        )

        AnalysisHandlerExtension.registerExtension(project, kapt3AnalysisCompletedHandlerExtension)
        StorageComponentContainerContributor.registerExtension(project, KaptComponentContributor())
    }

    class KaptComponentContributor : StorageComponentContainerContributor {
        override fun registerModuleComponents(
            container: StorageComponentContainer,
            platform: TargetPlatform,
            moduleDescriptor: ModuleDescriptor
        ) {
            if (platform != JvmPlatform) return
            container.useInstance(KaptAnonymousTypeTransformer())
        }
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
            return AnalysisResult.success(bindingTrace.bindingContext, module, shouldGenerateCode = false)
        }
    }
}

enum class AptMode(val optionName: String) {
    WITH_COMPILATION("compile"),
    STUBS_AND_APT("stubsAndApt"),
    STUBS_ONLY("stubs"),
    APT_ONLY("apt");

    val runAnnotationProcessing
        get() = this != STUBS_ONLY

    val generateStubs
        get() = this != APT_ONLY

    companion object {
        // Supports both deprecated APT_ONLY and new APT_MODE options
        fun parse(mode: String?): AptMode {
            return when (mode) {
                "true" -> STUBS_AND_APT
                "false" -> WITH_COMPILATION
                else -> AptMode.values().firstOrNull { it.optionName == mode } ?: WITH_COMPILATION
            }
        }
    }
}