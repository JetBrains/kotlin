/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.js.config

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import java.io.File
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer

object JSConfigurationKeys {
    @JvmField
    val WASM_COMPILATION = CompilerConfigurationKey.create<Boolean>("WASM_COMPILATION")

    // Name of output KLib file.
    @JvmField
    val OUTPUT_NAME = CompilerConfigurationKey.create<String>("OUTPUT_NAME")

    @JvmField
    val LIBRARIES = CompilerConfigurationKey.create<List<String>>("LIBRARIES")

    @JvmField
    val FRIEND_LIBRARIES = CompilerConfigurationKey.create<List<String>>("FRIEND_LIBRARIES")

    @JvmField
    val SOURCE_MAP = CompilerConfigurationKey.create<Boolean>("SOURCE_MAP")

    @JvmField
    val USE_DEBUGGER_CUSTOM_FORMATTERS = CompilerConfigurationKey.create<Boolean>("USE_DEBUGGER_CUSTOM_FORMATTERS")

    @JvmField
    val ARTIFACT_CONFIGURATION = CompilerConfigurationKey.create<WebArtifactConfiguration>("ARTIFACT_CONFIGURATION")

    @JvmField
    val OUTPUT_DIR = CompilerConfigurationKey.create<File>("OUTPUT_DIR")

    // Prefix to add to paths in source map.
    @JvmField
    val SOURCE_MAP_PREFIX = CompilerConfigurationKey.create<String>("SOURCE_MAP_PREFIX")

    // Base directories used to calculate relative paths for source map.
    @JvmField
    val SOURCE_MAP_SOURCE_ROOTS = CompilerConfigurationKey.create<List<String>>("SOURCE_MAP_SOURCE_ROOTS")

    // Embed source files into source map.
    @JvmField
    val SOURCE_MAP_EMBED_SOURCES = CompilerConfigurationKey.create<SourceMapSourceEmbedding>("SOURCE_MAP_EMBED_SOURCES")

    // A policy to generate a mapping from generated identifiers to their corresponding original names.
    @JvmField
    val SOURCEMAP_NAMES_POLICY = CompilerConfigurationKey.create<SourceMapNamesPolicy>("SOURCEMAP_NAMES_POLICY")

    // Insert source mappings from libraries even if their sources are unavailable on the end-user machine.
    @JvmField
    val SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES = CompilerConfigurationKey.create<Boolean>("SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES")

    @JvmField
    val MODULE_KIND = CompilerConfigurationKey.create<ModuleKind>("MODULE_KIND")

    @JvmField
    val JS_INCREMENTAL_COMPILATION_ENABLED = CompilerConfigurationKey.create<Boolean>("JS_INCREMENTAL_COMPILATION_ENABLED")

    @JvmField
    val INCREMENTAL_DATA_PROVIDER = CompilerConfigurationKey.create<IncrementalDataProvider>("INCREMENTAL_DATA_PROVIDER")

    @JvmField
    val INCREMENTAL_RESULTS_CONSUMER = CompilerConfigurationKey.create<IncrementalResultsConsumer>("INCREMENTAL_RESULTS_CONSUMER")

    @JvmField
    val INCREMENTAL_NEXT_ROUND_CHECKER = CompilerConfigurationKey.create<IncrementalNextRoundChecker>("INCREMENTAL_NEXT_ROUND_CHECKER")

    @JvmField
    val FRIEND_PATHS_DISABLED = CompilerConfigurationKey.create<Boolean>("FRIEND_PATHS_DISABLED")

    @JvmField
    val METADATA_ONLY = CompilerConfigurationKey.create<Boolean>("METADATA_ONLY")

    // Enable additional checkers.
    @JvmField
    val DEVELOPER_MODE = CompilerConfigurationKey.create<Boolean>("DEVELOPER_MODE")

    // Generate comments with file path at the start of each file block.
    @JvmField
    val GENERATE_COMMENTS_WITH_FILE_PATH = CompilerConfigurationKey.create<Boolean>("GENERATE_COMMENTS_WITH_FILE_PATH")

    // Generate polyfills for newest properties, methods and classes from ES6+.
    @JvmField
    val GENERATE_POLYFILLS = CompilerConfigurationKey.create<Boolean>("GENERATE_POLYFILLS")

    // Provide platform-specific args as a parameter of the main function.
    @JvmField
    val DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS = CompilerConfigurationKey.create<String>("DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS")

    // Generate TypeScript definition file.
    @JvmField
    val GENERATE_DTS = CompilerConfigurationKey.create<Boolean>("GENERATE_DTS")

    // Strategy to generate TypeScript definition file.
    @JvmField
    val DTS_COMPILATION_STRATEGY = CompilerConfigurationKey.create<TsCompilationStrategy>("DTS_COMPILATION_STRATEGY")

    // Force suspend functions compilation in JS generator functions.
    @JvmField
    val COMPILE_SUSPEND_AS_JS_GENERATOR = CompilerConfigurationKey.create<Boolean>("COMPILE_SUSPEND_AS_JS_GENERATOR")

    // Lower Kotlin lambdas into arrow functions instead of anonymous functions.
    @JvmField
    val COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS = CompilerConfigurationKey.create<Boolean>("COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS")

    @JvmField
    val COMPILE_LONG_AS_BIGINT = CompilerConfigurationKey.create<Boolean>("COMPILE_LONG_AS_BIGINT")

    // Generate special comments at the start and the end of each file block, it allows to fold them and navigate to them in IDEA.
    @JvmField
    val GENERATE_REGION_COMMENTS = CompilerConfigurationKey.create<Boolean>("GENERATE_REGION_COMMENTS")

    // This map is used to shorten/replace prefix of paths in comments with file paths, including region comments.
    @JvmField
    val FILE_PATHS_PREFIX_MAP = CompilerConfigurationKey.create<Map<String, String>>("FILE_PATHS_PREFIX_MAP")

    // Print declarations' reachability info during performing DCE.
    @JvmField
    val PRINT_REACHABILITY_INFO = CompilerConfigurationKey.create<Boolean>("PRINT_REACHABILITY_INFO")

    // Dump declarations' reachability info to file during performing DCE.
    @JvmField
    val DUMP_REACHABILITY_INFO_TO_FILE = CompilerConfigurationKey.create<String>("DUMP_REACHABILITY_INFO_TO_FILE")

    @JvmField
    val FAKE_OVERRIDE_VALIDATOR = CompilerConfigurationKey.create<Boolean>("FAKE_OVERRIDE_VALIDATOR")

    @JvmField
    val PROPERTY_LAZY_INITIALIZATION = CompilerConfigurationKey.create<Boolean>("PROPERTY_LAZY_INITIALIZATION")

    // Translate lambdas into inline anonymous functions.
    @JvmField
    val GENERATE_INLINE_ANONYMOUS_FUNCTIONS = CompilerConfigurationKey.create<Boolean>("GENERATE_INLINE_ANONYMOUS_FUNCTIONS")

    // Enable strict implicitly exported entities types inside d.ts files.
    @JvmField
    val GENERATE_STRICT_IMPLICIT_EXPORT = CompilerConfigurationKey.create<Boolean>("GENERATE_STRICT_IMPLICIT_EXPORT")

    @JvmField
    val OPTIMIZE_GENERATED_JS = CompilerConfigurationKey.create<Boolean>("OPTIMIZE_GENERATED_JS")

    @JvmField
    val USE_ES6_CLASSES = CompilerConfigurationKey.create<Boolean>("USE_ES6_CLASSES")

    // List of KLibs for this linking phase.
    @JvmField
    val INCLUDES = CompilerConfigurationKey.create<String>("INCLUDES")

    // Need to produce KLib file or not.
    @JvmField
    val PRODUCE_KLIB_FILE = CompilerConfigurationKey.create<Boolean>("PRODUCE_KLIB_FILE")

    // Need to produce unpacked KLib dir or not.
    @JvmField
    val PRODUCE_KLIB_DIR = CompilerConfigurationKey.create<Boolean>("PRODUCE_KLIB_DIR")

    // Custom output name to the split .js files.
    @JvmField
    val PER_MODULE_OUTPUT_NAME = CompilerConfigurationKey.create<String>("PER_MODULE_OUTPUT_NAME")

    // List of fully qualified names not to be eliminated by DCE.
    @JvmField
    val KEEP = CompilerConfigurationKey.create<List<String>>("KEEP")

    // Perform experimental dead code elimination.
    @JvmField
    val DCE = CompilerConfigurationKey.create<Boolean>("DCE")

    // Enable runtime diagnostics instead of removing declarations when performing DCE.
    @JvmField
    val DCE_RUNTIME_DIAGNOSTIC = CompilerConfigurationKey.create<String>("DCE_RUNTIME_DIAGNOSTIC")

    // Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.
    @JvmField
    val SAFE_EXTERNAL_BOOLEAN = CompilerConfigurationKey.create<Boolean>("SAFE_EXTERNAL_BOOLEAN")

    // Enable runtime diagnostics when accessing external 'Boolean' properties.
    @JvmField
    val SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC = CompilerConfigurationKey.create<String>("SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")

    @JvmField
    val MINIMIZED_MEMBER_NAMES = CompilerConfigurationKey.create<Boolean>("MINIMIZED_MEMBER_NAMES")

    // Specify whether the 'main' function should be called upon execution.
    @JvmField
    val CALL_MAIN_MODE = CompilerConfigurationKey.create<String>("CALL_MAIN_MODE")

    @JvmField
    val IC_CACHE_DIRECTORY = CompilerConfigurationKey.create<String>("IC_CACHE_DIRECTORY")

    @JvmField
    val IC_CACHE_READ_ONLY = CompilerConfigurationKey.create<Boolean>("IC_CACHE_READ_ONLY")

    @JvmField
    val PRESERVE_IC_ORDER = CompilerConfigurationKey.create<Boolean>("PRESERVE_IC_ORDER")

}

var CompilerConfiguration.wasmCompilation: Boolean
    get() = getBoolean(JSConfigurationKeys.WASM_COMPILATION)
    set(value) { put(JSConfigurationKeys.WASM_COMPILATION, value) }

var CompilerConfiguration.outputName: String?
    get() = get(JSConfigurationKeys.OUTPUT_NAME)
    set(value) { put(JSConfigurationKeys.OUTPUT_NAME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.libraries: List<String>
    get() = getList(JSConfigurationKeys.LIBRARIES)
    set(value) { put(JSConfigurationKeys.LIBRARIES, value) }

var CompilerConfiguration.friendLibraries: List<String>
    get() = getList(JSConfigurationKeys.FRIEND_LIBRARIES)
    set(value) { put(JSConfigurationKeys.FRIEND_LIBRARIES, value) }

var CompilerConfiguration.sourceMap: Boolean
    get() = getBoolean(JSConfigurationKeys.SOURCE_MAP)
    set(value) { put(JSConfigurationKeys.SOURCE_MAP, value) }

var CompilerConfiguration.useDebuggerCustomFormatters: Boolean
    get() = getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)
    set(value) { put(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS, value) }

var CompilerConfiguration.artifactConfiguration: WebArtifactConfiguration?
    get() = get(JSConfigurationKeys.ARTIFACT_CONFIGURATION)
    set(value) { put(JSConfigurationKeys.ARTIFACT_CONFIGURATION, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.outputDir: File?
    get() = get(JSConfigurationKeys.OUTPUT_DIR)
    set(value) { put(JSConfigurationKeys.OUTPUT_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.sourceMapPrefix: String?
    get() = get(JSConfigurationKeys.SOURCE_MAP_PREFIX)
    set(value) { put(JSConfigurationKeys.SOURCE_MAP_PREFIX, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.sourceMapSourceRoots: List<String>
    get() = getList(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS)
    set(value) { put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, value) }

var CompilerConfiguration.sourceMapEmbedSources: SourceMapSourceEmbedding?
    get() = get(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES)
    set(value) { put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.sourcemapNamesPolicy: SourceMapNamesPolicy?
    get() = get(JSConfigurationKeys.SOURCEMAP_NAMES_POLICY)
    set(value) { put(JSConfigurationKeys.SOURCEMAP_NAMES_POLICY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.sourceMapIncludeMappingsFromUnavailableFiles: Boolean
    get() = getBoolean(JSConfigurationKeys.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES)
    set(value) { put(JSConfigurationKeys.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES, value) }

var CompilerConfiguration.moduleKind: ModuleKind?
    get() = get(JSConfigurationKeys.MODULE_KIND)
    set(value) { put(JSConfigurationKeys.MODULE_KIND, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.jsIncrementalCompilationEnabled: Boolean
    get() = getBoolean(JSConfigurationKeys.JS_INCREMENTAL_COMPILATION_ENABLED)
    set(value) { put(JSConfigurationKeys.JS_INCREMENTAL_COMPILATION_ENABLED, value) }

var CompilerConfiguration.incrementalDataProvider: IncrementalDataProvider?
    get() = get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)
    set(value) { putIfNotNull(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER, value) }

var CompilerConfiguration.incrementalResultsConsumer: IncrementalResultsConsumer?
    get() = get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    set(value) { putIfNotNull(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, value) }

var CompilerConfiguration.incrementalNextRoundChecker: IncrementalNextRoundChecker?
    get() = get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER)
    set(value) { putIfNotNull(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER, value) }

var CompilerConfiguration.friendPathsDisabled: Boolean
    get() = getBoolean(JSConfigurationKeys.FRIEND_PATHS_DISABLED)
    set(value) { put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, value) }

var CompilerConfiguration.metadataOnly: Boolean
    get() = getBoolean(JSConfigurationKeys.METADATA_ONLY)
    set(value) { put(JSConfigurationKeys.METADATA_ONLY, value) }

var CompilerConfiguration.developerMode: Boolean
    get() = getBoolean(JSConfigurationKeys.DEVELOPER_MODE)
    set(value) { put(JSConfigurationKeys.DEVELOPER_MODE, value) }

var CompilerConfiguration.generateCommentsWithFilePath: Boolean
    get() = getBoolean(JSConfigurationKeys.GENERATE_COMMENTS_WITH_FILE_PATH)
    set(value) { put(JSConfigurationKeys.GENERATE_COMMENTS_WITH_FILE_PATH, value) }

var CompilerConfiguration.generatePolyfills: Boolean
    get() = getBoolean(JSConfigurationKeys.GENERATE_POLYFILLS)
    set(value) { put(JSConfigurationKeys.GENERATE_POLYFILLS, value) }

var CompilerConfiguration.definePlatformMainFunctionArguments: String?
    get() = get(JSConfigurationKeys.DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS)
    set(value) { put(JSConfigurationKeys.DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.generateDts: Boolean
    get() = getBoolean(JSConfigurationKeys.GENERATE_DTS)
    set(value) { put(JSConfigurationKeys.GENERATE_DTS, value) }

var CompilerConfiguration.dtsCompilationStrategy: TsCompilationStrategy?
    get() = get(JSConfigurationKeys.DTS_COMPILATION_STRATEGY)
    set(value) { put(JSConfigurationKeys.DTS_COMPILATION_STRATEGY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.compileSuspendAsJsGenerator: Boolean
    get() = getBoolean(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR)
    set(value) { put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, value) }

var CompilerConfiguration.compileLambdasAsEs6ArrowFunctions: Boolean
    get() = getBoolean(JSConfigurationKeys.COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS)
    set(value) { put(JSConfigurationKeys.COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS, value) }

var CompilerConfiguration.compileLongAsBigint: Boolean
    get() = getBoolean(JSConfigurationKeys.COMPILE_LONG_AS_BIGINT)
    set(value) { put(JSConfigurationKeys.COMPILE_LONG_AS_BIGINT, value) }

var CompilerConfiguration.generateRegionComments: Boolean
    get() = getBoolean(JSConfigurationKeys.GENERATE_REGION_COMMENTS)
    set(value) { put(JSConfigurationKeys.GENERATE_REGION_COMMENTS, value) }

var CompilerConfiguration.filePathsPrefixMap: Map<String, String>
    get() = getMap(JSConfigurationKeys.FILE_PATHS_PREFIX_MAP)
    set(value) { put(JSConfigurationKeys.FILE_PATHS_PREFIX_MAP, value) }

var CompilerConfiguration.printReachabilityInfo: Boolean
    get() = getBoolean(JSConfigurationKeys.PRINT_REACHABILITY_INFO)
    set(value) { put(JSConfigurationKeys.PRINT_REACHABILITY_INFO, value) }

var CompilerConfiguration.dumpReachabilityInfoToFile: String?
    get() = get(JSConfigurationKeys.DUMP_REACHABILITY_INFO_TO_FILE)
    set(value) { putIfNotNull(JSConfigurationKeys.DUMP_REACHABILITY_INFO_TO_FILE, value) }

var CompilerConfiguration.fakeOverrideValidator: Boolean
    get() = getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)
    set(value) { put(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR, value) }

var CompilerConfiguration.propertyLazyInitialization: Boolean
    get() = getBoolean(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
    set(value) { put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, value) }

var CompilerConfiguration.generateInlineAnonymousFunctions: Boolean
    get() = getBoolean(JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS)
    set(value) { put(JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS, value) }

var CompilerConfiguration.generateStrictImplicitExport: Boolean
    get() = getBoolean(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT)
    set(value) { put(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT, value) }

var CompilerConfiguration.optimizeGeneratedJs: Boolean
    get() = getBoolean(JSConfigurationKeys.OPTIMIZE_GENERATED_JS)
    set(value) { put(JSConfigurationKeys.OPTIMIZE_GENERATED_JS, value) }

var CompilerConfiguration.useEs6Classes: Boolean
    get() = getBoolean(JSConfigurationKeys.USE_ES6_CLASSES)
    set(value) { put(JSConfigurationKeys.USE_ES6_CLASSES, value) }

var CompilerConfiguration.includes: String?
    get() = get(JSConfigurationKeys.INCLUDES)
    set(value) { put(JSConfigurationKeys.INCLUDES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.produceKlibFile: Boolean
    get() = getBoolean(JSConfigurationKeys.PRODUCE_KLIB_FILE)
    set(value) { put(JSConfigurationKeys.PRODUCE_KLIB_FILE, value) }

var CompilerConfiguration.produceKlibDir: Boolean
    get() = getBoolean(JSConfigurationKeys.PRODUCE_KLIB_DIR)
    set(value) { put(JSConfigurationKeys.PRODUCE_KLIB_DIR, value) }

var CompilerConfiguration.perModuleOutputName: String?
    get() = get(JSConfigurationKeys.PER_MODULE_OUTPUT_NAME)
    set(value) { putIfNotNull(JSConfigurationKeys.PER_MODULE_OUTPUT_NAME, value) }

var CompilerConfiguration.keep: List<String>
    get() = getList(JSConfigurationKeys.KEEP)
    set(value) { put(JSConfigurationKeys.KEEP, value) }

var CompilerConfiguration.dce: Boolean
    get() = getBoolean(JSConfigurationKeys.DCE)
    set(value) { put(JSConfigurationKeys.DCE, value) }

var CompilerConfiguration.dceRuntimeDiagnostic: String?
    get() = get(JSConfigurationKeys.DCE_RUNTIME_DIAGNOSTIC)
    set(value) { put(JSConfigurationKeys.DCE_RUNTIME_DIAGNOSTIC, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.safeExternalBoolean: Boolean
    get() = getBoolean(JSConfigurationKeys.SAFE_EXTERNAL_BOOLEAN)
    set(value) { put(JSConfigurationKeys.SAFE_EXTERNAL_BOOLEAN, value) }

var CompilerConfiguration.safeExternalBooleanDiagnostic: String?
    get() = get(JSConfigurationKeys.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC)
    set(value) { put(JSConfigurationKeys.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.minimizedMemberNames: Boolean
    get() = getBoolean(JSConfigurationKeys.MINIMIZED_MEMBER_NAMES)
    set(value) { put(JSConfigurationKeys.MINIMIZED_MEMBER_NAMES, value) }

var CompilerConfiguration.callMainMode: String?
    get() = get(JSConfigurationKeys.CALL_MAIN_MODE)
    set(value) { put(JSConfigurationKeys.CALL_MAIN_MODE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.icCacheDirectory: String?
    get() = get(JSConfigurationKeys.IC_CACHE_DIRECTORY)
    set(value) { putIfNotNull(JSConfigurationKeys.IC_CACHE_DIRECTORY, value) }

var CompilerConfiguration.icCacheReadOnly: Boolean
    get() = getBoolean(JSConfigurationKeys.IC_CACHE_READ_ONLY)
    set(value) { put(JSConfigurationKeys.IC_CACHE_READ_ONLY, value) }

var CompilerConfiguration.preserveIcOrder: Boolean
    get() = getBoolean(JSConfigurationKeys.PRESERVE_IC_ORDER)
    set(value) { put(JSConfigurationKeys.PRESERVE_IC_ORDER, value) }

