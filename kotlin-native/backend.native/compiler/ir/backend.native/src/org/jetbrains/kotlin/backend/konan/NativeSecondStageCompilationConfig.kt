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
import org.jetbrains.kotlin.backend.konan.util.systemCacheRootDirectory
import org.jetbrains.kotlin.backend.konan.util.toObsoleteKind
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.SanitizerKind
import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.allocationMode
import org.jetbrains.kotlin.konan.config.autoCacheDir
import org.jetbrains.kotlin.konan.config.checkDependencies
import org.jetbrains.kotlin.konan.config.compileFromBitcode
import org.jetbrains.kotlin.konan.config.debug
import org.jetbrains.kotlin.konan.config.enableAssertions
import org.jetbrains.kotlin.konan.config.externalDependencies
import org.jetbrains.kotlin.konan.config.fullExportedNamePrefix
import org.jetbrains.kotlin.konan.config.generateDebugTrampoline
import org.jetbrains.kotlin.konan.config.incrementalCacheDir
import org.jetbrains.kotlin.konan.config.konanDataDir
import org.jetbrains.kotlin.konan.config.konanHome
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.konan.config.konanManifestAddend
import org.jetbrains.kotlin.konan.config.konanPurgeUserLibs
import org.jetbrains.kotlin.konan.config.konanTarget
import org.jetbrains.kotlin.konan.config.llvmLtoPasses
import org.jetbrains.kotlin.konan.config.llvmModulePasses
import org.jetbrains.kotlin.konan.config.llvmVariant
import org.jetbrains.kotlin.konan.config.makePerFileCache
import org.jetbrains.kotlin.konan.config.omitFrameworkBinary
import org.jetbrains.kotlin.konan.config.optimization
import org.jetbrains.kotlin.konan.config.runtimeFile
import org.jetbrains.kotlin.konan.config.runtimeLogs
import org.jetbrains.kotlin.konan.config.saveDependenciesPath
import org.jetbrains.kotlin.konan.config.saveLlvmIrDirectory
import org.jetbrains.kotlin.konan.config.serializedDependencies
import org.jetbrains.kotlin.konan.config.staticFramework
import org.jetbrains.kotlin.konan.config.testDumpOutputPath
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.native.resolve.KonanLibrariesResolveSupport
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.nio.file.Files
import java.nio.file.Paths

class NativeSecondStageCompilationConfig(
        val project: Project,
        override val configuration: CompilerConfiguration
) : NativeCompilationConfig {
    /**
     * Determine if we compile for iOS target with Mac ABI (Catalyst).
     * Avoid using this property if possible. Instead, use [TargetTriple.isMacabi] as it is more direct.
     */
    val macabi: Boolean = run {
        val macabi = configuration.getBoolean(BinaryOptions.macabi)
        // We can't access `target` property due to circular dependency.
        val target = configuration.konanTarget
        if (macabi && target !in setOf("ios_simulator_arm64", "ios_x64")) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "macabi is only supported for iosArm64 target")
            false
        } else macabi
    }

    internal val distribution = run {
        val overridenProperties = mutableMapOf<String, String>().apply {
            if (macabi) {
                val target = configuration.konanTarget
                // Overriding properties is a bit better alternative than
                // Tracking all usages of `configurables` in code and making adjustments on call-site.
                // Still ugly, of course.
                when (target) {
                    "ios_x64" -> {
                        put("targetTriple.$target", "x86_64-apple-ios-macabi")
                        put("targetSysRoot.$target", "\$targetSysRoot.macos_x64")
                    }
                    "ios_simulator_arm64" -> {
                        // macabi implies usage of macOS sysroot.
                        put("targetTriple.$target", "arm64-apple-ios-macabi")
                        put("targetSysRoot.$target", "\$targetSysRoot.macos_arm64")
                    }
                }
            }
            configuration[NativeConfigurationKeys.OVERRIDE_KONAN_PROPERTIES]?.let(this::putAll)
            configuration.llvmVariant?.getKonanPropertiesEntry()?.let { (key, value) ->
                put(key, value)
            }
        }

        Distribution(
                configuration.konanHome ?: KotlinNativePaths.homePath.absolutePath,
                false,
                configuration.runtimeFile,
                overridenProperties,
                configuration.konanDataDir,
        )
    }

    private val platformManager = PlatformManager(distribution)
    internal val targetManager = platformManager.targetManager(configuration.konanTarget)
    override val target = targetManager.target
    internal val phaseConfig = configuration.phaseConfig!!

    // See https://youtrack.jetbrains.com/issue/KT-67692.
    val useLlvmOpaquePointers = true

    // TODO: debug info generation mode and debug/release variant selection probably requires some refactoring.
    val debug: Boolean get() = configuration.debug
    val lightDebug: Boolean = configuration[NativeConfigurationKeys.LIGHT_DEBUG]
            ?: target.family.isAppleFamily // Default is true for Apple targets.
    val generateDebugTrampoline = debug && configuration.generateDebugTrampoline
    val optimizationsEnabled = configuration.optimization

    val smallBinary: Boolean = run {
        val explicit = configuration.get(BinaryOptions.smallBinary)
        if (debug) {
            if (explicit == true) {
                configuration.report(CompilerMessageSeverity.WARNING, "smallBinary is not compatible with debug, and will be ignored")
            }
            return@run false
        }
        explicit ?: target.needSmallBinary()
    }
    val inlineForPerformance get() = !debug && !smallBinary

    val assertsEnabled = configuration.enableAssertions

    val sanitizer = configuration.get(BinaryOptions.sanitizer)?.takeIf {
        when {
            it != SanitizerKind.THREAD -> "${it.name} sanitizer is not supported yet"
            produce == CompilerOutputKind.STATIC -> "${it.name} sanitizer is unsupported for static library"
            produce == CompilerOutputKind.FRAMEWORK && produceStaticFramework -> "${it.name} sanitizer is unsupported for static framework"
            it.toObsoleteKind() !in target.supportedSanitizers() -> "${it.name} sanitizer is unsupported on ${target.name}"
            else -> null
        }?.let { message ->
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
            return@takeIf false
        }
        return@takeIf true
    }

    private val defaultGC get() = GC.CONCURRENT_MARK_AND_SWEEP
    val gc: GC
        get() = configuration.get(BinaryOptions.gc) ?: defaultGC
    val runtimeAssertsMode: RuntimeAssertsMode get() = configuration.get(BinaryOptions.runtimeAssertionsMode) ?: RuntimeAssertsMode.IGNORE
    val checkStateAtExternalCalls: Boolean get() = configuration.get(BinaryOptions.checkStateAtExternalCalls) ?: false
    private val defaultDisableMmap get() = target.family == Family.MINGW || !pagedAllocator
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
    val mmapTag: UByte by lazy {
        configuration.get(BinaryOptions.mmapTag)?.let {
            if (it > 255U) {
                configuration.report(CompilerMessageSeverity.ERROR, "mmap tag must be between 1 and 255")
            }
            it.toUByte()
        } ?: 246U // doesn't seem to be used in the wild.
    }
    val packFields: Boolean by lazy {
        configuration.get(BinaryOptions.packFields) ?: true
    }

    val runtimeLogsEnabled: Boolean by lazy {
        configuration.runtimeLogs != null
    }

    val runtimeLogs: Map<LoggingTag, LoggingLevel> by lazy {
        val default = LoggingTag.entries.associateWith { LoggingLevel.None }

        val cfgString = configuration.runtimeLogs ?: return@lazy default

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

        val configured = cfgString.split(",").map { parseSingleTagLevel(it) ?: return@lazy default }
        default + configured
    }


    val suspendFunctionsFromAnyThreadFromObjC: Boolean by lazy {
        configuration.get(BinaryOptions.objcExportSuspendFunctionLaunchThreadRestriction) !=
                ObjCExportSuspendFunctionLaunchThreadRestriction.MAIN
    }

    val sourceInfoType: SourceInfoType
        get() = configuration.get(BinaryOptions.sourceInfoType)
                ?: SourceInfoType.CORESYMBOLICATION.takeIf { debug && target.supportsCoreSymbolication() }
                ?: SourceInfoType.NOOP

    val coreSymbolicationUseOnlyKotlinImage: Boolean
        get() = when (configuration.get(BinaryOptions.coreSymbolicationImageListType)) {
            null,
            CoreSymbolicationImageListType.ALL_LOADED -> false
            CoreSymbolicationImageListType.ONLY_KOTLIN -> true
        }

    val defaultGCSchedulerType
        get() =
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

    private val defaultGcMarkSingleThreaded get() = target.family == Family.MINGW && gc == GC.PARALLEL_MARK_CONCURRENT_SWEEP

    val gcMarkSingleThreaded: Boolean by lazy {
        configuration.get(BinaryOptions.gcMarkSingleThreaded) ?: defaultGcMarkSingleThreaded
    }

    private val defaultFixedBlockPageSize: UInt get() = 128u

    val fixedBlockPageSize: UInt
        get() = configuration.get(BinaryOptions.fixedBlockPageSize) ?: defaultFixedBlockPageSize

    val concurrentWeakSweep: Boolean
        get() = configuration.get(BinaryOptions.concurrentWeakSweep) ?: true

    val concurrentMarkMaxIterations: UInt
        get() = configuration.get(BinaryOptions.concurrentMarkMaxIterations) ?: 100U

    val gcMutatorsCooperate: Boolean by lazy {
        val mutatorsCooperate = configuration.get(BinaryOptions.gcMutatorsCooperate)
        if (gcMarkSingleThreaded) {
            if (mutatorsCooperate == true) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Mutators cooperation is not supported during single threaded mark")
            }
            false
        } else if (gc == GC.CONCURRENT_MARK_AND_SWEEP) {
            if (mutatorsCooperate == true) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Mutators cooperation is not yet supported in CMS GC")
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

    val needCompilerVerification: Boolean
        get() = configuration[NativeConfigurationKeys.VERIFY_COMPILER]
                ?: (optimizationsEnabled || !KotlinCompilerVersion.VERSION.isRelease())

    val appStateTracking: AppStateTracking by lazy {
        configuration.get(BinaryOptions.appStateTracking) ?: AppStateTracking.DISABLED
    }

    val objcDisposeOnMain: Boolean by lazy {
        configuration.get(BinaryOptions.objcDisposeOnMain) ?: true
    }

    val objcDisposeWithRunLoop: Boolean by lazy {
        configuration.get(BinaryOptions.objcDisposeWithRunLoop) ?: true
    }

    val objcEntryPoints: ObjCEntryPoints by lazy {
        configuration
                .get(BinaryOptions.objcExportEntryPointsPath)
                ?.let { File(it).readObjCEntryPoints() }
                ?: ObjCEntryPoints.ALL
    }

    /**
     * Path to store ObjC selector to Kotlin signature mapping
     */
    val dumpObjcSelectorToSignatureMapping: String? by lazy {
        configuration.get(BinaryOptions.dumpObjcSelectorToSignatureMapping)
    }

    val enableSafepointSignposts: Boolean = configuration.get(BinaryOptions.enableSafepointSignposts)?.also {
        if (it && !target.supportsSignposts) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Signposts are not available on $target. The setting will have no effect.")
        }
    } ?: false // Disabled by default because of KT-68928

    val forceNativeThreadStateForFunctions: Set<String> =
            configuration.get(BinaryOptions.forceNativeThreadStateForFunctions)?.toSet()
                    ?: setOf(
                            "org_jetbrains_skia_DirectContext__1nFlushAndSubmit", // KT-75895, KT-79384
                    )

    val globalDataLazyInit: Boolean
        get() = configuration.get(BinaryOptions.globalDataLazyInit)?.also {
            if (!it) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Eager Global Data initialization is deprecated")
            }
        } ?: true

    private val defaultGenericSafeCasts = !optimizationsEnabled // For now disabled in -opt due to performance penalty.
    val genericSafeCasts: Boolean by lazy {
        configuration.get(BinaryOptions.genericSafeCasts)
                ?: defaultGenericSafeCasts
    }

    internal val defaultPagedAllocator: Boolean get() = sanitizer == null

    val pagedAllocator: Boolean by lazy {
        configuration.get(BinaryOptions.pagedAllocator) ?: defaultPagedAllocator
    }

    internal val bridgesPolicy: BridgesPolicy by lazy {
        if (genericSafeCasts) BridgesPolicy.BOX_UNBOX_CASTS else BridgesPolicy.BOX_UNBOX_ONLY
    }

    val llvmModulePasses: String? by lazy {
        configuration.llvmModulePasses
    }

    val llvmLTOPasses: String? by lazy {
        configuration.llvmLtoPasses
    }

    val preCodegenInlineThreshold: UInt by lazy {
        configuration.get(BinaryOptions.preCodegenInlineThreshold) ?: 0U
    }

    val enableDebugTransparentStepping: Boolean
        get() = target.family.isAppleFamily && (configuration.get(BinaryOptions.enableDebugTransparentStepping) ?: true)

    val latin1Strings: Boolean
        get() = configuration.get(BinaryOptions.latin1Strings) ?: false

    init {
        // NB: producing LIBRARY is enabled on any combination of hosts/targets
        if (produce != CompilerOutputKind.LIBRARY && !platformManager.isEnabled(target)) {
            error("Target ${target.visibleName} is not available for output kind '${produce}' on the ${HostManager.hostName} host")
        }
    }

    val platform by lazy {
        platformManager.platform(target).apply {
            if (configuration.checkDependencies) {
                downloadDependencies()
            }
        }
    }

    internal val clang by lazy { platform.clang }

    internal val produceStaticFramework get() = configuration.staticFramework

    private val resolve = KonanLibrariesResolveSupport(
            configuration, target, distribution, resolveManifestDependenciesLenient = true
    )

    val resolvedLibraries get() = resolve.resolvedLibraries

    val includedLibraries: List<KotlinLibrary>
        get() = getIncludedLibraries(
                configuration.konanIncludedLibraries.map { File(it) },
                configuration,
                resolve.resolvedLibraries
        )

    val exportedLibraries: List<KotlinLibrary>
        get() = getExportedLibraries(configuration, resolve.resolvedLibraries, resolve.resolver.searchPathResolver, report = true)

    fun librariesWithDependencies(): List<KotlinLibrary> {
        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(
                TopologicalLibraryOrder
        )
    }

    internal val externalDependenciesFile = configuration.externalDependencies?.let(::File)

    internal val userVisibleIrModulesSupport = KonanUserVisibleIrModulesSupport(
            externalDependenciesLoader = UserVisibleIrModulesSupport.ExternalDependenciesLoader.from(
                    externalDependenciesFile = externalDependenciesFile,
                    onMalformedExternalDependencies = { warningMessage ->
                        configuration.report(CompilerMessageSeverity.STRONG_WARNING, warningMessage)
                    }),
            konanKlibDir = File(distribution.klib)
    )

    val fullExportedNamePrefix: String
        get() = configuration.fullExportedNamePrefix ?: implicitModuleName

    override val moduleId: String
        get() = configuration.moduleName ?: implicitModuleName

    private val defaultAllocationMode
        get() = AllocationMode.CUSTOM

    val allocationMode by lazy {
        (configuration.allocationMode ?: defaultAllocationMode).also {
            if (it == AllocationMode.CUSTOM && sanitizer != null && pagedAllocator) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Sanitizers are not useful with the paged allocator")
            }
        }
    }

    val minidumpLocation by lazy {
        configuration.get(BinaryOptions.minidumpLocation)
    }

    val minidumpOnSIGTERM by lazy {
        configuration.get(BinaryOptions.minidumpOnSIGTERM)?.let {
            if (it && minidumpLocation == null) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Minidump location is not defined")
                false
            } else it
        } ?: false
    }

    val swiftExport by lazy {
        configuration.get(BinaryOptions.swiftExport)?.let {
            if (it && !target.supportsObjcInterop()) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Swift Export cannot be enabled on $target that does not have objc interop")
                false
            } else it
        } ?: false
    }

    internal val runtimeLinkageStrategy: RuntimeLinkageStrategy by lazy {
        // Intentionally optimize in debug mode only. See `RuntimeLinkageStrategy`.
        val defaultStrategy = if (debug) RuntimeLinkageStrategy.Optimize else RuntimeLinkageStrategy.Raw
        configuration.get(BinaryOptions.linkRuntime) ?: defaultStrategy
    }

    override val manifestProperties = configuration.konanManifestAddend?.let {
        File(it).loadProperties()
    }

    private val defaultPropertyLazyInitialization = true
    internal val propertyLazyInitialization: Boolean
        get() = configuration[NativeConfigurationKeys.PROPERTY_LAZY_INITIALIZATION]?.also {
            if (!it) {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING, "Eager property initialization is deprecated")
            }
        } ?: defaultPropertyLazyInitialization

    internal val entryPointName: String by lazy {
        if (target.family == Family.ANDROID) {
            val androidProgramType = configuration.get(BinaryOptions.androidProgramType)
                    ?: AndroidProgramType.Default
            val konanMainOverride = androidProgramType.konanMainOverride
            if (konanMainOverride != null) {
                return@lazy konanMainOverride
            }
        }
        "Konan_main"
    }

    internal val testDumpFile: File? = configuration.testDumpOutputPath?.let(::File)

    internal val useDebugInfoInNativeLibs = configuration.get(BinaryOptions.stripDebugInfoFromNativeLibs) == false

    internal val partialLinkageConfig = configuration.partialLinkageConfig

    internal val additionalCacheFlags by lazy { platformManager.loader(target).additionalCacheFlags }

    internal val threadsCount = configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1

    val stackProtectorMode = configuration.get(BinaryOptions.stackProtector)?.let {
        if (it != StackProtectorMode.NO && target.family == Family.MINGW) {
            configuration.reportCompilationError("Stack protector is not supported on MinGW targets")
            null
        } else it
    } ?: StackProtectorMode.NO

    private fun StringBuilder.appendCommonCacheFlavor() {
        append(target.toString())
        if (debug) append("-g")
        append("STATIC")

        if (propertyLazyInitialization != defaultPropertyLazyInitialization)
            append("-lazy_init${if (propertyLazyInitialization) "ENABLE" else "DISABLE"}")
        if (sanitizer != null)
            append("-sanitizer${sanitizer.name}")
        if (checkStateAtExternalCalls)
            append("-check_state_at_external_calls")
        if (stackProtectorMode != StackProtectorMode.NO)
            append("-stack_protector${stackProtectorMode.name}")
        if (genericSafeCasts != defaultGenericSafeCasts)
            append("-generic_safe_casts${if (genericSafeCasts) "ENABLE" else "DISABLE"}")
        // Ideally, we would like to use targetTriple instead, but this requires a lot of changes in our test infrastructure.
        if (macabi) {
            append("-macabi")
        }
    }

    private val systemCacheFlavorString = buildString {
        appendCommonCacheFlavor()
        append("-system")

        if (useDebugInfoInNativeLibs)
            append("-runtime_debug")
        if (allocationMode != defaultAllocationMode)
            append("-allocator${allocationMode.name}")
        if (gc != defaultGC)
            append("-gc${gc.shortcut}")
        if (gcSchedulerType != defaultGCSchedulerType)
            append("-gc_scheduler${gcSchedulerType.name}")
        if (runtimeAssertsMode != RuntimeAssertsMode.IGNORE)
            append("-runtime_asserts${runtimeAssertsMode.name}")
        if (disableMmap != defaultDisableMmap)
            append("-disable_mmap${if (disableMmap) "TRUE" else "FALSE"}")
        if (gcMarkSingleThreaded != defaultGcMarkSingleThreaded)
            append("-gc_mark_single_threaded${if (gcMarkSingleThreaded) "TRUE" else "FALSE"}")
        if (fixedBlockPageSize != defaultFixedBlockPageSize)
            append("-fixed_block_page_size$fixedBlockPageSize")
        if (pagedAllocator != defaultPagedAllocator)
            append("-paged_allocator${if (pagedAllocator) "TRUE" else "FALSE"}")
        if (minidumpLocation != null)
            append("-with_crash_dumps")
    }

    private val userCacheFlavorString = buildString {
        appendCommonCacheFlavor()
        append("-user")
        if (partialLinkageConfig.isEnabled) append("-pl")
    }

    internal val systemCacheDirectory = File(distribution.systemCacheRootDirectory.absolutePath).child(systemCacheFlavorString).also { it.mkdirs() }
    private val autoCacheRootDirectory = configuration.autoCacheDir?.let {
        File(it).apply {
            if (!isDirectory) configuration.reportCompilationError("auto cache directory $this is not found or is not a directory")
        }
    } ?: File(distribution.systemCacheRootDirectory.absolutePath)
    internal val autoCacheDirectory = autoCacheRootDirectory.child(userCacheFlavorString).also { it.mkdirs() }
    private val incrementalCacheRootDirectory = configuration.incrementalCacheDir?.let {
        File(it).apply {
            if (!isDirectory) configuration.reportCompilationError("incremental cache directory $this is not found or is not a directory")
        }
    }
    internal val incrementalCacheDirectory = incrementalCacheRootDirectory?.child(userCacheFlavorString)?.also { it.mkdirs() }

    internal val ignoreCacheReason = when {
        optimizationsEnabled -> "for optimized compilation"
        runtimeLogsEnabled -> "with runtime logs"
        configuration.get(BinaryOptions.forceNativeThreadStateForFunctions) != null -> "with non-default forceNativeThreadStateForFunctions"
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
        get() = configuration.makePerFileCache

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
        configuration.omitFrameworkBinary.also {
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
        configuration.compileFromBitcode
    }

    /**
     * Path to serialized dependencies to use for bitcode compilation.
     */
    internal val readSerializedDependencies: String? by lazy {
        configuration.serializedDependencies
    }

    /**
     * Path to store backend dependency information.
     */
    internal val writeSerializedDependencies: String? by lazy {
        configuration.saveDependenciesPath
    }

    /**
     * Directory to store LLVM IR from -Xsave-llvm-ir-after.
     */
    internal val saveLlvmIrDirectory: java.io.File by lazy {
        val path = configuration.saveLlvmIrDirectory
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

    val cCallMode get() = configuration.get(BinaryOptions.cCallMode) ?: CCallMode.IndirectOrDirect

    val unitSuspendFunctionObjCExport: UnitSuspendFunctionObjCExport
        get() = configuration.get(BinaryOptions.unitSuspendFunctionObjCExport) ?: UnitSuspendFunctionObjCExport.DEFAULT

    val languageVersionSettings: LanguageVersionSettings
        get() = configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!

    val purgeUserLibs: Boolean
        get() = configuration.konanPurgeUserLibs

    val isInteropStubs: Boolean
        get() = manifestProperties?.getProperty("interop") == "true"
}

private fun String.isRelease(): Boolean {
    // major.minor.patch-meta-build where patch, meta and build are optional.
    val versionPattern = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-(\\p{Alpha}*\\p{Alnum}+(?:\\.\\p{Alnum}+)*|-[\\p{Alnum}.-]+))?(?:-(\\d+))?".toRegex()
    val (_, _, _, metaString, build) = versionPattern.matchEntire(this)?.destructured
            ?: throw IllegalStateException("Cannot parse Kotlin/Native version: $this")

    return metaString.isEmpty() && build.isEmpty()
}
