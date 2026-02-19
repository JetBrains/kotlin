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

package org.jetbrains.kotlin.kapt

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.kapt.base.*
import org.jetbrains.kotlin.kapt.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt.cli.KaptCliOption
import org.jetbrains.kotlin.kapt.cli.KaptCliOption.*
import org.jetbrains.kotlin.kapt.cli.KaptCliOption.Companion.ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.utils.decodePluginOptions
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.util.*

val KAPT_OPTIONS = CompilerConfigurationKey.create<KaptOptions.Builder>("KAPT_OPTIONS")

class KaptCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = KaptCliOption.entries

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        doOpenInternalPackagesIfRequired()
        if (option !is KaptCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        val kaptOptions = configuration[KAPT_OPTIONS]
            ?: KaptOptions.Builder().also { configuration.put(KAPT_OPTIONS, it) }

        if (option == @Suppress("DEPRECATION") CONFIGURATION) {
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
            USE_K2 -> {}

            SHOW_PROCESSOR_STATS -> setFlag(KaptFlag.SHOW_PROCESSOR_STATS, value)
            DUMP_PROCESSOR_STATS -> processorsStatsReportFile = File(value)
            DUMP_FILE_READ_HISTORY -> fileReadHistoryReportFile = File(value)
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
