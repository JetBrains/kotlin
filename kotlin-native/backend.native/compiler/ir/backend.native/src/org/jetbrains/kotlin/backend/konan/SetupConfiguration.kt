/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.getModuleNameForSource
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptionWithValue
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.Freezing
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.config.nativeBinaryOptions.MemoryModel
import org.jetbrains.kotlin.config.nativeBinaryOptions.parseBinaryOptions
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
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
    @Suppress("DEPRECATION")
    put(NOENDORSEDLIBS, arguments.noendorsedlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOSTDLIB, arguments.nostdlib || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOPACK, arguments.nopack)
    put(NOMAIN, arguments.nomain)
    put(LIBRARY_FILES, arguments.libraries.toNonNullList())
    put(LINKER_ARGS, arguments.linkerArguments.toNonNullList() +
            arguments.singleLinkerArguments.toNonNullList())
    arguments.moduleName?.let { put(MODULE_NAME, it) }

    // TODO: allow overriding the prefix directly.
    // With Swift Export, exported prefix must be Kotlin.
    ("Kotlin".takeIf { get(BinaryOptions.swiftExport) == true } ?: arguments.moduleName)?.let { put(FULL_EXPORTED_NAME_PREFIX, it) }

    arguments.target?.let { put(TARGET, it) }

    put(INCLUDED_BINARY_FILES, arguments.includeBinaries.toNonNullList())
    put(NATIVE_LIBRARY_FILES, arguments.nativeLibraries.toNonNullList())

    // TODO: Collect all the explicit file names into an object
    // and teach the compiler to work with temporaries and -save-temps.

    arguments.outputName?.let { put(OUTPUT, it) }
    val outputKind = CompilerOutputKind.valueOf(
            (arguments.produce ?: "program").uppercase())
    put(PRODUCE, outputKind)
    putIfNotNull(HEADER_KLIB, arguments.headerKlibPath)

    arguments.mainPackage?.let { put(ENTRY, it) }
    arguments.manifestFile?.let { put(MANIFEST_FILE, it) }
    arguments.runtimeFile?.let { put(RUNTIME_FILE, it) }
    arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }
    put(SAVE_LLVM_IR, arguments.saveLlvmIrAfter.orEmpty().toList())

    if (arguments.optimization && arguments.debug) {
        report(WARNING, "Unsupported combination of flags: -opt and -g. Please pick one.")
    }

    put(LIST_TARGETS, arguments.listTargets)
    put(OPTIMIZATION, arguments.optimization)
    put(DEBUG, arguments.debug)
    // TODO: remove after 1.4 release.
    @Suppress("DEPRECATION")
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
    put(PRINT_FILES, arguments.printFiles)

    put(PURGE_USER_LIBS, arguments.purgeUserLibs)

    putIfNotNull(WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, arguments.writeDependenciesOfProducedKlibTo)

    if (arguments.verifyCompiler != null)
        put(VERIFY_COMPILER, arguments.verifyCompiler == "true")
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

    get(BinaryOptions.memoryModel)?.also {
        if (it != MemoryModel.EXPERIMENTAL) {
            report(ERROR, "Legacy MM is deprecated and no longer works.")
        } else {
            report(STRONG_WARNING, "-memory-model and memoryModel switches are deprecated and will be removed in a future release.")
        }
    }

    get(BinaryOptions.freezing)?.also {
        if (it != Freezing.Disabled) {
            report(
                    CompilerMessageSeverity.ERROR,
                    "`freezing` is not supported with the new MM. Freezing API is deprecated since 1.7.20. See https://kotlinlang.org/docs/native-migration-guide.html for details"
            )
        } else {
            report(STRONG_WARNING, "freezing switch is deprecated and will be removed in a future release.")
        }
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
                    || (outputKind == CompilerOutputKind.PROGRAM && arguments.libraries?.isNotEmpty() == true)
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

    put(DEBUG_INFO_VERSION, arguments.debugInfoFormatVersion.toInt())
    put(OBJC_GENERICS, !arguments.noObjcGenerics)
    put(DEBUG_PREFIX_MAP, parseDebugPrefixMap(arguments, this@setupFromArguments))

    val libraryToAddToCache = parseLibraryToAddToCache(arguments, this@setupFromArguments, outputKind)
    if (libraryToAddToCache != null && !arguments.outputName.isNullOrEmpty())
        report(ERROR, "${K2NativeCompilerArguments::libraryToAddToCache.cliArgument} already implicitly sets output file name")
    libraryToAddToCache?.let { put(LIBRARY_TO_ADD_TO_CACHE, it) }
    put(CACHED_LIBRARIES, parseCachedLibraries(arguments, this@setupFromArguments))
    put(CACHE_DIRECTORIES, arguments.cacheDirectories.toNonNullList())
    put(AUTO_CACHEABLE_FROM, arguments.autoCacheableFrom.toNonNullList())
    arguments.autoCacheDir?.let { put(AUTO_CACHE_DIR, it) }
    val incrementalCacheDir = arguments.incrementalCacheDir
    if ((incrementalCacheDir != null) xor (arguments.incrementalCompilation == true))
        report(ERROR, "For incremental compilation both flags should be supplied: " +
                "-Xenable-incremental-compilation and ${K2NativeCompilerArguments::incrementalCacheDir.cliArgument}")
    incrementalCacheDir?.let { put(INCREMENTAL_CACHE_DIR, it) }
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
    when (arguments.destroyRuntimeMode) {
        null -> {}
        "legacy" -> {
            report(ERROR, "New MM is incompatible with 'legacy' destroy runtime mode.")
        }
        "on-shutdown" -> {
            report(STRONG_WARNING, "-Xdestroy-runtime-mode switch is deprecated and will be removed in a future release.")
        }
        else -> {
            report(ERROR, "Unsupported destroy runtime mode ${arguments.destroyRuntimeMode}")
        }
    }

    val gcFromArgument = when (arguments.gc) {
        null -> null
        "noop" -> GC.NOOP
        "stms" -> GC.STOP_THE_WORLD_MARK_AND_SWEEP
        "cms" -> GC.PARALLEL_MARK_CONCURRENT_SWEEP
        else -> {
            val validValues = enumValues<GC>().joinToString("|") {
                val fullName = "$it".lowercase()
                it.shortcut.let { short ->
                    "$fullName (or: $short)"
                }
            }
            report(ERROR, "Unsupported argument -Xgc=${arguments.gc}. Use -Xbinary=gc= with values ${validValues}")
            null
        }
    }
    if (gcFromArgument != null) {
        val newValue = gcFromArgument.shortcut
        report(WARNING, "-Xgc=${arguments.gc} compiler argument is deprecated. Use -Xbinary=gc=${newValue} instead")
    }
    // TODO: revise priority and/or report conflicting values.
    if (get(BinaryOptions.gc) == null) {
        putIfNotNull(BinaryOptions.gc, gcFromArgument)
    }

    if (arguments.checkExternalCalls) {
        report(WARNING, "-Xcheck-state-at-external-calls compiler argument is deprecated. Use -Xbinary=checkStateAtExternalCalls=true instead")
    }
    // TODO: revise priority and/or report conflicting values.
    if (get(BinaryOptions.checkStateAtExternalCalls) == null) {
        putIfNotNull(BinaryOptions.checkStateAtExternalCalls, arguments.checkExternalCalls)
    }

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
        "mimalloc" -> {
            report(ERROR, "Usage of mimalloc in Kotlin/Native compiler is deprecated. Please remove -Xallocator=mimalloc compiler flag.")
            AllocationMode.CUSTOM
        }
        "custom" -> AllocationMode.CUSTOM
        else -> {
            report(ERROR, "Expected 'std', or 'custom' for allocator")
            AllocationMode.CUSTOM
        }
    })
    when (arguments.workerExceptionHandling) {
        null -> {}
        "legacy" -> {
            report(ERROR, "Legacy exception handling in workers is deprecated")
        }
        "use-hook" -> {
            report(STRONG_WARNING, "-Xworker-exception-handling is deprecated")
        }
        else -> {
            report(ERROR, "Unsupported worker exception handling mode ${arguments.workerExceptionHandling}")
        }
    }
    put(LAZY_IR_FOR_CACHES, when (arguments.lazyIrForCaches) {
        null -> false
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
    putIfNotNull(KONAN_DATA_DIR, arguments.konanDataDir)

    if (arguments.manifestNativeTargets != null)
        putIfNotNull(MANIFEST_NATIVE_TARGETS, parseManifestNativeTargets(arguments.manifestNativeTargets!!))

    putIfNotNull(LLVM_MODULE_PASSES, arguments.llvmModulePasses)
    putIfNotNull(LLVM_LTO_PASSES, arguments.llvmLTOPasses)
}

private fun String.absoluteNormalizedFile() = java.io.File(this).absoluteFile.normalize()

internal fun CompilerConfiguration.setupCommonOptionsForCaches(konanConfig: KonanConfig) = with(KonanConfigKeys) {
    put(TARGET, konanConfig.target.toString())
    put(DEBUG, konanConfig.debug)
    setupPartialLinkageConfig(konanConfig.partialLinkageConfig)
    putIfNotNull(EXTERNAL_DEPENDENCIES, konanConfig.externalDependenciesFile?.absolutePath)
    put(PROPERTY_LAZY_INITIALIZATION, konanConfig.propertyLazyInitialization)
    put(BinaryOptions.stripDebugInfoFromNativeLibs, !konanConfig.useDebugInfoInNativeLibs)
    put(ALLOCATION_MODE, konanConfig.allocationMode)
    put(BinaryOptions.gc, konanConfig.gc)
    put(BinaryOptions.gcSchedulerType, konanConfig.gcSchedulerType)
    put(BinaryOptions.runtimeAssertionsMode, konanConfig.runtimeAssertsMode)
    put(LAZY_IR_FOR_CACHES, konanConfig.lazyIrForCaches)
    put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, konanConfig.threadsCount)
    putIfNotNull(KONAN_DATA_DIR, konanConfig.distribution.localKonanDir.absolutePath)
    putIfNotNull(BinaryOptions.minidumpLocation, konanConfig.minidumpLocation)
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
                "'${K2NativeCompilerArguments::staticFramework.cliArgument}' is only supported when producing frameworks, " +
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
                "The ${K2NativeCompilerArguments::includes.cliArgument} flag is not supported when producing ${outputKind.name.lowercase()}"
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
                "incorrect ${K2NativeCompilerArguments::cachedLibraries.cliArgument} format: expected '<library>,<cache>', got '$it'"
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
        configuration.report(ERROR, "${K2NativeCompilerArguments::libraryToAddToCache.cliArgument} can't be used when not producing cache")
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
                "${K2NativeCompilerArguments::shortModuleName.cliArgument} is only supported when producing a Kotlin library, " +
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

private fun <T : Any> CompilerConfiguration.put(binaryOptionWithValue: BinaryOptionWithValue<T>) {
    this.put(binaryOptionWithValue.compilerConfigurationKey, binaryOptionWithValue.value)
}

fun parseBinaryOptions(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): List<BinaryOptionWithValue<*>> = parseBinaryOptions(
        arguments.binaryOptions,
        reportWarning = { configuration.report(STRONG_WARNING, it) },
        reportError = { configuration.report(ERROR, it) },
)

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

private fun CompilerConfiguration.parseManifestNativeTargets(targetStrings: Array<String>): Collection<KonanTarget> {
    val trimmedTargetStrings = targetStrings.map { it.trim() }
    val (recognizedTargetNames, unrecognizedTargetNames) = trimmedTargetStrings.partition { it in KonanTarget.predefinedTargets.keys }

    if (unrecognizedTargetNames.isNotEmpty()) {
        report(
                WARNING,
                """
                    The following target names passed to the -Xmanifest-native-targets are not recognized:
                    ${unrecognizedTargetNames.joinToString(separator = ", ")}
                    
                    List of known target names:
                    ${KonanTarget.predefinedTargets.keys.joinToString(separator = ", ")}
                """.trimIndent()
        )
    }

    return recognizedTargetNames.map { KonanTarget.predefinedTargets[it]!! }
}
