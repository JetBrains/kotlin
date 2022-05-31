/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.kapt.cli.CliToolOption.Format.*

class CliToolOption(val name: String, val format: Format) {
    enum class Format {
        /**
         * A boolean flag.
         * Example: '-option=true'
         * */
        FLAG,

        /**
         * An option with value.
         * Example: '-option=some/path'
         */
        VALUE,

        /**
         * A key-value pair option.
         * Example: '-option:key=value'
         */
        KEY_VALUE
    }
}

enum class KaptCliOption(
    override val optionName: String,
    override val valueDescription: String,
    override val description: String,
    override val allowMultipleOccurrences: Boolean = false,
    val cliToolOption: CliToolOption? = null
) : AbstractCliOption {
    APT_MODE_OPTION(
        "aptMode", "<apt|stubs|stubsAndApt|compile>",
        "Annotation processing mode: only apt, only stub generation, both, or with the subsequent compilation",
        cliToolOption = CliToolOption("-Kapt-mode", VALUE)
    ),

    @Deprecated("Do not use in CLI")
    CONFIGURATION("configuration", "<encoded>", "Encoded configuration"),

    SOURCE_OUTPUT_DIR_OPTION(
        "sources",
        "<path>",
        "Output path for generated sources",
        cliToolOption = CliToolOption("-Kapt-sources", VALUE)
    ),

    CLASS_OUTPUT_DIR_OPTION(
        "classes",
        "<path>",
        "Output path for generated classes",
        cliToolOption = CliToolOption("-Kapt-classes", VALUE)
    ),

    STUBS_OUTPUT_DIR_OPTION(
        "stubs",
        "<path>",
        "Output path for Java stubs",
        cliToolOption = CliToolOption("-Kapt-stubs", VALUE)
    ),

    INCREMENTAL_DATA_OUTPUT_DIR_OPTION("incrementalData", "<path>", "Output path for incremental data"),

    CHANGED_FILES(
        "changedFile",
        "<path>",
        "Use only in apt mode. Changed java source file that should be processed when using incremental annotation processing.",
        true
    ),

    COMPILED_SOURCES_DIR(
        "compiledSourcesDir",
        "<path>",
        "Use only in apt mode. Compiled sources (.class files) from previous compilation. This is typically a kotlinc or javac output.",
        true
    ),

    INCREMENTAL_CACHE(
        "incrementalCache",
        "<path>",
        "Use only in apt mode. Output directory for cache necessary to support incremental annotation processing."
    ),

    CLASSPATH_CHANGES(
        "classpathChange",
        "<jvmInternalName,[jvmInternalName,...]>",
        "Use only in apt mode. Classpath jvm internal names that changed."
    ),

    PROCESS_INCREMENTALLY(
        "processIncrementally",
        "boolean",
        "Use only in apt mode. Enables incremental apt processing"
    ),

    ANNOTATION_PROCESSOR_CLASSPATH_OPTION(
        "apclasspath",
        "<classpath>",
        "Annotation processor classpath",
        true,
        cliToolOption = CliToolOption("-Kapt-classpath", VALUE)
    ),

    ANNOTATION_PROCESSORS_OPTION(
        "processors",
        "<fqname,[fqname2,...]>",
        "Annotation processor qualified names",
        true,
        cliToolOption = CliToolOption("-Kapt-processors", VALUE)
    ),

    APT_OPTION_OPTION(
        "apOption",
        ":<key>=<value>",
        "Annotation processor options",
        cliToolOption = CliToolOption("-Kapt-option", KEY_VALUE)
    ),

    JAVAC_OPTION_OPTION(
        "javacOption",
        ":<key>=<value>",
        "Javac options",
        cliToolOption = CliToolOption("-Kapt-javac-option", KEY_VALUE)
    ),

    TOOLS_JAR_OPTION(
        "toolsJarLocation",
        "<path>",
        "tools.jar file location (for JDK versions up to 1.8)",
        cliToolOption = CliToolOption("-Kapt-tools-jar-location", VALUE)
    ),

    USE_LIGHT_ANALYSIS_OPTION(
        "useLightAnalysis",
        "true | false",
        "Skip body analysis if possible",
        cliToolOption = CliToolOption("-Kapt-use-light-analysis", FLAG)
    ),

    CORRECT_ERROR_TYPES_OPTION(
        "correctErrorTypes",
        "true | false",
        "Replace generated or error types with ones from the generated sources",
        cliToolOption = CliToolOption("-Kapt-correct-error-types", FLAG)
    ),

    DUMP_DEFAULT_PARAMETER_VALUES(
        "dumpDefaultParameterValues",
        "true | false",
        "Put initializers on fields when corresponding primary constructor parameters have a default value specified",
        cliToolOption = CliToolOption("-Kapt-dump-default-parameter-values", FLAG)
    ),

    MAP_DIAGNOSTIC_LOCATIONS_OPTION(
        "mapDiagnosticLocations",
        "true | false",
        "Map diagnostic reported on kapt stubs to original locations in Kotlin sources",
        cliToolOption = CliToolOption("-Kapt-map-diagnostic-locations", FLAG)
    ),

    VERBOSE_MODE_OPTION(
        "verbose",
        "true | false",
        "Enable verbose output",
        cliToolOption = CliToolOption("-Kapt-verbose", FLAG)
    ),

    SHOW_PROCESSOR_STATS(
        "showProcessorStats",
        "true | false",
        "Show processor timings",
        cliToolOption = CliToolOption("-Kapt-show-processor-stats", FLAG)
    ),

    DUMP_PROCESSOR_STATS(
        "dumpProcessorStats",
        "<path>",
        "Dump processor statistics (performance and generations) to the specified file",
        cliToolOption = CliToolOption("-Kapt-dump-processor-stats", VALUE)
    ),

    STRICT_MODE_OPTION(
        "strict",
        "true | false",
        "Show errors on incompatibilities during stub generation",
        cliToolOption = CliToolOption("-Kapt-strict", FLAG)
    ),

    STRIP_METADATA_OPTION(
        "stripMetadata",
        "true | false",
        "Strip @Metadata annotations from stubs",
        cliToolOption = CliToolOption("-Kapt-strip-metadata", FLAG)
    ),

    KEEP_KDOC_COMMENTS_IN_STUBS(
        "keepKdocCommentsInStubs",
        "true | false",
        "Keep KDoc comments in stubs"
    ),

    USE_JVM_IR(
        "useJvmIr",
        "true | false",
        "Use JVM IR backend",
        cliToolOption = CliToolOption("-Kapt-use-jvm-ir", FLAG)
    ),

    DETECT_MEMORY_LEAKS_OPTION("detectMemoryLeaks", "true | false", "Detect memory leaks in annotation processors"),
    INCLUDE_COMPILE_CLASSPATH(
        "includeCompileClasspath",
        "true | false",
        "Discover annotation processors in compile classpath"
    ),

    INFO_AS_WARNINGS_OPTION("infoAsWarnings", "true | false", "Show information messages as warnings"),

    @Deprecated("Do not use in CLI")
    APT_OPTIONS_OPTION("apoptions", "options map", "Encoded annotation processor options", false),

    @Deprecated("Do not use in CLI")
    JAVAC_CLI_OPTIONS_OPTION("javacArguments", "javac CLI options map", "Encoded javac CLI options", false);

    override val required: Boolean = false

    companion object {
        const val ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt3"
    }
}
