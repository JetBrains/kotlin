/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.google.common.base.StandardSystemProperty
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.linkage.issues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.konan.ir.BridgesPolicy
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCEntryPoints
import org.jetbrains.kotlin.backend.konan.objcexport.readObjCEntryPoints
import org.jetbrains.kotlin.backend.konan.serialization.KonanUserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.konan.serialization.PartialCacheInfo
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    // FIXME () -> AAAAAAAAAAAA
    private val systemCacheFlavor = mutableListOf<() -> String?>()
    private val userCacheFlavor = mutableListOf<() -> String?>()
    private var ignoreCacheReason0: String? = null // FIXME use or delete

    private data class Config<C, D>(val configured: C, val default: D)

    private fun <C, R> option(
            configKey: CompilerConfigurationKey<C>,
            default: () -> R,
            altersSystemCache: Boolean = false,
            altersUserCache: Boolean = false,
            invalidatesCaches: Boolean = false,
            transform: Config<C, R>.() -> R,
    ): PropertyDelegateProvider<KonanConfig, ReadOnlyProperty<KonanConfig, R>> =
            PropertyDelegateProvider { _, property ->
                val configured = configuration.get(configKey)
                val lazyDefault = lazy { default() }
                val lazyValue = lazy { configured?.let { Config<C, R>(it, lazyDefault.value).transform() } ?: lazyDefault.value }

                val flavor = {
                    "-${property.name}${lazyValue.value}" // TODO mangle properly?
                            .takeIf { lazyValue.value != lazyDefault.value }
                }
                if (altersSystemCache) {
                    systemCacheFlavor += flavor
                }
                if (altersUserCache) {
                    userCacheFlavor += flavor
                }

                ReadOnlyProperty { _, _ -> lazyValue.value }
            }

    @JvmName("optionSameType")
    private fun <T> option(
            configKey: CompilerConfigurationKey<T>,
            default: () -> T,
            altersSystemCache: Boolean = false,
            altersUserCache: Boolean = false,
            invalidatesCaches: Boolean = false,
    ) = option<T, T>(
            configKey,
            default,
            altersSystemCache = altersSystemCache,
            altersUserCache = altersUserCache,
            invalidatesCaches = invalidatesCaches
    ) { configured }


    internal val distribution = run {
        val overridenProperties = mutableMapOf<String, String>().apply {
            configuration.get(KonanConfigKeys.OVERRIDE_KONAN_PROPERTIES)?.let(this::putAll)
            configuration.get(KonanConfigKeys.LLVM_VARIANT)?.getKonanPropertiesEntry()?.let { (key, value) ->
                put(key, value)
            }
        }

        Distribution(
                configuration.get(KonanConfigKeys.KONAN_HOME) ?: KotlinNativePaths.homePath.absolutePath,
                false,
                configuration.get(KonanConfigKeys.RUNTIME_FILE),
                overridenProperties,
                configuration.get(KonanConfigKeys.KONAN_DATA_DIR)
        )
    }

    private val platformManager = PlatformManager(distribution)
    internal val targetManager = platformManager.targetManager(configuration.get(KonanConfigKeys.TARGET))
    internal val target = targetManager.target
    internal val phaseConfig = configuration.phaseConfig!!

    // See https://youtrack.jetbrains.com/issue/KT-67692.
    val useLlvmOpaquePointers = true

    // TODO: debug info generation mode and debug/release variant selection probably requires some refactoring.
    val debug: Boolean by option(KonanConfigKeys.DEBUG, default = { false }, altersSystemCache = true, altersUserCache = true)
    // FIXME CompilerConfigurationKey<Boolean?>
    val lightDebug: Boolean by option(KonanConfigKeys.LIGHT_DEBUG, default = { target.family.isAppleFamily }) { configured ?: default }
    val generateDebugTrampoline by option(KonanConfigKeys.GENERATE_DEBUG_TRAMPOLINE, default = { false }) {
        debug && (configured == true)
    }
    val optimizationsEnabled by option(KonanConfigKeys.OPTIMIZATION, default = { false }, invalidatesCaches = true)

    val smallBinary: Boolean by option(BinaryOptions.smallBinary, default = { target.needSmallBinary() || debug })

    // FIXME how about caches?
    val assertsEnabled by option(KonanConfigKeys.ENABLE_ASSERTIONS, default = { false })

    val sanitizer by option<SanitizerKind, SanitizerKind?>(
            BinaryOptions.sanitizer,
            default = { null },
            altersSystemCache = true,
            altersUserCache = true,
    ) {
        val notSupportedReason = when {
            configured != SanitizerKind.THREAD -> "yet"
            produce == CompilerOutputKind.STATIC -> "for static library"
            produce == CompilerOutputKind.FRAMEWORK && produceStaticFramework -> "for static framework"
            configured !in target.supportedSanitizers() -> "on ${target.name}"
            else -> null
        }
        if (notSupportedReason != null) {
            configuration.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "${configured.name} sanitizer is not supported $notSupportedReason"
            )
            return@option default
        }
        return@option configured
    }

    val swiftExport: Boolean by option(BinaryOptions.swiftExport, default = { false }) {
        if (configured && !target.supportsObjcInterop()) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Swift Export cannot be enabled on $target that does not have objc interop")
            false
        } else configured
    }

    val gc: GC by option(
            BinaryOptions.gc,
            default = { if (swiftExport) GC.CONCURRENT_MARK_AND_SWEEP else GC.PARALLEL_MARK_CONCURRENT_SWEEP }, // FIXME something wired was here
            altersSystemCache = true,
    )

    val runtimeAssertsMode: RuntimeAssertsMode by option(
            BinaryOptions.runtimeAssertionsMode,
            default = { RuntimeAssertsMode.IGNORE },
            altersSystemCache = true
    )

    val checkStateAtExternalCalls: Boolean by option(
            BinaryOptions.checkStateAtExternalCalls,
            default = { false },
            altersSystemCache = true,
            altersUserCache = true,
    )
    val disableMmap: Boolean by option(
            BinaryOptions.disableMmap,
            default = { target.family == Family.MINGW || !pagedAllocator },
            altersSystemCache = true
    ) {
        when (configured) {
            true -> true
            false -> {
                if (target.family == Family.MINGW) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING, "MinGW target does not support mmap/munmap")
                    true // FIXME should this be reflected in default?
                } else {
                    false // FIXME should this be reflected in default?
                }
            }
        }
    }

    // 246 doesn't seem to be used in the wild.
    val mmapTag: UByte by option(BinaryOptions.mmapTag, default = { 246U }) {
        if (configured > 255U) {
            configuration.report(CompilerMessageSeverity.ERROR, "mmap tag must be between 1 and 255")
        }
        configured.toUByte()
    }

    val packFields: Boolean by option(BinaryOptions.packFields, default = { true })

    val runtimeLogsEnabled: Boolean by option(KonanConfigKeys.RUNTIME_LOGS, default = { false }, invalidatesCaches = true) { true }

    val runtimeLogs: Map<LoggingTag, LoggingLevel> by option(
            KonanConfigKeys.RUNTIME_LOGS,
            default = { LoggingTag.entries.associateWith { LoggingLevel.None } }
    ) {
        fun <T> error(message: String): T? {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "$message. No logging will be performed.")
            return null
        }

        fun parseSingleTagLevel(tagLevel: String): Pair<LoggingTag, LoggingLevel>? {
            val parts = tagLevel.split("=")
            val tagStr = parts[0]
            val tag = tagStr.let {
                LoggingTag.parse(it) ?: error("Failed to parse log tag at \"$tagStr\"")
            }
            val levelStr = parts.getOrNull(1) ?: error("Failed to parse log tag-level pair at \"$tagLevel\"")
            val level = parts.getOrNull(1)?.let {
                LoggingLevel.parse(it) ?: error("Failed to parse log level at \"$levelStr\"")
            }
            if (level == LoggingLevel.None) return error("Invalid log level: \"$levelStr\"")
            return tag?.let { t -> level?.let { l -> Pair(t, l) } }
        }

        default + configured.split(",").map { parseSingleTagLevel(it) ?: return@option default }
    }


    val suspendFunctionsFromAnyThreadFromObjC: Boolean by option(BinaryOptions.objcExportSuspendFunctionLaunchThreadRestriction, default = { false }) {
        configured == ObjCExportSuspendFunctionLaunchThreadRestriction.NONE
    }

    val sourceInfoType: SourceInfoType by option(
            BinaryOptions.sourceInfoType,
            default = { if (debug && target.supportsCoreSymbolication()) SourceInfoType.CORESYMBOLICATION else SourceInfoType.NOOP }
    )

    val coreSymbolicationUseOnlyKotlinImage: Boolean by option(BinaryOptions.coreSymbolicationImageListType, default = { true }) {
        configured == CoreSymbolicationImageListType.ONLY_KOTLIN
    }

    val gcSchedulerType: GCSchedulerType by option(
            BinaryOptions.gcSchedulerType,
            default = { if (gc == GC.NOOP) GCSchedulerType.MANUAL else GCSchedulerType.ADAPTIVE },
            altersSystemCache = true
    ) {
        configured.deprecatedWithReplacement?.let { replacement ->
            assert(configured != default)
            configuration.report(
                    CompilerMessageSeverity.WARNING,
                    "Binary option gcSchedulerType=$configured is deprecated. Use gcSchedulerType=$replacement instead"
            )
            replacement
        } ?: configured
    }

    val gcMarkSingleThreaded: Boolean by option(
            BinaryOptions.gcMarkSingleThreaded,
            default = { target.family == Family.MINGW && gc == GC.PARALLEL_MARK_CONCURRENT_SWEEP },
            altersSystemCache = true,
    )

    val fixedBlockPageSize: UInt by option(
            BinaryOptions.fixedBlockPageSize,
            default = { 128u },
            altersSystemCache = true
    )

    val concurrentWeakSweep: Boolean by option(BinaryOptions.concurrentWeakSweep, default = { true }, altersSystemCache = true)

    val concurrentMarkMaxIterations: UInt by option(BinaryOptions.concurrentMarkMaxIterations, default = { 100U })

    val gcMutatorsCooperate: Boolean by option(BinaryOptions.gcMutatorsCooperate, default = { true }) {
        // FIXME reflect these checks in default?
        if (gcMarkSingleThreaded) {
            if (configured == true) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Mutators cooperation is not supported during single threaded mark")
            }
            false
        } else if (gc == GC.CONCURRENT_MARK_AND_SWEEP) {
            if (configured == true) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Mutators cooperation is not yet supported in CMS GC")
            }
            false
        } else {
            configured
        }
    }

    val auxGCThreads: UInt by option(BinaryOptions.auxGCThreads, default = { 0U }) {
        if (gcMarkSingleThreaded && configured != 0U) {
            configuration.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Auxiliary GC workers are not supported during single threaded mark"
            )
        }
        configured
    }

    val needCompilerVerification: Boolean by
            option(KonanConfigKeys.VERIFY_COMPILER, default = { optimizationsEnabled || !KotlinCompilerVersion.VERSION.isRelease() })

    val appStateTracking: AppStateTracking by option(BinaryOptions.appStateTracking, default = { AppStateTracking.DISABLED })

    val objcDisposeOnMain: Boolean by option(BinaryOptions.objcDisposeOnMain, default = { true })

    val objcDisposeWithRunLoop: Boolean by option(BinaryOptions.objcDisposeWithRunLoop, default = { true })

    val objcEntryPoints: ObjCEntryPoints by option(BinaryOptions.objcExportEntryPointsPath, default = { ObjCEntryPoints.ALL }) {
        File(configured).readObjCEntryPoints()
    }

    /**
     * Path to store ObjC selector to Kotlin signature mapping
     */
    val dumpObjcSelectorToSignatureMapping: String? by
        option(BinaryOptions.dumpObjcSelectorToSignatureMapping, default = { null }) { configured }

    // Disabled by default because of KT-68928
    val enableSafepointSignposts: Boolean by option(BinaryOptions.enableSafepointSignposts, default = { false }) {
        if (configured && !target.supportsSignposts) {
            configuration.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Signposts are not available on $target. The setting will have no effect."
            )
        }
        configured
    }

    val globalDataLazyInit: Boolean by option(BinaryOptions.globalDataLazyInit, default = { true })

    // For now disabled by default due to performance penalty.
    val genericSafeCasts: Boolean by option(BinaryOptions.genericSafeCasts, default = { false })

    val pagedAllocator: Boolean by option(BinaryOptions.pagedAllocator, default = { true }, altersSystemCache = true)

    // TODO not an option?
    internal val bridgesPolicy: BridgesPolicy by lazy {
        if (genericSafeCasts) BridgesPolicy.BOX_UNBOX_CASTS else BridgesPolicy.BOX_UNBOX_ONLY
    }

    val llvmModulePasses: String? by option(KonanConfigKeys.LLVM_MODULE_PASSES, default = { null })

    val llvmLTOPasses: String? by option(KonanConfigKeys.LLVM_LTO_PASSES, default = { null })

    val preCodegenInlineThreshold: UInt by option(BinaryOptions.preCodegenInlineThreshold, default = { 0U })

    // TODO
    val enableDebugTransparentStepping: Boolean
        get() = target.family.isAppleFamily && (configuration.get(BinaryOptions.enableDebugTransparentStepping) ?: true)

    val latin1Strings: Boolean by option(BinaryOptions.latin1Strings, default = { false })

    init {
        // NB: producing LIBRARY is enabled on any combination of hosts/targets
        if (produce != CompilerOutputKind.LIBRARY && !platformManager.isEnabled(target)) {
            error("Target ${target.visibleName} is not available for output kind '${produce}' on the ${HostManager.hostName} host")
        }
    }

    // TODO
    val platform by lazy {
        platformManager.platform(target).apply {
            if (configuration.getBoolean(KonanConfigKeys.CHECK_DEPENDENCIES)) {
                downloadDependencies()
            }
        }
    }

    internal val clang by lazy { platform.clang }

    internal val produce: CompilerOutputKind get() = configuration.get(KonanConfigKeys.PRODUCE)!!

    internal val metadataKlib get() = configuration.getBoolean(CommonConfigurationKeys.METADATA_KLIB)

    internal val headerKlibPath get() = configuration.get(KonanConfigKeys.HEADER_KLIB)?.removeSuffixIfPresent(".klib")

    internal val produceStaticFramework get() = configuration.getBoolean(KonanConfigKeys.STATIC_FRAMEWORK)

    internal val purgeUserLibs: Boolean
        get() = configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)

    internal val writeDependenciesOfProducedKlibTo
        get() = configuration.get(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)

    internal val resolve = KonanLibrariesResolveSupport(
            configuration, target, distribution, resolveManifestDependenciesLenient = true
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

    val allocationMode by option(
            KonanConfigKeys.ALLOCATION_MODE,
            default = { if (sanitizer == null) AllocationMode.CUSTOM else AllocationMode.STD },
            altersSystemCache = true
    ) {
        if (sanitizer != null && configured == AllocationMode.CUSTOM) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Sanitizers are useful only with the std allocator")
        }
        configured
    }

    internal val runtimeNativeLibraries: List<String> = mutableListOf<String>().apply {
        if (debug) add("debug.bc")
        add("runtime.bc")
        add("mm.bc")
        add("common_alloc.bc")
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
        when (gc) {
            GC.STOP_THE_WORLD_MARK_AND_SWEEP -> add("same_thread_ms_gc.bc")
            GC.NOOP -> add("noop_gc.bc")
            GC.PARALLEL_MARK_CONCURRENT_SWEEP -> add("pmcs_gc.bc")
            GC.CONCURRENT_MARK_AND_SWEEP -> add("concurrent_ms_gc.bc")
        }
        if (target.supportsCoreSymbolication()) {
            add("source_info_core_symbolication.bc")
        }
        if (target.supportsLibBacktrace()) {
            add("source_info_libbacktrace.bc")
            add("libbacktrace.bc")
        }
        when (allocationMode) {
            AllocationMode.STD -> {
                add("legacy_alloc.bc")
                add("std_alloc.bc")
            }
            AllocationMode.CUSTOM -> {
                add("custom_alloc.bc")
            }
        }
        when (checkStateAtExternalCalls) {
            true -> add("impl_externalCallsChecker.bc")
            false -> add("noop_externalCallsChecker.bc")
        }
    }.map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val runtimeLinkageStrategy: RuntimeLinkageStrategy by lazy {
        // Intentionally optimize in debug mode only. See `RuntimeLinkageStrategy`.
        val defaultStrategy = if (debug) RuntimeLinkageStrategy.Optimize else RuntimeLinkageStrategy.Raw
        configuration.get(BinaryOptions.linkRuntime) ?: defaultStrategy
    }

    internal val launcherNativeLibraries: List<String> = distribution.launcherFiles.map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val objCNativeLibrary: String =
            File(distribution.defaultNatives(target)).child("objc.bc").absolutePath

    internal val xcTestLauncherNativeLibrary: String =
            File(distribution.defaultNatives(target)).child("xctest_launcher.bc").absolutePath

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

    internal val nativeTargetsForManifest = configuration.get(KonanConfigKeys.MANIFEST_NATIVE_TARGETS)

    internal val isInteropStubs: Boolean get() = manifestProperties?.getProperty("interop") == "true"

    internal val propertyLazyInitialization: Boolean by option(
            KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION,
            default = { true },
            altersSystemCache = true,
            altersUserCache = true,
    ) {
        if (!configured) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Eager property initialization is deprecated")
        }
        configured
    }

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

    internal val useDebugInfoInNativeLibs: Boolean by option(
            BinaryOptions.stripDebugInfoFromNativeLibs,
            default = { false },
            altersSystemCache = true,
    ) { !configured }

    internal val partialLinkageConfig = configuration.partialLinkageConfig

    internal val additionalCacheFlags by lazy { platformManager.loader(target).additionalCacheFlags }

    internal val threadsCount = configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1

    private fun StringBuilder.appendCommonCacheFlavor() {
        append(target.toString())
        if (debug) append("-g") // todo from option?
        append("STATIC") // TODO what is this?

        systemCacheFlavor.mapNotNull { it() }.forEach { append(it) }
        userCacheFlavor.mapNotNull { it() }.forEach { append(it) }
    }

    private val systemCacheFlavorString = buildString {
        appendCommonCacheFlavor()
        append("-system")
        systemCacheFlavor.mapNotNull { it() }.forEach { append(it) }
    }

    private val userCacheFlavorString = buildString {
        appendCommonCacheFlavor()
        append("-user")
        if (partialLinkageConfig.isEnabled) append("-pl") // TODO replace with option?
        userCacheFlavor.mapNotNull { it() }.forEach { append(it) }
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
        runtimeLogsEnabled -> "with runtime logs"
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

    internal val cInterfaceGenerationMode: CInterfaceGenerationMode by lazy {
        val explicitMode = configuration.get(BinaryOptions.cInterfaceMode)
        when {
            explicitMode != null -> explicitMode
            produce == CompilerOutputKind.DYNAMIC || produce == CompilerOutputKind.STATIC -> CInterfaceGenerationMode.V1
            else -> CInterfaceGenerationMode.NONE
        }
    }
}

private fun String.isRelease(): Boolean {
    // major.minor.patch-meta-build where patch, meta and build are optional.
    val versionPattern = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-(\\p{Alpha}*\\p{Alnum}+(?:\\.\\p{Alnum}+)*|-[\\p{Alnum}.-]+))?(?:-(\\d+))?".toRegex()
    val (_, _, _, metaString, build) = versionPattern.matchEntire(this)?.destructured
            ?: throw IllegalStateException("Cannot parse Kotlin/Native version: $this")

    return metaString.isEmpty() && build.isEmpty()
}
