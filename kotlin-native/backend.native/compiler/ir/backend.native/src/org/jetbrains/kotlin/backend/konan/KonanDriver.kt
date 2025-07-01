/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.fileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.backend.konan.driver.NativeCompilerDriver
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.copyCommonKlibArgumentsFrom
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import java.util.*

private val softDeprecatedTargets = setOf(
        KonanTarget.LINUX_ARM32_HFP,
)

private const val DEPRECATION_LINK = "https://kotl.in/native-targets-tiers"

interface CompilationSpawner {
    fun spawn(configuration: CompilerConfiguration)
    fun spawn(arguments: List<String>, setupConfiguration: CompilerConfiguration.() -> Unit)
}

class KonanDriver(
        val project: Project,
        val environment: KotlinCoreEnvironment,
        val configuration: CompilerConfiguration,
        val performanceManager: PerformanceManager?,
        val compilationSpawner: CompilationSpawner
) {
    fun run() {
        val outputKind = configuration[KonanConfigKeys.PRODUCE]
        val isCompilingFromBitcode = configuration[KonanConfigKeys.COMPILE_FROM_BITCODE] != null
        val hasSourceRoots = configuration.kotlinSourceRoots.isNotEmpty()

        if (isCompilingFromBitcode && hasSourceRoots) {
            configuration.report(
                    CompilerMessageSeverity.WARNING,
                    "Source files will be ignored by the compiler when compiling from bitcode"
            )
        }

        if (outputKind != CompilerOutputKind.LIBRARY && hasSourceRoots && !isCompilingFromBitcode) {
            // TODO KT-72014: Consider raising deprecation error instead of `splitOntoTwoStages()` invocation
            splitOntoTwoStages()
            return
        }

        val fileNames = configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)?.let { libPath ->
            val filesToCache = configuration.get(KonanConfigKeys.FILES_TO_CACHE)
            when {
                !filesToCache.isNullOrEmpty() -> filesToCache
                configuration.get(KonanConfigKeys.MAKE_PER_FILE_CACHE) == true -> {
                    val lib = createKonanLibrary(File(libPath), "default", null, true)
                    (0 until lib.fileCount()).map { fileIndex ->
                        val proto = IrFile.parseFrom(lib.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
                        lib.fileEntry(proto, fileIndex).name
                    }
                }
                else -> null
            }
        }
        if (fileNames != null) {
            configuration.put(KonanConfigKeys.MAKE_PER_FILE_CACHE, true)
            configuration.put(KonanConfigKeys.FILES_TO_CACHE, fileNames)
        }

        var konanConfig = KonanConfig(project, configuration)

        if (configuration.get(KonanConfigKeys.LIST_TARGETS) == true) {
            konanConfig.targetManager.list()
        }

        val hasIncludedLibraries = configuration[KonanConfigKeys.INCLUDED_LIBRARIES]?.isNotEmpty() == true
        val isProducingExecutableFromLibraries = konanConfig.produce == CompilerOutputKind.PROGRAM
                && configuration[KonanConfigKeys.LIBRARY_FILES]?.isNotEmpty() == true && !hasIncludedLibraries
        val hasCompilerInput = configuration.kotlinSourceRoots.isNotEmpty()
                || hasIncludedLibraries
                || configuration[KonanConfigKeys.EXPORTED_LIBRARIES]?.isNotEmpty() == true
                || konanConfig.libraryToCache != null
                || konanConfig.compileFromBitcode?.isNotEmpty() == true
                || isProducingExecutableFromLibraries

        if (!hasCompilerInput) return

        if (isProducingExecutableFromLibraries && configuration.get(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                    "Use `-Xinclude=<path-to-klib>` to pass libraries that contain tests.")
        }

        // Avoid showing warning twice in 2-phase compilation.
        if (konanConfig.produce != CompilerOutputKind.LIBRARY && konanConfig.target in softDeprecatedTargets) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                    "target ${konanConfig.target} is deprecated and will be removed soon. See: $DEPRECATION_LINK")
        }

        ensureModuleName(konanConfig)

        val sourcesFiles = environment.getSourceFiles()
        performanceManager?.apply {
            targetDescription = konanConfig.moduleId
            this.outputKind = konanConfig.produce.name
            addSourcesStats(sourcesFiles.size, environment.countLinesOfCode(sourcesFiles))
            // Finishing initialization phase before cache setup. Otherwise, cache building time will be counted as initialization phase.
            // Since cache builders use PerformanceManager to report precise phases, the only timing we lose is "calculating what to cache".
            notifyPhaseFinished(PhaseType.Initialization)
        }

        val cacheBuilder = CacheBuilder(konanConfig, compilationSpawner)
        if (cacheBuilder.needToBuild()) {
            cacheBuilder.build()
            konanConfig = KonanConfig(project, configuration) // TODO: Just set freshly built caches.
        }

        if (!konanConfig.produce.isHeaderCache) {
            konanConfig.cacheSupport.checkConsistency()
        }

        NativeCompilerDriver(performanceManager).run(konanConfig, environment)
    }

    private fun ensureModuleName(config: KonanConfig) {
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

    private fun splitOntoTwoStages() {
        // K2/Native backend cannot produce binary directly from FIR frontend output, since descriptors, deserialized from KLib, are needed
        // So, such compilation is split to two stages:
        // - source files are compiled to intermediate KLib by FIR frontend
        // - intermediate Klib is compiled to binary by K2/Native backend

        if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR) &&
                configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)
                        ?.getFeatureSupport(LanguageFeature.MultiPlatformProjects) == LanguageFeature.State.ENABLED)
            configuration.report(CompilerMessageSeverity.ERROR,
                    """
                            Producing a multiplatform library directly from sources is not allowed since language version 2.0.
                        
                            If you use the command-line compiler, then first compile the sources to a KLIB with
                            the `-p library` compiler flag. Then, use '-Xinclude=<klib>' to pass the KLIB to
                            the compiler to produce the required type of binary artifact.
                        """.trimIndent())

        // For the first stage, construct a temporary file name for an intermediate KLib.
        val intermediateKLib = File(System.getProperty("java.io.tmpdir"), "${UUID.randomUUID()}.klib").also {
            require(!it.exists) { "Collision writing intermediate KLib $it" }
            it.deleteOnExit()
        }

        // We will now spawn and wait for 2 separate compilers. Therefore, the initialization phase of this compiler is done.
        performanceManager?.notifyPhaseFinished(PhaseType.Initialization)

        compilationSpawner.spawn(emptyList()) {
            fun <T> copy(key: CompilerConfigurationKey<T>) = putIfNotNull(key, configuration.get(key))
            fun <T> copyNotNull(key: CompilerConfigurationKey<T>) = put(key, configuration.getNotNull(key))
            // For the first stage, use "-p library" produce mode.
            put(KonanConfigKeys.PRODUCE, CompilerOutputKind.LIBRARY)
            copy(KonanConfigKeys.TARGET)
            put(KonanConfigKeys.OUTPUT, intermediateKLib.absolutePath)
            copyNotNull(CLIConfigurationKeys.CONTENT_ROOTS)
            copyNotNull(KonanConfigKeys.LIBRARY_FILES)
            copy(KonanConfigKeys.FRIEND_MODULES)
            copy(KonanConfigKeys.REFINES_MODULES)
            copy(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE)
            copy(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX)
            copy(KonanConfigKeys.EXPORT_KDOC)
            copy(BinaryOptions.unitSuspendFunctionObjCExport)
            copy(BinaryOptions.objcExportDisableSwiftMemberNameMangling)
            copy(BinaryOptions.objcExportIgnoreInterfaceMethodCollisions)
            copy(KonanConfigKeys.OBJC_GENERICS)

            // KT-71976: Restore keys, which are reset within `compilationSpawner.spawn(emptyList())`,
            // during invocation of `prepareEnvironment()` with empty arguments.
            copyCommonKlibArgumentsFrom(configuration)
        }

        // For the second stage, remove already compiled source files from the configuration.
        configuration.put(CLIConfigurationKeys.CONTENT_ROOTS, listOf())
        // Frontend version must not be passed to 2nd stage (same as Gradle plugin does when calling CLI compiler), since there are no sources anymore
        configuration.put(CommonConfigurationKeys.USE_FIR, false)
        // For the second stage, provide just compiled intermediate KLib as "-Xinclude=" param.
        require(intermediateKLib.exists) { "Intermediate KLib $intermediateKLib must have been created by successful first compilation stage" }
        // We need to remove this flag, as it would otherwise override header written previously.
        // Unfortunately, there is no way to remove the flag, so empty string is put instead
        configuration.get(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE)?.let { configuration.put(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE, "") }
        configuration.put(KonanConfigKeys.INCLUDED_LIBRARIES,
                configuration.get(KonanConfigKeys.INCLUDED_LIBRARIES).orEmpty() + listOf(intermediateKLib.absolutePath))
        compilationSpawner.spawn(configuration) // Need to spawn a new compilation to create fresh environment (without sources).
    }
}
