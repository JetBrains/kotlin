/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.backend.common.serialization.IrKlibBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.deserializeFileEntryName
import org.jetbrains.kotlin.backend.common.serialization.fileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.backend.konan.driver.NativeCompilerDriver
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.config.compileFromBitcode
import org.jetbrains.kotlin.konan.config.exportedLibraries
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.config.generateTestRunner
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanLibraryToAddToCache
import org.jetbrains.kotlin.konan.config.listTargets
import org.jetbrains.kotlin.konan.config.makePerFileCache
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType

private val softDeprecatedTargets = setOf(
        KonanTarget.LINUX_ARM32_HFP,
        KonanTarget.MACOS_X64,
        KonanTarget.IOS_X64,
        KonanTarget.TVOS_X64,
        KonanTarget.WATCHOS_X64,
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
        val isCompilingFromBitcode = configuration.compileFromBitcode != null
        val hasSourceRoots = configuration.kotlinSourceRoots.isNotEmpty()

        if (isCompilingFromBitcode && hasSourceRoots) {
            configuration.report(
                    CompilerMessageSeverity.WARNING,
                    "Source files will be ignored by the compiler when compiling from bitcode"
            )
        }

        val fileNames = configuration.konanLibraryToAddToCache?.let { libPath ->
            val filesToCache = configuration.filesToCache
            when {
                !filesToCache.isEmpty() -> filesToCache
                configuration.makePerFileCache -> {
                    val result = KlibLoader {
                        libraryPaths(libPath)
                        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
                        configuration.zipFileSystemAccessor?.let(::zipFileSystemAccessor)
                    }.load()
                    result.reportLoadingProblemsIfAny(configuration, allAsErrors = true)

                    val lib = result.librariesStdlibFirst.singleOrNull() ?: return@let null

                    val ir = lib.irOrFail
                    (0 until ir.irFileCount).map { fileIndex ->
                        val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(ir, fileIndex))
                        val proto = IrFile.parseFrom(ir.irFile(fileIndex).codedInputStream, ExtensionRegistryLite.getEmptyRegistry())
                        val fileEntry = fileReader.fileEntry(proto)
                        fileReader.deserializeFileEntryName(fileEntry)
                    }
                }
                else -> null
            }
        }
        if (fileNames != null) {
            configuration.makePerFileCache = true
            configuration.filesToCache = fileNames
        }

        var config = NativeSecondStageCompilationConfig(project, configuration)

        if (configuration.listTargets) {
            config.targetManager.list()
        }

        val hasIncludedLibraries = configuration.konanIncludedLibraries.isNotEmpty()
        val isProducingExecutableFromLibraries = config.produce == CompilerOutputKind.PROGRAM
                && configuration.konanLibraries.isNotEmpty() && !hasIncludedLibraries
        val hasCompilerInput = configuration.kotlinSourceRoots.isNotEmpty()
                || hasIncludedLibraries
                || configuration.exportedLibraries.isNotEmpty()
                || config.libraryToCache != null
                || config.compileFromBitcode?.isNotEmpty() == true
                || isProducingExecutableFromLibraries

        if (!hasCompilerInput) return

        if (isProducingExecutableFromLibraries && configuration.generateTestRunner != TestRunnerKind.NONE) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                    "Use `-Xinclude=<path-to-klib>` to pass libraries that contain tests.")
        }

        // Avoid showing warning twice in 2-phase compilation.
        if (config.produce != CompilerOutputKind.LIBRARY && config.target in softDeprecatedTargets) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                    "target ${config.target} is deprecated and will be removed soon. See: $DEPRECATION_LINK")
        }

        ensureModuleName(config)

        val sourcesFiles = environment.getSourceFiles()
        performanceManager?.apply {
            targetDescription = config.moduleId
            this.outputKind = config.produce.name
            addSourcesStats(sourcesFiles.size, environment.countLinesOfCode(sourcesFiles))
            // Finishing initialization phase before cache setup. Otherwise, cache building time will be counted as initialization phase.
            // Since cache builders use PerformanceManager to report precise phases, the only timing we lose is "calculating what to cache".
            notifyPhaseFinished(PhaseType.Initialization)
        }

        val cacheBuilder = CacheBuilder(config, compilationSpawner)
        if (cacheBuilder.needToBuild()) {
            cacheBuilder.build()
            config = NativeSecondStageCompilationConfig(project, configuration) // TODO: Just set freshly built caches.
        }

        if (!config.produce.isHeaderCache) {
            config.cacheSupport.checkConsistency()
        }

        NativeCompilerDriver(performanceManager).run(config, environment)
    }

    private fun ensureModuleName(config: NativeSecondStageCompilationConfig) {
        if (environment.getSourceFiles().isEmpty()) {
            val libraries = config.resolvedLibraries.getFullList()
            val moduleName = config.moduleId
            if (libraries.any { it.uniqueName == moduleName }) {
                val kexeModuleName = "${moduleName}_kexe"
                config.configuration.moduleName = kexeModuleName
                assert(libraries.none { it.uniqueName == kexeModuleName })
            }
        }
    }

}
