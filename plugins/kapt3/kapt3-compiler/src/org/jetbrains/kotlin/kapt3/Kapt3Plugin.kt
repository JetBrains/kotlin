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
import org.jetbrains.kotlin.base.kapt3.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
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
import org.jetbrains.kotlin.kapt3.base.Kapt
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt3.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.utils.decodePluginOptions
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.util.*

private val KAPT_OPTIONS = CompilerConfigurationKey.create<KaptOptions.Builder>("Kapt options")

class Kapt3CommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        doOpenInternalPackagesIfRequired()
        if (option !is KaptCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
        if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]?.report(
                CompilerMessageSeverity.ERROR,
                "kapt currently doesn't support experimental K2 compiler"
            )
        }

        val kaptOptions = configuration[KAPT_OPTIONS]
            ?: KaptOptions.Builder().also { configuration.put(KAPT_OPTIONS, it) }

        if (option == @Suppress("DEPRECATION") KaptCliOption.CONFIGURATION) {
            configuration.applyOptionsFrom(decodePluginOptions(value), pluginOptions)
        } else {
            kaptOptions.processOption(option, value)
        }
    }

    private fun KaptOptions.Builder.processOption(option: KaptCliOption, value: String) {
        fun setFlag(flag: KaptFlag, value: String) = if (value == "true") flags.add(flag) else flags.remove(flag)

        fun <T : KaptSelector> setSelector(values: Array<T>, rawValue: String, selector: (T) -> Unit) {
            selector(values.firstOrNull { it.stringValue == rawValue }
                         ?: throw CliOptionProcessingException("Unknown value $rawValue for option ${option.optionName}"))
        }

        fun setKeyValue(rawValue: String, apply: (String, String) -> Unit) {
            val keyValuePair = rawValue.split('=', limit = 2).takeIf { it.size == 2 }
                ?: throw CliOptionProcessingException("Invalid option format for ${option.optionName}: key=value expected")
            apply(keyValuePair[0], keyValuePair[1])
        }

        @Suppress("DEPRECATION")
        when (option) {
            SOURCE_OUTPUT_DIR_OPTION -> sourcesOutputDir = File(value)
            CLASS_OUTPUT_DIR_OPTION -> classesOutputDir = File(value)
            STUBS_OUTPUT_DIR_OPTION -> stubsOutputDir = File(value)
            INCREMENTAL_DATA_OUTPUT_DIR_OPTION -> incrementalDataOutputDir = File(value)

            CHANGED_FILES -> changedFiles.add(File(value))
            COMPILED_SOURCES_DIR -> compiledSources.addAll(value.split(File.pathSeparator).map { File(it) })
            INCREMENTAL_CACHE -> incrementalCache = File(value)
            CLASSPATH_CHANGES -> classpathChanges.add(value)
            PROCESS_INCREMENTALLY -> setFlag(KaptFlag.INCREMENTAL_APT, value)

            ANNOTATION_PROCESSOR_CLASSPATH_OPTION -> processingClasspath += File(value)
            ANNOTATION_PROCESSORS_OPTION -> processors.addAll(value.split(',').map { it.trim() }.filter { it.isNotEmpty() })

            APT_OPTION_OPTION -> setKeyValue(value) { k, v -> processingOptions[k] = v }
            JAVAC_OPTION_OPTION -> setKeyValue(value) { k, v -> javacOptions[k] = v }

            VERBOSE_MODE_OPTION -> setFlag(KaptFlag.VERBOSE, value)
            USE_LIGHT_ANALYSIS_OPTION -> setFlag(KaptFlag.USE_LIGHT_ANALYSIS, value)
            CORRECT_ERROR_TYPES_OPTION -> setFlag(KaptFlag.CORRECT_ERROR_TYPES, value)
            DUMP_DEFAULT_PARAMETER_VALUES -> setFlag(KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES, value)
            MAP_DIAGNOSTIC_LOCATIONS_OPTION -> setFlag(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS, value)
            INFO_AS_WARNINGS_OPTION -> setFlag(KaptFlag.INFO_AS_WARNINGS, value)
            STRICT_MODE_OPTION -> setFlag(KaptFlag.STRICT, value)
            STRIP_METADATA_OPTION -> setFlag(KaptFlag.STRIP_METADATA, value)
            KEEP_KDOC_COMMENTS_IN_STUBS -> setFlag(KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS, value)
            USE_JVM_IR -> setFlag(KaptFlag.USE_JVM_IR, value)

            SHOW_PROCESSOR_STATS -> setFlag(KaptFlag.SHOW_PROCESSOR_STATS, value)
            DUMP_PROCESSOR_STATS -> processorsStatsReportFile = File(value)
            INCLUDE_COMPILE_CLASSPATH -> setFlag(KaptFlag.INCLUDE_COMPILE_CLASSPATH, value)

            DETECT_MEMORY_LEAKS_OPTION -> setSelector(enumValues<DetectMemoryLeaksMode>(), value) { detectMemoryLeaks = it }
            APT_MODE_OPTION -> setSelector(enumValues<AptMode>(), value) { mode = it }

            APT_OPTIONS_OPTION -> processingOptions.putAll(decodeMap(value))
            JAVAC_CLI_OPTIONS_OPTION -> javacOptions.putAll(decodeMap(value))
            CONFIGURATION -> throw CliOptionProcessingException("${CONFIGURATION.optionName} should be handled earlier")

            TOOLS_JAR_OPTION -> throw CliOptionProcessingException("'${TOOLS_JAR_OPTION.optionName}' is only supported in the kapt CLI tool")
        }
    }

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
}

class Kapt3ComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        doOpenInternalPackagesIfRequired()
        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()

        val optionsBuilder = (configuration[KAPT_OPTIONS] ?: KaptOptions.Builder()).apply {
            projectBaseDir = project.basePath?.let(::File)
            compileClasspath.addAll(contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file })
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            classesOutputDir = classesOutputDir ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        }

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, optionsBuilder.flags.contains(KaptFlag.VERBOSE))

        val logger = MessageCollectorBackedKaptLogger(
            optionsBuilder.flags.contains(KaptFlag.VERBOSE),
            optionsBuilder.flags.contains(KaptFlag.INFO_AS_WARNINGS),
            messageCollector
        )

        if (!optionsBuilder.checkOptions(project, logger, configuration)) {
            return
        }

        val options = optionsBuilder.build()

        options.sourcesOutputDir.mkdirs()

        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        val kapt3AnalysisCompletedHandlerExtension = ClasspathBasedKapt3Extension(options, logger, configuration)

        AnalysisHandlerExtension.registerExtension(project, kapt3AnalysisCompletedHandlerExtension)
        StorageComponentContainerContributor.registerExtension(project, KaptComponentContributor(kapt3AnalysisCompletedHandlerExtension))
    }

    private fun KaptOptions.Builder.checkOptions(project: MockProject, logger: KaptLogger, configuration: CompilerConfiguration): Boolean {
        fun abortAnalysis() = AnalysisHandlerExtension.registerExtension(project, AbortAnalysisHandlerExtension())

        if (classesOutputDir == null) {
            if (configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
                logger.error("Kapt does not support specifying JAR file outputs. Please specify the classes output directory explicitly.")
                abortAnalysis()
                return false
            } else {
                classesOutputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
            }
        }

        if (processingClasspath.isEmpty()) {
            // Skip annotation processing if no annotation processors were provided
            if (mode != AptMode.WITH_COMPILATION) {
                logger.info("No annotation processors provided. Skip KAPT processing.")
                abortAnalysis()
            }
            return false
        }

        if (sourcesOutputDir == null || classesOutputDir == null || stubsOutputDir == null) {
            if (mode != AptMode.WITH_COMPILATION) {
                val nonExistentOptionName = when {
                    sourcesOutputDir == null -> "Sources output directory"
                    classesOutputDir == null -> "Classes output directory"
                    stubsOutputDir == null -> "Stubs output directory"
                    else -> throw IllegalStateException()
                }
                val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME)
                    ?: configuration.get(JVMConfigurationKeys.MODULES).orEmpty().joinToString()

                logger.warn("$nonExistentOptionName is not specified for $moduleName, skipping annotation processing")
                abortAnalysis()
            }
            return false
        }

        if (!Kapt.checkJavacComponentsAccess(logger)) {
            abortAnalysis()
            return false
        }

        return true
    }

    class KaptComponentContributor(private val analysisExtension: PartialAnalysisHandlerExtension) : StorageComponentContainerContributor {
        override fun registerModuleComponents(
            container: StorageComponentContainer,
            platform: TargetPlatform,
            moduleDescriptor: ModuleDescriptor
        ) {
            if (!platform.isJvm()) return
            container.useInstance(KaptAnonymousTypeTransformer(analysisExtension))
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
