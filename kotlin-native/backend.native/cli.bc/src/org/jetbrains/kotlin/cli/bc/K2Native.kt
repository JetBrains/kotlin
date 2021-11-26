/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.KotlinPaths

private class K2NativeCompilerPerformanceManager: CommonCompilerPerformanceManager("Kotlin to Native Compiler")
class K2Native : CLICompiler<K2NativeCompilerArguments>() {

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeCompilerArguments) {}

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = KlibMetadataVersion(*versionArray)

    override val defaultPerformanceManager: CommonCompilerPerformanceManager by lazy {
        K2NativeCompilerPerformanceManager()
    }

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${CompilerVersion.CURRENT}")
            return ExitCode.OK
        }

        val pluginLoadResult =
            PluginCliParser.loadPluginsSafe(arguments.pluginClasspaths, arguments.pluginOptions, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
            configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)
        val project = environment.project
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE
        configuration.put(CLIConfigurationKeys.PHASE_CONFIG, createPhaseConfig(toplevelPhase, arguments, messageCollector))

        val enoughArguments = arguments.freeArgs.isNotEmpty() || arguments.isUsefulWithoutFreeArgs
        if (!enoughArguments) {
            configuration.report(ERROR, "You have not specified any compilation arguments. No output has been produced.")
        }

        /* Set default version of metadata version */
        val metadataVersionString = arguments.metadataVersion
        if (metadataVersionString == null) {
            configuration.put(CommonConfigurationKeys.METADATA_VERSION, KlibMetadataVersion.INSTANCE)
        }

        try {
            val konanConfig = KonanConfig(project, configuration)
            ensureModuleName(konanConfig, environment)
            runTopLevelPhases(konanConfig, environment)
        } catch (e: Throwable) {
            if (e is KonanCompilationException || e is CompilationErrorException)
                return ExitCode.COMPILATION_ERROR

            configuration.report(ERROR, """
                |Compilation failed: ${e.message}

                | * Source files: ${environment.getSourceFiles().joinToString(transform = KtFile::getName)}
                | * Compiler version info: Konan: ${CompilerVersion.CURRENT} / Kotlin: ${KotlinVersion.CURRENT}
                | * Output kind: ${configuration.get(KonanConfigKeys.PRODUCE)}

                """.trimMargin())
            throw e
        }

        return ExitCode.OK
    }

    val K2NativeCompilerArguments.isUsefulWithoutFreeArgs: Boolean
        get() = listTargets || listPhases || checkDependencies || !includes.isNullOrEmpty() ||
                !librariesToCache.isNullOrEmpty() || libraryToAddToCache != null || !exportedLibraries.isNullOrEmpty()

    fun Array<String>?.toNonNullList(): List<String> {
        return this?.asList<String>() ?: listOf<String>()
    }

    private fun ensureModuleName(config: KonanConfig, environment: KotlinCoreEnvironment) {
        if (environment.getSourceFiles().isEmpty()) {
            val libraries = config.resolvedLibraries.getFullList()
            val moduleName = config.moduleId
            if (libraries.any { it.uniqueName == moduleName }) {
                val kexeModuleName = "${moduleName}_kexe"
                config.configuration.put(KonanConfigKeys.MODULE_NAME, kexeModuleName)
                assert(libraries.none { it.uniqueName == kexeModuleName })
            }
        }
    }

    // It is executed before doExecute().
    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments    : K2NativeCompilerArguments,
            services     : Services) {

        val commonSources = arguments.commonSources?.toSet().orEmpty()
        arguments.freeArgs.forEach {
            configuration.addKotlinSourceRoot(it, it in commonSources)
        }

        with(KonanConfigKeys) {
            with(configuration) {
                // Can be overwritten by explicit arguments below.
                parseBinaryOptions(arguments, configuration).forEach { optionWithValue ->
                    configuration.put(optionWithValue)
                }

                arguments.kotlinHome?.let { put(KONAN_HOME, it) }

                put(NODEFAULTLIBS, arguments.nodefaultlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
                put(NOENDORSEDLIBS, arguments.noendorsedlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
                put(NOSTDLIB, arguments.nostdlib || !arguments.libraryToAddToCache.isNullOrEmpty())
                put(NOPACK, arguments.nopack)
                put(NOMAIN, arguments.nomain)
                put(LIBRARY_FILES,
                        arguments.libraries.toNonNullList())
                put(LINKER_ARGS, arguments.linkerArguments.toNonNullList() +
                        arguments.singleLinkerArguments.toNonNullList())
                arguments.moduleName?.let{ put(MODULE_NAME, it) }

                // TODO: allow overriding the prefix directly.
                arguments.moduleName?.let { put(FULL_EXPORTED_NAME_PREFIX, it) }

                arguments.target?.let{ put(TARGET, it) }

                put(INCLUDED_BINARY_FILES,
                        arguments.includeBinaries.toNonNullList())
                put(NATIVE_LIBRARY_FILES,
                        arguments.nativeLibraries.toNonNullList())
                put(REPOSITORIES,
                        arguments.repositories.toNonNullList())

                // TODO: Collect all the explicit file names into an object
                // and teach the compiler to work with temporaries and -save-temps.

                arguments.outputName ?.let { put(OUTPUT, it) }
                val outputKind = CompilerOutputKind.valueOf(
                    (arguments.produce ?: "program").uppercase())
                put(PRODUCE, outputKind)
                put(METADATA_KLIB, arguments.metadataKlib)

                arguments.libraryVersion ?. let { put(LIBRARY_VERSION, it) }

                arguments.mainPackage ?.let{ put(ENTRY, it) }
                arguments.manifestFile ?.let{ put(MANIFEST_FILE, it) }
                arguments.runtimeFile ?.let{ put(RUNTIME_FILE, it) }
                arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }
                put(SAVE_LLVM_IR, arguments.saveLlvmIr)

                put(LIST_TARGETS, arguments.listTargets)
                put(OPTIMIZATION, arguments.optimization)
                put(DEBUG, arguments.debug)
                // TODO: remove after 1.4 release.
                if (arguments.lightDebugDeprecated) {
                    configuration.report(WARNING,
                            "-Xg0 is now deprecated and skipped by compiler. Light debug information is enabled by default for Darwin platforms." +
                                    " For other targets, please, use `-Xadd-light-debug=enable` instead.")
                }
                putIfNotNull(LIGHT_DEBUG, when (val it = arguments.lightDebugString) {
                    "enable" -> true
                    "disable" -> false
                    null -> null
                    else -> {
                        configuration.report(ERROR, "Unsupported -Xadd-light-debug= value: $it. Possible values are 'enable'/'disable'")
                        null
                    }
                })
                putIfNotNull(GENERATE_DEBUG_TRAMPOLINE, when (val it = arguments.generateDebugTrampolineString) {
                    "enable" -> true
                    "disable" -> false
                    null -> null
                    else -> {
                        configuration.report(ERROR, "Unsupported -Xg-generate-debug-tramboline= value: $it. Possible values are 'enable'/'disable'")
                        null
                    }
                })
                put(STATIC_FRAMEWORK, selectFrameworkType(configuration, arguments, outputKind))
                put(OVERRIDE_CLANG_OPTIONS, arguments.clangOptions.toNonNullList())

                put(EXPORT_KDOC, arguments.exportKDoc)

                put(PRINT_IR, arguments.printIr)
                put(PRINT_IR_WITH_DESCRIPTORS, arguments.printIrWithDescriptors)
                put(PRINT_DESCRIPTORS, arguments.printDescriptors)
                put(PRINT_LOCATIONS, arguments.printLocations)
                put(PRINT_BITCODE, arguments.printBitCode)
                put(CHECK_EXTERNAL_CALLS, arguments.checkExternalCalls)
                put(PRINT_FILES, arguments.printFiles)

                put(PURGE_USER_LIBS, arguments.purgeUserLibs)

                if (arguments.verifyCompiler != null)
                    put(VERIFY_COMPILER, arguments.verifyCompiler == "true")
                put(VERIFY_IR, arguments.verifyIr)
                put(VERIFY_BITCODE, arguments.verifyBitCode)

                put(ENABLED_PHASES,
                        arguments.enablePhases.toNonNullList())
                put(DISABLED_PHASES,
                        arguments.disablePhases.toNonNullList())
                put(LIST_PHASES, arguments.listPhases)

                put(ENABLE_ASSERTIONS, arguments.enableAssertions)

                val memoryModelFromArgument = when (arguments.memoryModel) {
                    "relaxed" -> MemoryModel.RELAXED
                    "strict" -> MemoryModel.STRICT
                    "experimental" -> MemoryModel.EXPERIMENTAL
                    else -> {
                        configuration.report(ERROR, "Unsupported memory model ${arguments.memoryModel}")
                        MemoryModel.STRICT
                    }
                }

                // TODO: revise priority and/or report conflicting values.
                val memoryModel = get(BinaryOptions.memoryModel) ?: memoryModelFromArgument
                put(BinaryOptions.memoryModel, memoryModel)

                when {
                    arguments.generateWorkerTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.WORKER)
                    arguments.generateTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD)
                    arguments.generateNoExitTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD_NO_EXIT)
                    else -> put(GENERATE_TEST_RUNNER, TestRunnerKind.NONE)
                }
                // We need to download dependencies only if we use them ( = there are files to compile).
                put(
                    CHECK_DEPENDENCIES,
                    configuration.kotlinSourceRoots.isNotEmpty()
                            || !arguments.includes.isNullOrEmpty()
                            || !arguments.exportedLibraries.isNullOrEmpty()
                            || outputKind.isCache
                            || arguments.checkDependencies
                )
                if (arguments.friendModules != null)
                    put(FRIEND_MODULES, arguments.friendModules!!.split(File.pathSeparator).filterNot(String::isEmpty))

                put(EXPORTED_LIBRARIES, selectExportedLibraries(configuration, arguments, outputKind))
                put(INCLUDED_LIBRARIES, selectIncludes(configuration, arguments, outputKind))
                put(FRAMEWORK_IMPORT_HEADERS, arguments.frameworkImportHeaders.toNonNullList())
                arguments.emitLazyObjCHeader?.let { put(EMIT_LAZY_OBJC_HEADER_FILE, it) }

                put(BITCODE_EMBEDDING_MODE, selectBitcodeEmbeddingMode(this, arguments))
                put(DEBUG_INFO_VERSION, arguments.debugInfoFormatVersion.toInt())
                put(COVERAGE, arguments.coverage)
                put(LIBRARIES_TO_COVER, arguments.coveredLibraries.toNonNullList())
                arguments.coverageFile?.let { put(PROFRAW_PATH, it) }
                put(OBJC_GENERICS, !arguments.noObjcGenerics)
                put(DEBUG_PREFIX_MAP, parseDebugPrefixMap(arguments, configuration))

                put(LIBRARIES_TO_CACHE, parseLibrariesToCache(arguments, configuration, outputKind))
                val libraryToAddToCache = parseLibraryToAddToCache(arguments, configuration, outputKind)
                if (libraryToAddToCache != null && !arguments.outputName.isNullOrEmpty())
                    configuration.report(ERROR, "$ADD_CACHE already implicitly sets output file name")
                val cacheDirectories = arguments.cacheDirectories.toNonNullList()
                libraryToAddToCache?.let { put(LIBRARY_TO_ADD_TO_CACHE, it) }
                put(CACHE_DIRECTORIES, cacheDirectories)
                put(CACHED_LIBRARIES, parseCachedLibraries(arguments, configuration))

                parseShortModuleName(arguments, configuration, outputKind)?.let {
                    put(SHORT_MODULE_NAME, it)
                }
                put(FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)
                putIfNotNull(PRE_LINK_CACHES, parsePreLinkCachesValue(configuration, arguments.preLinkCaches))
                putIfNotNull(OVERRIDE_KONAN_PROPERTIES, parseOverrideKonanProperties(arguments, configuration))
                put(DESTROY_RUNTIME_MODE, when (arguments.destroyRuntimeMode) {
                    "legacy" -> DestroyRuntimeMode.LEGACY
                    "on-shutdown" -> DestroyRuntimeMode.ON_SHUTDOWN
                    else -> {
                        configuration.report(ERROR, "Unsupported destroy runtime mode ${arguments.destroyRuntimeMode}")
                        DestroyRuntimeMode.ON_SHUTDOWN
                    }
                })
                val assertGcSupported = {
                    if (memoryModel != MemoryModel.EXPERIMENTAL) {
                        configuration.report(ERROR, "-Xgc is only supported for -memory-model experimental")
                    }
                }
                put(GARBAGE_COLLECTOR, when (arguments.gc) {
                    null -> GC.SAME_THREAD_MARK_AND_SWEEP
                    "noop" -> {
                        assertGcSupported()
                        GC.NOOP
                    }
                    "stms" -> {
                        assertGcSupported()
                        GC.SAME_THREAD_MARK_AND_SWEEP
                    }
                    else -> {
                        configuration.report(ERROR, "Unsupported GC ${arguments.gc}")
                        GC.SAME_THREAD_MARK_AND_SWEEP
                    }
                })
                if (memoryModel != MemoryModel.EXPERIMENTAL && arguments.gcAggressive) {
                    configuration.report(ERROR, "-Xgc-aggressive is only supported for -memory-model experimental")
                }
                put(GARBAGE_COLLECTOR_AGRESSIVE, arguments.gcAggressive)
                put(PROPERTY_LAZY_INITIALIZATION, when (arguments.propertyLazyInitialization) {
                    null -> {
                        when (memoryModel) {
                            MemoryModel.EXPERIMENTAL -> true
                            else -> false
                        }
                    }
                    "enable" -> true
                    "disable" -> false
                    else -> {
                        configuration.report(ERROR, "Expected 'enable' or 'disable' for lazy property initialization")
                        false
                    }
                })
                put(ALLOCATION_MODE, when (arguments.allocator) {
                    null -> {
                        when (memoryModel) {
                            MemoryModel.EXPERIMENTAL -> "mimalloc"
                            else -> "std"
                        }
                    }
                    "std" -> arguments.allocator!!
                    "mimalloc" -> arguments.allocator!!
                    else -> {
                        configuration.report(ERROR, "Expected 'std' or 'mimalloc' for allocator")
                        "std"
                    }
                })
                put(WORKER_EXCEPTION_HANDLING, when (arguments.workerExceptionHandling) {
                    null -> if (memoryModel == MemoryModel.EXPERIMENTAL) WorkerExceptionHandling.USE_HOOK else WorkerExceptionHandling.LEGACY
                    "legacy" -> WorkerExceptionHandling.LEGACY
                    "use-hook" -> WorkerExceptionHandling.USE_HOOK
                    else -> {
                        configuration.report(ERROR, "Unsupported worker exception handling mode ${arguments.workerExceptionHandling}")
                        WorkerExceptionHandling.LEGACY
                    }
                })
                put(LAZY_IR_FOR_CACHES, when (arguments.lazyIrForCaches) {
                    null -> true
                    "enable" -> true
                    "disable" -> false
                    else -> {
                        configuration.report(ERROR, "Expected 'enable' or 'disable' for lazy IR usage for cached libraries")
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
                            configuration.report(ERROR, "`-Xllvm-variant` should be `user`, `dev` or an absolute path. Got: $variant")
                            null
                        } else {
                            LlvmVariant.Custom(file)
                        }
                    }
                })
                putIfNotNull(RUNTIME_LOGS, arguments.runtimeLogs)
                putIfNotNull(BUNDLE_ID, parseBundleId(arguments, outputKind, configuration))
                put(MEANINGFUL_BRIDGE_NAMES, arguments.meaningfulBridgeNames)
            }
        }
    }

    override fun createArguments() = K2NativeCompilerArguments()

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                doMain(K2Native(), args)
            }
        }
        @JvmStatic fun mainNoExit(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }

        @JvmStatic fun mainNoExitWithGradleRenderer(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args, MessageRenderer.GRADLE_STYLE) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }
    }
}

private fun selectFrameworkType(
    configuration: CompilerConfiguration,
    arguments: K2NativeCompilerArguments,
    outputKind: CompilerOutputKind
): Boolean {
    return if (outputKind != CompilerOutputKind.FRAMEWORK && arguments.staticFramework) {
        configuration.report(
            STRONG_WARNING,
            "'$STATIC_FRAMEWORK_FLAG' is only supported when producing frameworks, " +
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
                    "'$EMBED_BITCODE_FLAG' is ignored because '$EMBED_BITCODE_MARKER_FLAG' is specified"
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
            "The $INCLUDE_ARG flag is not supported when producing ${outputKind.name.lowercase()}"
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
                "incorrect $CACHED_LIBRARY format: expected '<library>,<cache>', got '$it'"
        )
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()

private fun parseLibrariesToCache(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): List<String> {
    val input = arguments.librariesToCache?.asList().orEmpty()

    return if (input.isNotEmpty() && !outputKind.isCache) {
        configuration.report(ERROR, "$MAKE_CACHE can't be used when not producing cache")
        emptyList()
    } else if (input.isNotEmpty() && !arguments.libraryToAddToCache.isNullOrEmpty()) {
        configuration.report(ERROR, "supplied both $MAKE_CACHE and $ADD_CACHE options")
        emptyList()
    } else {
        input
    }
}

private fun parseLibraryToAddToCache(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): String? {
    val input = arguments.libraryToAddToCache

    return if (input != null && !outputKind.isCache) {
        configuration.report(ERROR, "$ADD_CACHE can't be used when not producing cache")
        null
    } else {
        input
    }
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
                "$SHORT_MODULE_NAME_ARG is only supported when producing a Kotlin library, " +
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
        configuration.report(
                ERROR,
                "incorrect debug prefix map format: expected '<old>=<new>', got '$it'"
        )
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()

private class BinaryOptionWithValue<T : Any>(val option: BinaryOption<T>, val value: T)

private fun <T : Any> CompilerConfiguration.put(binaryOptionWithValue: BinaryOptionWithValue<T>) {
    this.put(binaryOptionWithValue.option.compilerConfigurationKey, binaryOptionWithValue.value)
}

private fun parseBinaryOptions(
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
        configuration.report(
                ERROR,
                "incorrect property format: expected '<key>=<value>', got '$it'"
        )
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

fun main(args: Array<String>) = K2Native.main(args)
fun mainNoExitWithGradleRenderer(args: Array<String>) = K2Native.mainNoExitWithGradleRenderer(args)
