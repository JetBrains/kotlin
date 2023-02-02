/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.konan.serialization.KonanUserVisibleIrModulesSupport
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.util.removeSuffixIfPresent

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
                overridenProperties
        )
    }

    private val platformManager = PlatformManager(distribution)
    internal val targetManager = platformManager.targetManager(configuration.get(KonanConfigKeys.TARGET))
    internal val target = targetManager.target
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

    private val defaultMemoryModel get() =
        if (target.supportsThreads()) {
            MemoryModel.EXPERIMENTAL
        } else {
            MemoryModel.STRICT
        }

    val memoryModel: MemoryModel by lazy {
        when (configuration.get(BinaryOptions.memoryModel)) {
            MemoryModel.STRICT -> MemoryModel.STRICT
            MemoryModel.RELAXED -> {
                configuration.report(CompilerMessageSeverity.ERROR,
                        "Relaxed MM is deprecated and isn't expected to work right way with current Kotlin version. Using legacy MM.")
                MemoryModel.STRICT
            }
            MemoryModel.EXPERIMENTAL -> {
                if (!target.supportsThreads()) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                            "New MM requires threads, which are not supported on target ${target.name}. Using legacy MM.")
                    MemoryModel.STRICT
                } else {
                    MemoryModel.EXPERIMENTAL
                }
            }
            null -> defaultMemoryModel
        }.also {
            if (it == MemoryModel.EXPERIMENTAL && destroyRuntimeMode == DestroyRuntimeMode.LEGACY) {
                configuration.report(CompilerMessageSeverity.ERROR,
                        "New MM is incompatible with 'legacy' destroy runtime mode.")
            }
        }
    }
    val destroyRuntimeMode: DestroyRuntimeMode get() = configuration.get(KonanConfigKeys.DESTROY_RUNTIME_MODE)!!
    private val defaultGC get() = if (target.supportsThreads()) GC.CONCURRENT_MARK_AND_SWEEP else GC.SAME_THREAD_MARK_AND_SWEEP
    val gc: GC by lazy {
        val configGc = configuration.get(KonanConfigKeys.GARBAGE_COLLECTOR)
        val (gcFallbackReason, realGc) = when {
            configGc == GC.CONCURRENT_MARK_AND_SWEEP && !target.supportsThreads() ->
                "Concurrent mark and sweep gc is not supported for this target. Fallback to Same thread mark and sweep is done" to GC.SAME_THREAD_MARK_AND_SWEEP
            configGc == null -> null to defaultGC
            else -> null to configGc
        }
        if (gcFallbackReason != null) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, gcFallbackReason)
        }
        realGc
    }
    val runtimeAssertsMode: RuntimeAssertsMode get() = configuration.get(BinaryOptions.runtimeAssertionsMode) ?: RuntimeAssertsMode.IGNORE
    val workerExceptionHandling: WorkerExceptionHandling get() = configuration.get(KonanConfigKeys.WORKER_EXCEPTION_HANDLING) ?: when (memoryModel) {
            MemoryModel.EXPERIMENTAL -> WorkerExceptionHandling.USE_HOOK
            else -> WorkerExceptionHandling.LEGACY
        }
    val runtimeLogs: String? get() = configuration.get(KonanConfigKeys.RUNTIME_LOGS)
    val suspendFunctionsFromAnyThreadFromObjC: Boolean by lazy { configuration.get(BinaryOptions.objcExportSuspendFunctionLaunchThreadRestriction) == ObjCExportSuspendFunctionLaunchThreadRestriction.NONE }
    private val defaultFreezing get() = when (memoryModel) {
        MemoryModel.EXPERIMENTAL -> Freezing.Disabled
        else -> Freezing.Full
    }
    val freezing: Freezing by lazy {
        val freezingMode = configuration.get(BinaryOptions.freezing)
        when {
            freezingMode == null -> defaultFreezing
            memoryModel != MemoryModel.EXPERIMENTAL && freezingMode != Freezing.Full -> {
                configuration.report(
                        CompilerMessageSeverity.ERROR,
                        "`freezing` can only be adjusted with new MM. Falling back to default behavior.")
                Freezing.Full
            }
            memoryModel == MemoryModel.EXPERIMENTAL && freezingMode != Freezing.Disabled -> {
                // INFO because deprecation is currently ignorable via OptIn. Using WARNING will require silencing (for warnings-as-errors)
                // by some compiler flag.
                // TODO: When moving into proper deprecation cycle replace with WARNING.
                configuration.report(
                        CompilerMessageSeverity.INFO,
                        "`freezing` should not be enabled with the new MM. Freezing API is deprecated since 1.7.20. See https://kotlinlang.org/docs/native-migration-guide.html for details"
                )
                freezingMode
            }
            else -> freezingMode
        }
    }
    val sourceInfoType: SourceInfoType
        get() = configuration.get(BinaryOptions.sourceInfoType)
                ?: SourceInfoType.CORESYMBOLICATION.takeIf { debug && target.supportsCoreSymbolication() }
                ?: SourceInfoType.NOOP

    val defaultGCSchedulerType get() = when {
        !target.supportsThreads() -> GCSchedulerType.ON_SAFE_POINTS
        else -> GCSchedulerType.WITH_TIMER
    }

    val gcSchedulerType: GCSchedulerType by lazy {
        configuration.get(BinaryOptions.gcSchedulerType) ?: defaultGCSchedulerType
    }

    val gcMarkSingleThreaded: Boolean
        get() = configuration.get(BinaryOptions.gcMarkSingleThreaded) == true

    val needVerifyIr: Boolean
        get() = configuration.get(KonanConfigKeys.VERIFY_IR) == true

    val needCompilerVerification: Boolean
        get() = configuration.get(KonanConfigKeys.VERIFY_COMPILER) ?:
            (optimizationsEnabled || CompilerVersion.CURRENT.meta != MetaVersion.RELEASE)

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

    fun librariesWithDependencies(moduleDescriptor: ModuleDescriptor?): List<KonanLibrary> {

        if (moduleDescriptor == null) error("purgeUnneeded() only works correctly after resolve is over, and we have successfully marked package files as needed or not needed.")
        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(TopologicalLibraryOrder).map { it as KonanLibrary }
    }

    val shouldCoverSources = configuration.getBoolean(KonanConfigKeys.COVERAGE)
    private val shouldCoverLibraries = !configuration.getList(KonanConfigKeys.LIBRARIES_TO_COVER).isNullOrEmpty()

    private val defaultAllocationMode get() = when {
        memoryModel == MemoryModel.EXPERIMENTAL && target.supportsMimallocAllocator() && sanitizer == null -> {
            AllocationMode.MIMALLOC
        }
        else -> AllocationMode.STD
    }

    val allocationMode by lazy {
        when (configuration.get(KonanConfigKeys.ALLOCATION_MODE)) {
            null -> defaultAllocationMode
            AllocationMode.STD -> AllocationMode.STD
            AllocationMode.MIMALLOC -> {
                if (target.supportsMimallocAllocator()) {
                    AllocationMode.MIMALLOC
                } else {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                            "Mimalloc allocator isn't supported on target ${target.name}. Used standard mode.")
                    AllocationMode.STD
                }
            }
            AllocationMode.CUSTOM -> {
                if (gc != GC.CONCURRENT_MARK_AND_SWEEP) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                            "Custom allocator is currently only integrated with concurrent mark and sweep gc. Performance will not be ideal with selected gc.")
                }
                AllocationMode.CUSTOM
            }
        }
    }

    internal val runtimeNativeLibraries: List<String> = mutableListOf<String>().apply {
        if (debug) add("debug.bc")
        when (memoryModel) {
            MemoryModel.STRICT -> {
                add("strict.bc")
                add("legacy_memory_manager.bc")
            }
            MemoryModel.RELAXED -> {
                add("relaxed.bc")
                add("legacy_memory_manager.bc")
            }
            MemoryModel.EXPERIMENTAL -> {
                add("common_gc.bc")
                add("experimental_memory_manager.bc")
                when (gc) {
                    GC.SAME_THREAD_MARK_AND_SWEEP -> {
                        add("same_thread_ms_gc.bc")
                    }
                    GC.NOOP -> {
                        add("noop_gc.bc")
                    }
                    GC.CONCURRENT_MARK_AND_SWEEP -> {
                        if (allocationMode == AllocationMode.CUSTOM) {
                            add("concurrent_ms_gc_custom.bc")
                        } else {
                            add("concurrent_ms_gc.bc")
                        }
                    }
                }
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

    private val defaultPropertyLazyInitialization get() = when (memoryModel) {
        MemoryModel.EXPERIMENTAL -> true
        else -> false
    }
    internal val propertyLazyInitialization: Boolean get() = configuration.get(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION) ?:
            defaultPropertyLazyInitialization

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

    internal val partialLinkage = configuration.get(KonanConfigKeys.PARTIAL_LINKAGE) == true

    internal val additionalCacheFlags by lazy { platformManager.loader(target).additionalCacheFlags }

    private fun StringBuilder.appendCommonCacheFlavor() {
        append(target.toString())
        if (debug) append("-g")
        append("STATIC")

        if (memoryModel != defaultMemoryModel)
            append("-mm=$memoryModel")
        if (freezing != defaultFreezing)
            append("-freezing=${freezing.name}")
        if (propertyLazyInitialization != defaultPropertyLazyInitialization)
            append("-lazy_init=${if (propertyLazyInitialization) "enable" else "disable"}")
    }

    private val systemCacheFlavorString = buildString {
        appendCommonCacheFlavor()

        if (useDebugInfoInNativeLibs)
            append("-runtime_debug")
        if (allocationMode != defaultAllocationMode)
            append("-allocator=${allocationMode.name}")
        if (memoryModel == MemoryModel.EXPERIMENTAL && gc != defaultGC)
            append("-gc=${gc.name}")
        if (memoryModel == MemoryModel.EXPERIMENTAL && gcSchedulerType != defaultGCSchedulerType)
            append("-gc-scheduler=${gcSchedulerType.name}")
        if (runtimeAssertsMode != RuntimeAssertsMode.IGNORE)
            append("-runtime_asserts=${runtimeAssertsMode.name}")
    }

    private val userCacheFlavorString = buildString {
        appendCommonCacheFlavor()

        if (partialLinkage) append("-pl")
    }

    private val systemCacheRootDirectory = File(distribution.konanHome).child("klib").child("cache")
    internal val systemCacheDirectory = systemCacheRootDirectory.child(systemCacheFlavorString)
    private val autoCacheRootDirectory = configuration.get(KonanConfigKeys.AUTO_CACHE_DIR)?.let { File(it) } ?: systemCacheRootDirectory
    internal val autoCacheDirectory = autoCacheRootDirectory.child(userCacheFlavorString)

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

    val infoArgsOnly = (configuration.kotlinSourceRoots.isEmpty()
            && configuration[KonanConfigKeys.INCLUDED_LIBRARIES].isNullOrEmpty()
            && configuration[KonanConfigKeys.EXPORTED_LIBRARIES].isNullOrEmpty()
            && libraryToCache == null)

    val preview19LLVMPipeline = configuration.get(BinaryOptions.preview19LLVMPipeline) ?: false

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
}

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String)
    = this.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)
