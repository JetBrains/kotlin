/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.visibleName

fun CompilerConfiguration.setupFromArguments(arguments: K2NativeCompilerArguments) = with(KonanConfigKeys) {
    val commonSources = arguments.commonSources?.toSet().orEmpty().map { it.absoluteNormalizedFile() }
    val hmppModuleStructure = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
    arguments.freeArgs.forEach {
        addKotlinSourceRoot(it, isCommon = it.absoluteNormalizedFile() in commonSources, hmppModuleStructure?.getModuleNameForSource(it))
    }

    // Can be overwritten by explicit arguments below.
    parseBinaryOptions(arguments, this@setupFromArguments).forEach { optionWithValue ->
        put(optionWithValue)
    }

    arguments.kotlinHome?.let { put(KONAN_HOME, it) }

    put(NODEFAULTLIBS, arguments.nodefaultlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOENDORSEDLIBS, arguments.noendorsedlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOSTDLIB, arguments.nostdlib || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOPACK, arguments.nopack)
    put(NOMAIN, arguments.nomain)
    put(LIBRARY_FILES, arguments.libraries.toNonNullList())
    put(LINKER_ARGS, arguments.linkerArguments.toNonNullList() +
            arguments.singleLinkerArguments.toNonNullList())
    arguments.moduleName?.let { put(MODULE_NAME, it) }

    // TODO: allow overriding the prefix directly.
    arguments.moduleName?.let { put(FULL_EXPORTED_NAME_PREFIX, it) }

    arguments.target?.let { put(TARGET, it) }

    put(INCLUDED_BINARY_FILES, arguments.includeBinaries.toNonNullList())
    put(NATIVE_LIBRARY_FILES, arguments.nativeLibraries.toNonNullList())
    put(REPOSITORIES, arguments.repositories.toNonNullList())

    // TODO: Collect all the explicit file names into an object
    // and teach the compiler to work with temporaries and -save-temps.

    arguments.outputName?.let { put(OUTPUT, it) }
    val outputKind = CompilerOutputKind.valueOf(
            (arguments.produce ?: "program").uppercase())
    put(PRODUCE, outputKind)
    put(METADATA_KLIB, arguments.metadataKlib)

    arguments.libraryVersion?.let { put(LIBRARY_VERSION, it) }

    arguments.mainPackage?.let { put(ENTRY, it) }
    arguments.manifestFile?.let { put(MANIFEST_FILE, it) }
    arguments.runtimeFile?.let { put(RUNTIME_FILE, it) }
    arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }
    put(SAVE_LLVM_IR, arguments.saveLlvmIrAfter.toList())

    put(LIST_TARGETS, arguments.listTargets)
    put(OPTIMIZATION, arguments.optimization)
    put(DEBUG, arguments.debug)
    // TODO: remove after 1.4 release.
    if (arguments.lightDebugDeprecated) {
        report(WARNING,
                "-Xg0 is now deprecated and skipped by compiler. Light debug information is enabled by default for Darwin platforms." +
                        " For other targets, please, use `-Xadd-light-debug=enable` instead.")
    }
    putIfNotNull(LIGHT_DEBUG, when (val it = arguments.lightDebugString) {
        "enable" -> true
        "disable" -> false
        null -> null
        else -> {
            report(ERROR, "Unsupported -Xadd-light-debug= value: $it. Possible values are 'enable'/'disable'")
            null
        }
    })
    putIfNotNull(GENERATE_DEBUG_TRAMPOLINE, when (val it = arguments.generateDebugTrampolineString) {
        "enable" -> true
        "disable" -> false
        null -> null
        else -> {
            report(ERROR, "Unsupported -Xg-generate-debug-tramboline= value: $it. Possible values are 'enable'/'disable'")
            null
        }
    })
    put(STATIC_FRAMEWORK, selectFrameworkType(this@setupFromArguments, arguments, outputKind))
    put(OVERRIDE_CLANG_OPTIONS, arguments.clangOptions.toNonNullList())

    put(EXPORT_KDOC, arguments.exportKDoc)

    put(PRINT_IR, arguments.printIr)
    put(PRINT_BITCODE, arguments.printBitCode)
    put(CHECK_EXTERNAL_CALLS, arguments.checkExternalCalls)
    put(PRINT_FILES, arguments.printFiles)

    put(PURGE_USER_LIBS, arguments.purgeUserLibs)

    if (arguments.verifyCompiler != null)
        put(VERIFY_COMPILER, arguments.verifyCompiler == "true")
    put(VERIFY_IR, when (arguments.verifyIr) {
        null -> IrVerificationMode.NONE
        "none" -> IrVerificationMode.NONE
        "warning" -> IrVerificationMode.WARNING
        "error" -> IrVerificationMode.ERROR
        else -> {
            report(ERROR, "Unsupported IR verification mode ${arguments.verifyIr}")
            IrVerificationMode.NONE
        }
    })
    put(VERIFY_BITCODE, arguments.verifyBitCode)

    put(ENABLE_ASSERTIONS, arguments.enableAssertions)

    val memoryModelFromArgument = when (arguments.memoryModel) {
        "relaxed" -> MemoryModel.RELAXED
        "strict" -> MemoryModel.STRICT
        "experimental" -> MemoryModel.EXPERIMENTAL
        null -> null
        else -> {
            report(ERROR, "Unsupported memory model ${arguments.memoryModel}")
            null
        }
    }

    // TODO: revise priority and/or report conflicting values.
    if (get(BinaryOptions.memoryModel) == null) {
        putIfNotNull(BinaryOptions.memoryModel, memoryModelFromArgument)
    }

    when {
        arguments.generateWorkerTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.WORKER)
        arguments.generateTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD)
        arguments.generateNoExitTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD_NO_EXIT)
        else -> put(GENERATE_TEST_RUNNER, TestRunnerKind.NONE)
    }
    // We need to download dependencies only if we use them ( = there are files to compile).
    put(CHECK_DEPENDENCIES,
            kotlinSourceRoots.isNotEmpty()
                    || !arguments.includes.isNullOrEmpty()
                    || !arguments.exportedLibraries.isNullOrEmpty()
                    || outputKind.isCache
                    || arguments.checkDependencies
    )
    if (arguments.friendModules != null)
        put(FRIEND_MODULES, arguments.friendModules!!.split(File.pathSeparator).filterNot(String::isEmpty))

    if (arguments.refinesPaths != null)
        put(REFINES_MODULES, arguments.refinesPaths!!.filterNot(String::isEmpty))

    put(EXPORTED_LIBRARIES, selectExportedLibraries(this@setupFromArguments, arguments, outputKind))
    put(INCLUDED_LIBRARIES, selectIncludes(this@setupFromArguments, arguments, outputKind))
    put(FRAMEWORK_IMPORT_HEADERS, arguments.frameworkImportHeaders.toNonNullList())
    arguments.emitLazyObjCHeader?.let { put(EMIT_LAZY_OBJC_HEADER_FILE, it) }

    put(BITCODE_EMBEDDING_MODE, selectBitcodeEmbeddingMode(this@setupFromArguments, arguments))
    put(DEBUG_INFO_VERSION, arguments.debugInfoFormatVersion.toInt())
    put(COVERAGE, arguments.coverage)
    put(LIBRARIES_TO_COVER, arguments.coveredLibraries.toNonNullList())
    arguments.coverageFile?.let { put(PROFRAW_PATH, it) }
    put(OBJC_GENERICS, !arguments.noObjcGenerics)
    put(DEBUG_PREFIX_MAP, parseDebugPrefixMap(arguments, this@setupFromArguments))

    val libraryToAddToCache = parseLibraryToAddToCache(arguments, this@setupFromArguments, outputKind)
    if (libraryToAddToCache != null && !arguments.outputName.isNullOrEmpty())
        report(ERROR, "${K2NativeCompilerArguments.ADD_CACHE} already implicitly sets output file name")
    libraryToAddToCache?.let { put(LIBRARY_TO_ADD_TO_CACHE, it) }
    put(CACHED_LIBRARIES, parseCachedLibraries(arguments, this@setupFromArguments))
    put(CACHE_DIRECTORIES, arguments.cacheDirectories.toNonNullList())
    put(AUTO_CACHEABLE_FROM, arguments.autoCacheableFrom.toNonNullList())
    arguments.autoCacheDir?.let { put(AUTO_CACHE_DIR, it) }
    arguments.filesToCache?.let { put(FILES_TO_CACHE, it.toList()) }
    put(MAKE_PER_FILE_CACHE, arguments.makePerFileCache)
    val nThreadsRaw = parseBackendThreads(arguments.backendThreads)
    val availableProcessors = Runtime.getRuntime().availableProcessors()
    val nThreads = if (nThreadsRaw == 0) availableProcessors else nThreadsRaw
    if (nThreads > 1) {
        report(LOGGING, "Running backend in parallel with $nThreads threads")
    }
    if (nThreads > availableProcessors) {
        report(WARNING, "The number of threads $nThreads is more than the number of processors $availableProcessors")
    }
    put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, nThreads)

    parseShortModuleName(arguments, this@setupFromArguments, outputKind)?.let {
        put(SHORT_MODULE_NAME, it)
    }
    put(FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)
    putIfNotNull(PRE_LINK_CACHES, parsePreLinkCachesValue(this@setupFromArguments, arguments.preLinkCaches))
    putIfNotNull(OVERRIDE_KONAN_PROPERTIES, parseOverrideKonanProperties(arguments, this@setupFromArguments))
    put(DESTROY_RUNTIME_MODE, when (arguments.destroyRuntimeMode) {
        "legacy" -> DestroyRuntimeMode.LEGACY
        "on-shutdown" -> DestroyRuntimeMode.ON_SHUTDOWN
        else -> {
            report(ERROR, "Unsupported destroy runtime mode ${arguments.destroyRuntimeMode}")
            DestroyRuntimeMode.ON_SHUTDOWN
        }
    })
    putIfNotNull(GARBAGE_COLLECTOR, when (arguments.gc) {
        null -> null
        "noop" -> GC.NOOP
        "stms" -> GC.SAME_THREAD_MARK_AND_SWEEP
        "cms" -> GC.CONCURRENT_MARK_AND_SWEEP
        else -> {
            report(ERROR, "Unsupported GC ${arguments.gc}")
            null
        }
    })
    putIfNotNull(PROPERTY_LAZY_INITIALIZATION, when (arguments.propertyLazyInitialization) {
        null -> null
        "enable" -> true
        "disable" -> false
        else -> {
            report(ERROR, "Expected 'enable' or 'disable' for lazy property initialization")
            false
        }
    })
    putIfNotNull(ALLOCATION_MODE, when (arguments.allocator) {
        null -> null
        "std" -> AllocationMode.STD
        "mimalloc" -> AllocationMode.MIMALLOC
        "custom" -> AllocationMode.CUSTOM
        else -> {
            report(ERROR, "Expected 'std', 'mimalloc', or 'custom' for allocator")
            AllocationMode.STD
        }
    })
    putIfNotNull(WORKER_EXCEPTION_HANDLING, when (arguments.workerExceptionHandling) {
        null -> null
        "legacy" -> WorkerExceptionHandling.LEGACY
        "use-hook" -> WorkerExceptionHandling.USE_HOOK
        else -> {
            report(ERROR, "Unsupported worker exception handling mode ${arguments.workerExceptionHandling}")
            WorkerExceptionHandling.LEGACY
        }
    })
    put(LAZY_IR_FOR_CACHES, when (arguments.lazyIrForCaches) {
        null -> true
        "enable" -> true
        "disable" -> false
        else -> {
            report(ERROR, "Expected 'enable' or 'disable' for lazy IR usage for cached libraries")
            false
        }
    })

    arguments.externalDependencies?.let { put(EXTERNAL_DEPENDENCIES, it) }
    putIfNotNull(LLVM_VARIANT, when (val variant = arguments.llvmVariant) {
        "user" -> LlvmVariant.User
        "dev" -> LlvmVariant.Dev
        null -> null
        else -> {
            val file = File(variant)
            if (!file.exists) {
                report(ERROR, "`-Xllvm-variant` should be `user`, `dev` or an absolute path. Got: $variant")
                null
            } else {
                LlvmVariant.Custom(file)
            }
        }
    })
    putIfNotNull(RUNTIME_LOGS, arguments.runtimeLogs)
    putIfNotNull(BUNDLE_ID, parseBundleId(arguments, outputKind, this@setupFromArguments))
    arguments.testDumpOutputPath?.let { put(TEST_DUMP_OUTPUT_PATH, it) }

    setupPartialLinkageConfig(
            mode = arguments.partialLinkageMode,
            logLevel = arguments.partialLinkageLogLevel,
            compilerModeAllowsUsingPartialLinkage = outputKind != CompilerOutputKind.LIBRARY, // Don't run PL when producing KLIB.
            onWarning = { report(WARNING, it) },
            onError = { report(ERROR, it) }
    )

    put(OMIT_FRAMEWORK_BINARY, arguments.omitFrameworkBinary)
    putIfNotNull(COMPILE_FROM_BITCODE, parseCompileFromBitcode(arguments, this@setupFromArguments, outputKind))
    putIfNotNull(SERIALIZED_DEPENDENCIES, parseSerializedDependencies(arguments, this@setupFromArguments))
    putIfNotNull(SAVE_DEPENDENCIES_PATH, arguments.saveDependenciesPath)
    putIfNotNull(SAVE_LLVM_IR_DIRECTORY, arguments.saveLlvmIrDirectory)
}

private fun String.absoluteNormalizedFile() = java.io.File(this).absoluteFile.normalize()

internal fun CompilerConfiguration.setupCommonOptionsForCaches(konanConfig: KonanConfig) = with(KonanConfigKeys) {
    put(TARGET, konanConfig.target.toString())
    put(DEBUG, konanConfig.debug)
    setupPartialLinkageConfig(konanConfig.partialLinkageConfig)
    putIfNotNull(EXTERNAL_DEPENDENCIES, konanConfig.externalDependenciesFile?.absolutePath)
    put(BinaryOptions.memoryModel, konanConfig.memoryModel)
    put(PROPERTY_LAZY_INITIALIZATION, konanConfig.propertyLazyInitialization)
    put(BinaryOptions.stripDebugInfoFromNativeLibs, !konanConfig.useDebugInfoInNativeLibs)
    put(ALLOCATION_MODE, konanConfig.allocationMode)
    put(GARBAGE_COLLECTOR, konanConfig.gc)
    put(BinaryOptions.gcSchedulerType, konanConfig.gcSchedulerType)
    put(BinaryOptions.freezing, konanConfig.freezing)
    put(BinaryOptions.runtimeAssertionsMode, konanConfig.runtimeAssertsMode)
    put(LAZY_IR_FOR_CACHES, konanConfig.lazyIrForCaches)
}

private fun Array<String>?.toNonNullList() = this?.asList().orEmpty()

private fun selectFrameworkType(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
): Boolean {
    return if (outputKind != CompilerOutputKind.FRAMEWORK && arguments.staticFramework) {
        configuration.report(
                STRONG_WARNING,
                "'${K2NativeCompilerArguments.STATIC_FRAMEWORK_FLAG}' is only supported when producing frameworks, " +
                        "but the compiler is producing ${outputKind.name.lowercase()}"
        )
        false
    } else {
        arguments.staticFramework
    }
}

private fun parsePreLinkCachesValue(
        configuration: CompilerConfiguration,
        value: String?
): Boolean? = when (value) {
    "enable" -> true
    "disable" -> false
    null -> null
    else -> {
        configuration.report(ERROR, "Unsupported `-Xpre-link-caches` value: $value. Possible values are 'enable'/'disable'")
        null
    }
}

private fun selectBitcodeEmbeddingMode(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments
): BitcodeEmbedding.Mode = when {
    arguments.embedBitcodeMarker -> {
        if (arguments.embedBitcode) {
            configuration.report(
                    STRONG_WARNING,
                    "'${K2NativeCompilerArguments.EMBED_BITCODE_FLAG}' is ignored because '${K2NativeCompilerArguments.EMBED_BITCODE_MARKER_FLAG}' is specified"
            )
        }
        BitcodeEmbedding.Mode.MARKER
    }
    arguments.embedBitcode -> {
        BitcodeEmbedding.Mode.FULL
    }
    else -> BitcodeEmbedding.Mode.NONE
}

private fun selectExportedLibraries(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
): List<String> {
    val exportedLibraries = arguments.exportedLibraries?.toList().orEmpty()

    return if (exportedLibraries.isNotEmpty() && outputKind != CompilerOutputKind.FRAMEWORK &&
            outputKind != CompilerOutputKind.STATIC && outputKind != CompilerOutputKind.DYNAMIC) {
        configuration.report(STRONG_WARNING,
                "-Xexport-library is only supported when producing frameworks or native libraries, " +
                        "but the compiler is producing ${outputKind.name.lowercase()}")

        emptyList()
    } else {
        exportedLibraries
    }
}

private fun selectIncludes(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
): List<String> {
    val includes = arguments.includes?.toList().orEmpty()

    return if (includes.isNotEmpty() && outputKind == CompilerOutputKind.LIBRARY) {
        configuration.report(
                ERROR,
                "The ${K2NativeCompilerArguments.INCLUDE_ARG} flag is not supported when producing ${outputKind.name.lowercase()}"
        )
        emptyList()
    } else {
        includes
    }
}

private fun parseCachedLibraries(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): Map<String, String> = arguments.cachedLibraries?.asList().orEmpty().mapNotNull {
    val libraryAndCache = it.split(",")
    if (libraryAndCache.size != 2) {
        configuration.report(
                ERROR,
                "incorrect ${K2NativeCompilerArguments.CACHED_LIBRARY} format: expected '<library>,<cache>', got '$it'"
        )
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()

private fun parseLibraryToAddToCache(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): String? {
    val input = arguments.libraryToAddToCache

    return if (input != null && !outputKind.isCache) {
        configuration.report(ERROR, "${K2NativeCompilerArguments.ADD_CACHE} can't be used when not producing cache")
        null
    } else {
        input
    }
}

private fun parseBackendThreads(stringValue: String): Int {
    val value = stringValue.toIntOrNull()
            ?: throw KonanCompilationException("Cannot parse -Xbackend-threads value: \"$stringValue\". Please use an integer number")
    if (value < 0)
        throw KonanCompilationException("-Xbackend-threads value cannot be negative")
    return value
}

// TODO: Support short names for current module in ObjC export and lift this limitation.
private fun parseShortModuleName(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): String? {
    val input = arguments.shortModuleName

    return if (input != null && outputKind != CompilerOutputKind.LIBRARY) {
        configuration.report(
                STRONG_WARNING,
                "${K2NativeCompilerArguments.SHORT_MODULE_NAME_ARG} is only supported when producing a Kotlin library, " +
                        "but the compiler is producing ${outputKind.name.lowercase()}"
        )
        null
    } else {
        input
    }
}

private fun parseDebugPrefixMap(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): Map<String, String> = arguments.debugPrefixMap?.asList().orEmpty().mapNotNull {
    val libraryAndCache = it.split("=")
    if (libraryAndCache.size != 2) {
        configuration.report(ERROR, "incorrect debug prefix map format: expected '<old>=<new>', got '$it'")
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()

class BinaryOptionWithValue<T : Any>(val option: BinaryOption<T>, val value: T)

private fun <T : Any> CompilerConfiguration.put(binaryOptionWithValue: BinaryOptionWithValue<T>) {
    this.put(binaryOptionWithValue.option.compilerConfigurationKey, binaryOptionWithValue.value)
}

fun parseBinaryOptions(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): List<BinaryOptionWithValue<*>> {
    val keyValuePairs = parseKeyValuePairs(arguments.binaryOptions, configuration) ?: return emptyList()

    return keyValuePairs.mapNotNull { (key, value) ->
        val option = BinaryOptions.getByName(key)
        if (option == null) {
            configuration.report(STRONG_WARNING, "Unknown binary option '$key'")
            null
        } else {
            parseBinaryOption(option, value, configuration)
        }
    }
}

private fun <T : Any> parseBinaryOption(
        option: BinaryOption<T>,
        valueName: String,
        configuration: CompilerConfiguration
): BinaryOptionWithValue<T>? {
    val value = option.valueParser.parse(valueName)
    return if (value == null) {
        configuration.report(STRONG_WARNING, "Unknown value '$valueName' of binary option '${option.name}'. " +
                "Possible values are: ${option.valueParser.validValuesHint}")
        null
    } else {
        BinaryOptionWithValue(option, value)
    }
}

private fun parseOverrideKonanProperties(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): Map<String, String>? = parseKeyValuePairs(arguments.overrideKonanProperties, configuration)

private fun parseKeyValuePairs(
        argumentValue: Array<String>?,
        configuration: CompilerConfiguration
): Map<String, String>? = argumentValue?.mapNotNull {
    val keyValueSeparatorIndex = it.indexOf('=')
    if (keyValueSeparatorIndex > 0) {
        it.substringBefore('=') to it.substringAfter('=')
    } else {
        configuration.report(ERROR, "incorrect property format: expected '<key>=<value>', got '$it'")
        null
    }
}?.toMap()

private fun parseBundleId(
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind,
        configuration: CompilerConfiguration
): String? {
    val argumentValue = arguments.bundleId
    return if (argumentValue != null && outputKind != CompilerOutputKind.FRAMEWORK) {
        configuration.report(STRONG_WARNING, "Setting a bundle ID is only supported when producing a framework " +
                "but the compiler is producing ${outputKind.name.lowercase()}")
        null
    } else {
        argumentValue
    }
}

private fun parseSerializedDependencies(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): String? {
    if (!arguments.serializedDependencies.isNullOrEmpty() && arguments.compileFromBitcode.isNullOrEmpty()) {
        configuration.report(STRONG_WARNING,
                "Providing serialized dependencies only works in conjunction with a bitcode file to compile.")
    }
    return arguments.serializedDependencies
}

private fun parseCompileFromBitcode(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind,
): String? {
    if (!arguments.compileFromBitcode.isNullOrEmpty() && !outputKind.involvesBitcodeGeneration) {
        configuration.report(ERROR,
                "Compilation from bitcode is not available when producing ${outputKind.visibleName}")
    }
    return arguments.compileFromBitcode
}