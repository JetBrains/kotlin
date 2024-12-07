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
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.serialization.js.ModuleKind

object JSConfigurationKeys {
    @JvmField
    val TRANSITIVE_LIBRARIES = CompilerConfigurationKey.create<List<String>>("library files for transitive dependencies")

    @JvmField
    val LIBRARIES = CompilerConfigurationKey.create<List<String>>("library file paths")

    @JvmField
    val SOURCE_MAP = CompilerConfigurationKey.create<Boolean>("generate source map")

    @JvmField
    val USE_DEBUGGER_CUSTOM_FORMATTERS = CompilerConfigurationKey.create<Boolean>("add import of debugger custom formatters")

    @JvmField
    val OUTPUT_DIR = CompilerConfigurationKey.create<File>("output directory")

    @JvmField
    val SOURCE_MAP_PREFIX = CompilerConfigurationKey.create<String>("prefix to add to paths in source map")

    @JvmField
    val SOURCE_MAP_SOURCE_ROOTS = CompilerConfigurationKey.create<List<String>>("base directories used to calculate relative paths for source map")

    @JvmField
    val SOURCE_MAP_EMBED_SOURCES = CompilerConfigurationKey.create<SourceMapSourceEmbedding>("embed source files into source map")

    @JvmField
    val SOURCEMAP_NAMES_POLICY = CompilerConfigurationKey.create<SourceMapNamesPolicy>("a policy to generate a mapping from generated identifiers to their corresponding original names")

    @JvmField
    val SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES = CompilerConfigurationKey.create<Boolean>("insert source mappings from libraries even if their sources are unavailable on the end-user machine")

    @JvmField
    val META_INFO = CompilerConfigurationKey.create<Boolean>("generate .meta.js and .kjsm files")

    @JvmField
    val TARGET = CompilerConfigurationKey.create<EcmaVersion>("ECMA version target")

    @JvmField
    val MODULE_KIND = CompilerConfigurationKey.create<ModuleKind>("module kind")

    @JvmField
    val INCREMENTAL_DATA_PROVIDER = CompilerConfigurationKey.create<IncrementalDataProvider>("incremental data provider")

    @JvmField
    val INCREMENTAL_RESULTS_CONSUMER = CompilerConfigurationKey.create<IncrementalResultsConsumer>("incremental results consumer")

    @JvmField
    val INCREMENTAL_NEXT_ROUND_CHECKER = CompilerConfigurationKey.create<IncrementalNextRoundChecker>("incremental compilation next round checker")

    @JvmField
    val FRIEND_PATHS_DISABLED = CompilerConfigurationKey.create<Boolean>("disable support for friend paths")

    @JvmField
    val FRIEND_PATHS = CompilerConfigurationKey.create<List<String>>("friend module paths")

    @JvmField
    val METADATA_ONLY = CompilerConfigurationKey.create<Boolean>("generate .meta.js and .kjsm files only")

    @JvmField
    val DEVELOPER_MODE = CompilerConfigurationKey.create<Boolean>("enables additional checkers")

    @JvmField
    val GENERATE_COMMENTS_WITH_FILE_PATH = CompilerConfigurationKey.create<Boolean>("generate comments with file path at the start of each file block")

    @JvmField
    val GENERATE_POLYFILLS = CompilerConfigurationKey.create<Boolean>("generate polyfills for newest properties, methods and classes from ES6+")

    @JvmField
    val DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS = CompilerConfigurationKey.create<String>("provide platform specific args as a parameter of the main function")

    @JvmField
    val GENERATE_DTS = CompilerConfigurationKey.create<Boolean>("generate TypeScript definition file")

    @JvmField
    val COMPILE_SUSPEND_AS_JS_GENERATOR = CompilerConfigurationKey.create<Boolean>("force suspend functions compilation int JS generator functions")

    @JvmField
    val COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS = CompilerConfigurationKey.create<Boolean>("lower Kotlin lambdas into arrow functions instead of anonymous functions")

    @JvmField
    val GENERATE_REGION_COMMENTS = CompilerConfigurationKey.create<Boolean>("generate special comments at the start and the end of each file block, it allows to fold them and navigate to them in the IDEA")

    @JvmField
    val FILE_PATHS_PREFIX_MAP = CompilerConfigurationKey.create<Map<String, String>>("this map used to shorten/replace prefix of paths in comments with file paths, including region comments")

    @JvmField
    val PRINT_REACHABILITY_INFO = CompilerConfigurationKey.create<Boolean>("print declarations' reachability info during performing DCE")

    @JvmField
    val DUMP_REACHABILITY_INFO_TO_FILE = CompilerConfigurationKey.create<String>("dump declarations' reachability info to file during performing DCE")

    @JvmField
    val FAKE_OVERRIDE_VALIDATOR = CompilerConfigurationKey.create<Boolean>("IR fake override validator")

    @JvmField
    val PROPERTY_LAZY_INITIALIZATION = CompilerConfigurationKey.create<Boolean>("perform lazy initialization for properties")

    @JvmField
    val GENERATE_INLINE_ANONYMOUS_FUNCTIONS = CompilerConfigurationKey.create<Boolean>("translate lambdas into in-line anonymous functions")

    @JvmField
    val GENERATE_STRICT_IMPLICIT_EXPORT = CompilerConfigurationKey.create<Boolean>("enable strict implicitly exported entities types inside d.ts files")

    @JvmField
    val ZIP_FILE_SYSTEM_ACCESSOR = CompilerConfigurationKey.create<ZipFileSystemAccessor>("zip file system accessor, used for klib reading")

    @JvmField
    val OPTIMIZE_GENERATED_JS = CompilerConfigurationKey.create<Boolean>("perform additional optimizations on the generated JS code")

    @JvmField
    val USE_ES6_CLASSES = CompilerConfigurationKey.create<Boolean>("perform ES6 class usage")

}

var CompilerConfiguration.transitiveLibraries: List<String>
    get() = getList(JSConfigurationKeys.TRANSITIVE_LIBRARIES)
    set(value) { put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, value) }

var CompilerConfiguration.libraries: List<String>
    get() = getList(JSConfigurationKeys.LIBRARIES)
    set(value) { put(JSConfigurationKeys.LIBRARIES, value) }

var CompilerConfiguration.sourceMap: Boolean
    get() = getBoolean(JSConfigurationKeys.SOURCE_MAP)
    set(value) { put(JSConfigurationKeys.SOURCE_MAP, value) }

var CompilerConfiguration.useDebuggerCustomFormatters: Boolean
    get() = getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)
    set(value) { put(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS, value) }

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

var CompilerConfiguration.metaInfo: Boolean
    get() = getBoolean(JSConfigurationKeys.META_INFO)
    set(value) { put(JSConfigurationKeys.META_INFO, value) }

var CompilerConfiguration.target: EcmaVersion?
    get() = get(JSConfigurationKeys.TARGET)
    set(value) { put(JSConfigurationKeys.TARGET, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.moduleKind: ModuleKind?
    get() = get(JSConfigurationKeys.MODULE_KIND)
    set(value) { put(JSConfigurationKeys.MODULE_KIND, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.incrementalDataProvider: IncrementalDataProvider?
    get() = get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)
    set(value) { put(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.incrementalResultsConsumer: IncrementalResultsConsumer?
    get() = get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    set(value) { put(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.incrementalNextRoundChecker: IncrementalNextRoundChecker?
    get() = get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER)
    set(value) { put(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.friendPathsDisabled: Boolean
    get() = getBoolean(JSConfigurationKeys.FRIEND_PATHS_DISABLED)
    set(value) { put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, value) }

var CompilerConfiguration.friendPaths: List<String>
    get() = getList(JSConfigurationKeys.FRIEND_PATHS)
    set(value) { put(JSConfigurationKeys.FRIEND_PATHS, value) }

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

var CompilerConfiguration.compileSuspendAsJsGenerator: Boolean
    get() = getBoolean(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR)
    set(value) { put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, value) }

var CompilerConfiguration.compileLambdasAsEs6ArrowFunctions: Boolean
    get() = getBoolean(JSConfigurationKeys.COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS)
    set(value) { put(JSConfigurationKeys.COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS, value) }

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
    set(value) { put(JSConfigurationKeys.DUMP_REACHABILITY_INFO_TO_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

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

var CompilerConfiguration.zipFileSystemAccessor: ZipFileSystemAccessor?
    get() = get(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR)
    set(value) { put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.optimizeGeneratedJs: Boolean
    get() = getBoolean(JSConfigurationKeys.OPTIMIZE_GENERATED_JS)
    set(value) { put(JSConfigurationKeys.OPTIMIZE_GENERATED_JS, value) }

var CompilerConfiguration.useEs6Classes: Boolean
    get() = getBoolean(JSConfigurationKeys.USE_ES6_CLASSES)
    set(value) { put(JSConfigurationKeys.USE_ES6_CLASSES, value) }

