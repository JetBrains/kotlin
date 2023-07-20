/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.google.common.base.StandardSystemProperty
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.linkage.issues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.konan.serialization.KonanUserVisibleIrModulesSupport
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.nio.file.Files
import java.nio.file.Paths

enum class IrVerificationMode {
    NONE,
    WARNING,
    ERROR
}

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {
    internal val distribution = run {
        val overridenProperties = mutableMapOf<String, String>().apply {
            configuration.get(KonanConfigKeys.OVERRIDE_KONAN_PROPERTIES)?.let(this::putAll)
            configuration.get(KonanConfigKeys.LLVM_VARIANT)?.getKonanPropertiesEntry()?.let { (key, value) ->
                put(key, value)
            }
        }

        Distribution(
                configuration.get(KonanConfigKeys.KONAN_HOME) ?: KonanHomeProvider.determineKonanHome(),
                false,
                configuration.get(KonanConfigKeys.RUNTIME_FILE),
                overridenProperties,
                configuration.get(KonanConfigKeys.KONAN_DATA_DIR)
        )
    }

    private val platformManager = PlatformManager(distribution)
    internal val targetManager = platformManager.targetManager(configuration.get(KonanConfigKeys.TARGET))
    internal val target = targetManager.target.also { target ->
        require(target.supportsThreads()) { "All supported targets must have threads, but was given $target" }
    }
    val targetHasAddressDependency get() = target.hasAddressDependencyInMemoryModel()
    internal val flexiblePhaseConfig = configuration.get(CLIConfigurationKeys.FLEXIBLE_PHASE_CONFIG)!!

    // TODO: debug info generation mode and debug/release variant selection probably requires some refactoring.
    val debug: Boolean get() = configuration.getBoolean(KonanConfigKeys.DEBUG)
    val lightDebug: Boolean = configuration.get(KonanConfigKeys.LIGHT_DEBUG)
            ?: target.family.isAppleFamily // Default is true for Apple targets.
    val generateDebugTrampoline = debug && configuration.get(KonanConfigKeys.GENERATE_DEBUG_TRAMPOLINE) ?: false
    val optimizationsEnabled = configuration.getBoolean(KonanConfigKeys.OPTIMIZATION)
    val sanitizer = configuration.get(BinaryOptions.sanitizer)?.takeIf {
        when {
            it != SanitizerKind.THREAD -> "${it.name} sanitizer is not supported yet"
            produce == CompilerOutputKind.STATIC -> "${it.name} sanitizer is unsupported for static library"
            produce == CompilerOutputKind.FRAMEWORK && produceStaticFramework -> "${it.name} sanitizer is unsupported for static framework"
            it !in target.supportedSanitizers() -> "${it.name} sanitizer is unsupported on ${target.name}"
            else -> null
        }?.let {message ->
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
            return@takeIf false
        }
        return@takeIf true
    }

    val memoryModel: MemoryModel get() = configuration.get(BinaryOptions.memoryModel)?.also {
        if (it != MemoryModel.EXPERIMENTAL) {
            configuration.report(CompilerMessageSeverity.ERROR, "Legacy MM is deprecated and no longer works.")
        } else {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "-memory-model and memoryModel switches are deprecated and will be removed in a future release.")
        }
    }.let { MemoryModel.EXPERIMENTAL }
    val destroyRuntimeMode: DestroyRuntimeMode get() = configuration.get(KonanConfigKeys.DESTROY_RUNTIME_MODE)?.also {
        if (it != DestroyRuntimeMode.ON_SHUTDOWN) {
            configuration.report(CompilerMessageSeverity.ERROR, "New MM is incompatible with 'legacy' destroy runtime mode.")
        } else {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "-Xdestroy-runtime-mode switch is deprecated and will be removed in a future release.")
        }
    }.let { DestroyRuntimeMode.ON_SHUTDOWN }
    private val defaultGC get() = GC.PARALLEL_MARK_CONCURRENT_SWEEP
    val gc: GC get() = configuration.get(BinaryOptions.gc) ?: defaultGC
    val runtimeAssertsMode: RuntimeAssertsMode get() = configuration.get(BinaryOptions.runtimeAssertionsMode) ?: RuntimeAssertsMode.IGNORE
    private val defaultDisableMmap get() = target.family == Family.MINGW
    val disableMmap: Boolean by lazy {
        when (configuration.get(BinaryOptions.disableMmap)) {
            null -> defaultDisableMmap
            true -> true
            false -> {
                if (target.family == Family.MINGW) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING, "MinGW target does not support mmap/munmap")
                    true
                } else {
                    false
                }
            }
        }
    }
    val workerExceptionHandling: WorkerExceptionHandling get() = configuration.get(KonanConfigKeys.WORKER_EXCEPTION_HANDLING)?.also {
        if (it != WorkerExceptionHandling.USE_HOOK) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Legacy exception handling in workers is deprecated")
        }
    } ?: WorkerExceptionHandling.USE_HOOK
    val runtimeLogs: String? get() = configuration.get(KonanConfigKeys.RUNTIME_LOGS)
    val suspendFunctionsFromAnyThreadFromObjC: Boolean by lazy { configuration.get(BinaryOptions.objcExportSuspendFunctionLaunchThreadRestriction) == ObjCExportSuspendFunctionLaunchThreadRestriction.NONE }
    val freezing: Freezing get() = configuration.get(BinaryOptions.freezing)?.also {
        if (it != Freezing.Disabled) {
            configuration.report(
                    CompilerMessageSeverity.ERROR,
                    "`freezing` is not supported with the new MM. Freezing API is deprecated since 1.7.20. See https://kotlinlang.org/docs/native-migration-guide.html for details"
            )
        } else {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "freezing switch is deprecated and will be removed in a future release.")
        }
    }.let { Freezing.Disabled }
    val sourceInfoType: SourceInfoType
        get() = configuration.get(BinaryOptions.sourceInfoType)
                ?: SourceInfoType.CORESYMBOLICATION.takeIf { debug && target.supportsCoreSymbolication() }
                ?: SourceInfoType.NOOP

    val defaultGCSchedulerType get() =
        when (gc) {
            GC.NOOP -> GCSchedulerType.MANUAL
            else -> GCSchedulerType.ADAPTIVE
        }

    val gcSchedulerType: GCSchedulerType by lazy {
        val arg = configuration.get(BinaryOptions.gcSchedulerType) ?: defaultGCSchedulerType
        arg.deprecatedWithReplacement?.let { replacement ->
            configuration.report(CompilerMessageSeverity.WARNING, "Binary option gcSchedulerType=$arg is deprecated. Use gcSchedulerType=$replacement instead")
            replacement
        } ?: arg
    }

    private val defaultGcMarkSingleThreaded get() = target.family == Family.MINGW

    val gcMarkSingleThreaded: Boolean by lazy {
        configuration.get(BinaryOptions.gcMarkSingleThreaded) ?: defaultGcMarkSingleThreaded
    }

    val concurrentWeakSweep: Boolean
        get() = configuration.get(BinaryOptions.concurrentWeakSweep) ?: false

    val gcMutatorsCooperate: Boolean by lazy {
        val mutatorsCooperate = configuration.get(BinaryOptions.gcMutatorsCooperate)
        if (gcMarkSingleThreaded) {
            if (mutatorsCooperate == true) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Mutators cooperation is not supported during single threaded mark")
            }
            false
        } else {
            mutatorsCooperate ?: true
        }
    }

    val auxGCThreads: UInt by lazy {
        val auxGCThreads = configuration.get(BinaryOptions.auxGCThreads)
        if (gcMarkSingleThreaded) {
            if (auxGCThreads != null && auxGCThreads != 0U) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Auxiliary GC workers are not supported during single threaded mark")
            }
            0U
        } else {
            auxGCThreads ?: 0U
        }
    }

    val irVerificationMode: IrVerificationMode
        get() = configuration.getNotNull(KonanConfigKeys.VERIFY_IR)

    val needCompilerVerification: Boolean
        get() = configuration.get(KonanConfigKeys.VERIFY_COMPILER)
                ?: (optimizationsEnabled || !KotlinCompilerVersion.VERSION.isRelease())

    val appStateTracking: AppStateTracking by lazy {
        configuration.get(BinaryOptions.appStateTracking) ?: AppStateTracking.DISABLED
    }


    val mimallocUseDefaultOptions: Boolean by lazy {
        configuration.get(BinaryOptions.mimallocUseDefaultOptions) ?: false
    }

    val mimallocUseCompaction: Boolean by lazy {
        // Turned off by default, because it slows down allocation.
        configuration.get(BinaryOptions.mimallocUseCompaction) ?: false
    }

    val objcDisposeOnMain: Boolean by lazy {
        configuration.get(BinaryOptions.objcDisposeOnMain) ?: true
    }

    init {
        if (!platformManager.isEnabled(target)) {
            error("Target ${target.visibleName} is not available on the ${HostManager.hostName} host")
        }
    }

    val platform = platformManager.platform(target).apply {
        if (configuration.getBoolean(KonanConfigKeys.CHECK_DEPENDENCIES)) {
            downloadDependencies()
        }
    }

    internal val clang = platform.clang
    val indirectBranchesAreAllowed = target != KonanTarget.WASM32
    val threadsAreAllowed = (target != KonanTarget.WASM32) && (target !is KonanTarget.ZEPHYR)

    internal val produce get() = configuration.get(KonanConfigKeys.PRODUCE)!!

    internal val metadataKlib get() = configuration.get(KonanConfigKeys.METADATA_KLIB)!!

    internal val produceStaticFramework get() = configuration.getBoolean(KonanConfigKeys.STATIC_FRAMEWORK)

    internal val purgeUserLibs: Boolean
        get() = configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)

    internal val resolve = KonanLibrariesResolveSupport(
            configuration, target, distribution, resolveManifestDependenciesLenient = metadataKlib
    )

    val resolvedLibraries get() = resolve.resolvedLibraries

    internal val externalDependenciesFile = configuration.get(KonanConfigKeys.EXTERNAL_DEPENDENCIES)?.let(::File)

    internal val userVisibleIrModulesSupport = KonanUserVisibleIrModulesSupport(
            externalDependenciesLoader = UserVisibleIrModulesSupport.ExternalDependenciesLoader.from(
                    externalDependenciesFile = externalDependenciesFile,
                    onMalformedExternalDependencies = { warningMessage ->
                        configuration.report(CompilerMessageSeverity.STRONG_WARNING, warningMessage)
                    }),
            konanKlibDir = File(distribution.klib)
    )

    val fullExportedNamePrefix: String
        get() = configuration.get(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX) ?: implicitModuleName

    val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME) ?: implicitModuleName

    val shortModuleName: String?
        get() = configuration.get(KonanConfigKeys.SHORT_MODULE_NAME)

    fun librariesWithDependencies(): List<KonanLibrary> {
        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(TopologicalLibraryOrder).map { it as KonanLibrary }
    }

    val shouldCoverSources = configuration.getBoolean(KonanConfigKeys.COVERAGE)
    private val shouldCoverLibraries = !configuration.getList(KonanConfigKeys.LIBRARIES_TO_COVER).isNullOrEmpty()

    private val defaultAllocationMode get() =
        if (sanitizer == null)
            AllocationMode.CUSTOM
        else
            AllocationMode.STD

    val allocationMode by lazy {
        when (configuration.get(KonanConfigKeys.ALLOCATION_MODE)) {
            null -> defaultAllocationMode
            AllocationMode.STD -> AllocationMode.STD
            AllocationMode.MIMALLOC -> {
                if (sanitizer != null) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Sanitizers are useful only with the std allocator")
                }
                if (target.supportsMimallocAllocator()) {
                    AllocationMode.MIMALLOC
                } else {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                            "Mimalloc allocator isn't supported on target ${target.name}. Used standard mode.")
                    AllocationMode.STD
                }
            }
            AllocationMode.CUSTOM -> {
                if (sanitizer != null) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Sanitizers are useful only with the std allocator")
                }
                AllocationMode.CUSTOM
            }
        }
    }

    internal val runtimeNativeLibraries: List<String> = mutableListOf<String>().apply {
        if (debug) add("debug.bc")
        add("common_gc.bc")
        add("common_gcScheduler.bc")
        when (gcSchedulerType) {
            GCSchedulerType.MANUAL -> {
                add("manual_gcScheduler.bc")
            }
            GCSchedulerType.ADAPTIVE -> {
                add("adaptive_gcScheduler.bc")
            }
            GCSchedulerType.AGGRESSIVE -> {
                add("aggressive_gcScheduler.bc")
            }
            GCSchedulerType.DISABLED, GCSchedulerType.WITH_TIMER, GCSchedulerType.ON_SAFE_POINTS -> {
                throw IllegalStateException("Deprecated options must have already been handled")
            }
        }
        if (allocationMode == AllocationMode.CUSTOM) {
            add("experimental_memory_manager_custom.bc")
            when (gc) {
                GC.STOP_THE_WORLD_MARK_AND_SWEEP -> add("same_thread_ms_gc_custom.bc")
                GC.NOOP -> add("noop_gc_custom.bc")
                GC.PARALLEL_MARK_CONCURRENT_SWEEP -> add("concurrent_ms_gc_custom.bc")
            }
        } else {
            add("experimental_memory_manager.bc")
            when (gc) {
                GC.STOP_THE_WORLD_MARK_AND_SWEEP -> add("same_thread_ms_gc.bc")
                GC.NOOP -> add("noop_gc.bc")
                GC.PARALLEL_MARK_CONCURRENT_SWEEP -> add("concurrent_ms_gc.bc")
            }
        }
        if (shouldCoverLibraries || shouldCoverSources) add("profileRuntime.bc")
        if (target.supportsCoreSymbolication()) {
            add("source_info_core_symbolication.bc")
        }
        if (target.supportsLibBacktrace()) {
            add("source_info_libbacktrace.bc")
            add("libbacktrace.bc")
        }
        when (allocationMode) {
            AllocationMode.MIMALLOC -> {
                add("opt_alloc.bc")
                add("mimalloc.bc")
            }
            AllocationMode.STD -> {
                add("std_alloc.bc")
            }
            AllocationMode.CUSTOM -> {
                add("custom_alloc.bc")
            }
        }
    }.map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val launcherNativeLibraries: List<String> = distribution.launcherFiles.map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val objCNativeLibrary: String =
            File(distribution.defaultNatives(target)).child("objc.bc").absolutePath

    internal val exceptionsSupportNativeLibrary: String =
            File(distribution.defaultNatives(target)).child("exceptionsSupport.bc").absolutePath

    internal val nativeLibraries: List<String> =
            configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    internal val includeBinaries: List<String> =
            configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    internal val languageVersionSettings =
            configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!

    internal val friendModuleFiles: Set<File> =
            configuration.get(KonanConfigKeys.FRIEND_MODULES)?.map { File(it) }?.toSet() ?: emptySet()

    internal val refinesModuleFiles: Set<File> =
            configuration.get(KonanConfigKeys.REFINES_MODULES)?.map { File(it) }?.toSet().orEmpty()

    internal val manifestProperties = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
        File(it).loadProperties()
    }

    internal val isInteropStubs: Boolean get() = manifestProperties?.getProperty("interop") == "true"

    private val defaultPropertyLazyInitialization = true
    internal val propertyLazyInitialization: Boolean get() = configuration.get(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION)?.also {
        if (!it) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Eager property initialization is deprecated")
        }
    } ?: defaultPropertyLazyInitialization

    internal val lazyIrForCaches: Boolean get() = configuration.get(KonanConfigKeys.LAZY_IR_FOR_CACHES)!!

    internal val entryPointName: String by lazy {
        if (target.family == Family.ANDROID) {
            val androidProgramType = configuration.get(BinaryOptions.androidProgramType)
                    ?: AndroidProgramType.Default
            if (androidProgramType.konanMainOverride != null) {
                return@lazy androidProgramType.konanMainOverride
            }
        }
        "Konan_main"
    }

    internal val unitSuspendFunctionObjCExport: UnitSuspendFunctionObjCExport
        get() = configuration.get(BinaryOptions.unitSuspendFunctionObjCExport) ?: UnitSuspendFunctionObjCExport.DEFAULT

    internal val testDumpFile: File? = configuration[KonanConfigKeys.TEST_DUMP_OUTPUT_PATH]?.let(::File)

    internal val useDebugInfoInNativeLibs= configuration.get(BinaryOptions.stripDebugInfoFromNativeLibs) == false

    internal val partialLinkageConfig = configuration.partialLinkageConfig

    internal val additionalCacheFlags by lazy { platformManager.loader(target).additionalCacheFlags }

    internal val threadsCount = configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1

    private fun StringBuilder.appendCommonCacheFlavor() {
        append(target.toString())
        if (debug) append("-g")
        append("STATIC")

        if (propertyLazyInitialization != defaultPropertyLazyInitialization)
            append("-lazy_init${if (propertyLazyInitialization) "ENABLE" else "DISABLE"}")
    }

    private val systemCacheFlavorString = buildString {
        appendCommonCacheFlavor()

        if (useDebugInfoInNativeLibs)
            append("-runtime_debug")
        if (allocationMode != defaultAllocationMode)
            append("-allocator${allocationMode.name}")
        if (gc != defaultGC)
            append("-gc${gc.name}")
        if (gcSchedulerType != defaultGCSchedulerType)
            append("-gc_scheduler${gcSchedulerType.name}")
        if (runtimeAssertsMode != RuntimeAssertsMode.IGNORE)
            append("-runtime_asserts${runtimeAssertsMode.name}")
        if (disableMmap != defaultDisableMmap)
            append("-disable_mmap${if (disableMmap) "TRUE" else "FALSE"}")
        if (gcMarkSingleThreaded != defaultGcMarkSingleThreaded)
            append("-gc_mark_single_threaded${if (gcMarkSingleThreaded) "TRUE" else "FALSE"}")
    }

    private val userCacheFlavorString = buildString {
        appendCommonCacheFlavor()
        if (partialLinkageConfig.isEnabled) append("-pl")
    }

    private val systemCacheRootDirectory = File(distribution.konanHome).child("klib").child("cache")
    internal val systemCacheDirectory = systemCacheRootDirectory.child(systemCacheFlavorString).also { it.mkdirs() }
    private val autoCacheRootDirectory = configuration.get(KonanConfigKeys.AUTO_CACHE_DIR)?.let {
        File(it).apply {
            if (!isDirectory) configuration.reportCompilationError("auto cache directory $this is not found or is not a directory")
        }
    } ?: systemCacheRootDirectory
    internal val autoCacheDirectory = autoCacheRootDirectory.child(userCacheFlavorString).also { it.mkdirs() }
    private val incrementalCacheRootDirectory = configuration.get(KonanConfigKeys.INCREMENTAL_CACHE_DIR)?.let {
        File(it).apply {
            if (!isDirectory) configuration.reportCompilationError("incremental cache directory $this is not found or is not a directory")
        }
    }
    internal val incrementalCacheDirectory = incrementalCacheRootDirectory?.child(userCacheFlavorString)?.also { it.mkdirs() }

    internal val ignoreCacheReason = when {
        optimizationsEnabled -> "for optimized compilation"
        sanitizer != null -> "with sanitizers enabled"
        runtimeLogs != null -> "with runtime logs"
        else -> null
    }

    internal val cacheSupport = CacheSupport(
            configuration = configuration,
            resolvedLibraries = resolvedLibraries,
            ignoreCacheReason = ignoreCacheReason,
            systemCacheDirectory = systemCacheDirectory,
            autoCacheDirectory = autoCacheDirectory,
            incrementalCacheDirectory = incrementalCacheDirectory,
            target = target,
            produce = produce
    )

    internal val cachedLibraries: CachedLibraries
        get() = cacheSupport.cachedLibraries

    internal val libraryToCache: PartialCacheInfo?
        get() = cacheSupport.libraryToCache

    internal val producePerFileCache
        get() = configuration.get(KonanConfigKeys.MAKE_PER_FILE_CACHE) == true

    val outputPath get() = configuration.get(KonanConfigKeys.OUTPUT)?.removeSuffixIfPresent(produce.suffix(target)) ?: produce.visibleName

    private val implicitModuleName: String
        get() = cacheSupport.libraryToCache?.let {
            if (producePerFileCache)
                CachedLibraries.getPerFileCachedLibraryName(it.klib)
            else
                CachedLibraries.getCachedLibraryName(it.klib)
        }
                ?: File(outputPath).name

    /**
     * Do not compile binary when compiling framework.
     * This is useful when user care only about framework's interface.
     */
    internal val omitFrameworkBinary: Boolean by lazy {
        configuration.getBoolean(KonanConfigKeys.OMIT_FRAMEWORK_BINARY).also {
            if (it && produce != CompilerOutputKind.FRAMEWORK) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Trying to disable framework binary compilation when producing ${produce.name.lowercase()} is meaningless.")
            }
        }
    }

    /**
     * Continue from bitcode. Skips the frontend and codegen phase of the compiler
     * and instead reads the provided bitcode file.
     * This option can be used for continuing the compilation from a previous invocation.
     */
    internal val compileFromBitcode: String? by lazy {
        configuration.get(KonanConfigKeys.COMPILE_FROM_BITCODE)
    }

    /**
     * Path to serialized dependencies to use for bitcode compilation.
     */
    internal val readSerializedDependencies: String? by lazy {
        configuration.get(KonanConfigKeys.SERIALIZED_DEPENDENCIES)
    }

    /**
     * Path to store backend dependency information.
     */
    internal val writeSerializedDependencies: String? by lazy {
        configuration.get(KonanConfigKeys.SAVE_DEPENDENCIES_PATH)
    }

    val infoArgsOnly = (configuration.kotlinSourceRoots.isEmpty()
            && configuration[KonanConfigKeys.INCLUDED_LIBRARIES].isNullOrEmpty()
            && configuration[KonanConfigKeys.EXPORTED_LIBRARIES].isNullOrEmpty()
            && libraryToCache == null && compileFromBitcode.isNullOrEmpty())


    /**
     * Directory to store LLVM IR from -Xsave-llvm-ir-after.
     */
    internal val saveLlvmIrDirectory: java.io.File by lazy {
        val path = configuration.get(KonanConfigKeys.SAVE_LLVM_IR_DIRECTORY)
        if (path == null) {
            val tempDir = Files.createTempDirectory(Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value()!!), /* prefix= */ null).toFile()
            configuration.report(CompilerMessageSeverity.WARNING,
                    "Temporary directory for LLVM IR is ${tempDir.canonicalPath}")
            tempDir
        } else {
            java.io.File(path)
        }
    }
}

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String)
    = this.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)

private fun String.isRelease(): Boolean {
    // major.minor.patch-meta-build where patch, meta and build are optional.
    val versionPattern = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-(\\p{Alpha}*\\p{Alnum}|[\\p{Alpha}-]*))?(?:-(\\d+))?".toRegex()
    val (_, _, _, metaString, build) = versionPattern.matchEntire(this)?.destructured
            ?: throw IllegalStateException("Cannot parse Kotlin/Native version: $this")

    return metaString.isEmpty() && build.isEmpty()
}
