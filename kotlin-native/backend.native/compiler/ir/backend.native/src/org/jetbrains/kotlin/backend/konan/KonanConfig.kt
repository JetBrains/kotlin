/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.utils.addToStdlib.cast

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
    internal val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)!!

    // TODO: debug info generation mode and debug/release variant selection probably requires some refactoring.
    val debug: Boolean get() = configuration.getBoolean(KonanConfigKeys.DEBUG)
    val lightDebug: Boolean = configuration.get(KonanConfigKeys.LIGHT_DEBUG)
            ?: target.family.isAppleFamily // Default is true for Apple targets.
    val generateDebugTrampoline = debug && configuration.get(KonanConfigKeys.GENERATE_DEBUG_TRAMPOLINE) ?: false

    val memoryModel: MemoryModel by lazy {
        when (configuration.get(BinaryOptions.memoryModel)!!) {
            MemoryModel.STRICT -> MemoryModel.STRICT
            MemoryModel.RELAXED -> MemoryModel.RELAXED
            MemoryModel.EXPERIMENTAL -> {
                if (!target.supportsThreads()) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                            "Experimental memory model requires threads, which are not supported on target ${target.name}. Used strict memory model.")
                    MemoryModel.STRICT
                } else if (destroyRuntimeMode == DestroyRuntimeMode.LEGACY) {
                    configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                            "Experimental memory model is incompatible with 'legacy' destroy runtime mode. Used strict memory model.")
                    MemoryModel.STRICT
                } else {
                    MemoryModel.EXPERIMENTAL
                }
            }
        }
    }
    val destroyRuntimeMode: DestroyRuntimeMode get() = configuration.get(KonanConfigKeys.DESTROY_RUNTIME_MODE)!!
    val gc: GC get() = configuration.get(KonanConfigKeys.GARBAGE_COLLECTOR)!!
    val gcAggressive: Boolean get() = configuration.get(KonanConfigKeys.GARBAGE_COLLECTOR_AGRESSIVE)!!
    val runtimeAssertsMode: RuntimeAssertsMode get() = configuration.get(BinaryOptions.runtimeAssertionsMode) ?: RuntimeAssertsMode.IGNORE
    val workerExceptionHandling: WorkerExceptionHandling get() = configuration.get(KonanConfigKeys.WORKER_EXCEPTION_HANDLING)!!
    val runtimeLogs: String? get() = configuration.get(KonanConfigKeys.RUNTIME_LOGS)

    val needVerifyIr: Boolean
        get() = configuration.get(KonanConfigKeys.VERIFY_IR) == true

    val needCompilerVerification: Boolean
        get() = configuration.get(KonanConfigKeys.VERIFY_COMPILER) ?:
            (configuration.getBoolean(KonanConfigKeys.OPTIMIZATION) ||
                CompilerVersion.CURRENT.meta != MetaVersion.RELEASE)

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

    internal val userVisibleIrModulesSupport = KonanUserVisibleIrModulesSupport(
            externalDependenciesLoader = UserVisibleIrModulesSupport.ExternalDependenciesLoader.from(
                    externalDependenciesFile = configuration.get(KonanConfigKeys.EXTERNAL_DEPENDENCIES)?.let(::File),
                    onMalformedExternalDependencies = { warningMessage ->
                        configuration.report(CompilerMessageSeverity.STRONG_WARNING, warningMessage)
                    }),
            konanKlibDir = File(distribution.klib)
    )

    internal val cacheSupport = CacheSupport(configuration, resolvedLibraries, target, produce)

    internal val cachedLibraries: CachedLibraries
        get() = cacheSupport.cachedLibraries

    internal val librariesToCache: Set<KotlinLibrary>
        get() = cacheSupport.librariesToCache

    val outputFiles =
            OutputFiles(configuration.get(KonanConfigKeys.OUTPUT) ?: cacheSupport.tryGetImplicitOutput(),
                    target, produce)

    val tempFiles = TempFiles(outputFiles.outputName, configuration.get(KonanConfigKeys.TEMPORARY_FILES_DIR))

    val outputFile get() = outputFiles.mainFile

    private val implicitModuleName: String
        get() = File(outputFiles.outputName).name

    val fullExportedNamePrefix: String
        get() = configuration.get(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX) ?: implicitModuleName

    val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME) ?: implicitModuleName

    val shortModuleName: String?
        get() = configuration.get(KonanConfigKeys.SHORT_MODULE_NAME)

    val infoArgsOnly = configuration.kotlinSourceRoots.isEmpty()
            && configuration[KonanConfigKeys.INCLUDED_LIBRARIES].isNullOrEmpty()
            && librariesToCache.isEmpty()

    fun librariesWithDependencies(moduleDescriptor: ModuleDescriptor?): List<KonanLibrary> {
        if (moduleDescriptor == null) error("purgeUnneeded() only works correctly after resolve is over, and we have successfully marked package files as needed or not needed.")
        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(TopologicalLibraryOrder).cast()
    }

    val shouldCoverSources = configuration.getBoolean(KonanConfigKeys.COVERAGE)
    private val shouldCoverLibraries = !configuration.getList(KonanConfigKeys.LIBRARIES_TO_COVER).isNullOrEmpty()

    internal val runtimeNativeLibraries: List<String> = mutableListOf<String>().apply {
        add(if (debug) "debug.bc" else "release.bc")
        val useMimalloc = if (configuration.get(KonanConfigKeys.ALLOCATION_MODE) == "mimalloc") {
            if (target.supportsMimallocAllocator()) {
                true
            } else {
                configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                        "Mimalloc allocator isn't supported on target ${target.name}. Used standard mode.")
                false
            }
        } else {
            false
        }
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
                when (gc) {
                    GC.SAME_THREAD_MARK_AND_SWEEP -> {
                        add("experimental_memory_manager_stms.bc")
                        add("same_thread_ms_gc.bc")
                    }
                    GC.NOOP -> {
                        add("experimental_memory_manager_noop.bc")
                        add("noop_gc.bc")
                    }
                }
            }
        }
        if (shouldCoverLibraries || shouldCoverSources) add("profileRuntime.bc")
        if (useMimalloc) {
            add("opt_alloc.bc")
            add("mimalloc.bc")
        } else {
            add("std_alloc.bc")
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

    internal val manifestProperties = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
        File(it).loadProperties()
    }

    internal val isInteropStubs: Boolean get() = manifestProperties?.getProperty("interop") == "true"

    internal val propertyLazyInitialization: Boolean get() = configuration.get(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION)!!
}

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String)
    = this.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)
